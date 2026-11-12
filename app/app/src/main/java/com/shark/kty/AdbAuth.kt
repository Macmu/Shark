package com.shark.kty

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.android.AdbMdns
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shark 内置授权后端。
 *
 * 通过无线调试（ADB over TLS）在本应用进程内直接与本机 adbd 建立会话，
 * 不再要求额外安装并启动 Shizuku / Stellar。ADB 协议与配对实现来自
 * MuntashirAkon/libadb-android（GPL-3.0-or-later OR Apache-2.0），
 * 身份材料为本应用自签的 RSA/X.509 密钥对。
 */
object AdbAuth {

    const val ACTION_PAIR_CODE = "com.shark.kty.ADB_PAIR_CODE"
    const val KEY_PAIR_CODE = "pair_code"

    private const val CHANNEL_ID = "shark_adb_auth"
    private const val NOTIFICATION_ID = 7101
    private const val KEY_FILE = "adb.key"
    private const val CERT_FILE = "adb.crt"

    enum class StartState { CONNECTED, PAIRING, NEED_PERMISSIONS }

    @Volatile
    private var manager: SharkAdbManager? = null

    @Volatile
    private var connected = false

    fun isReady(): Boolean = connected && (manager?.isConnected == true)

    fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.POST_NOTIFICATIONS)
        add(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.ACCESS_NETWORK_STATE)
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
        // Android 17 (API 37) 起本地网络访问收口到该运行时权限，
        // 未授予时 NsdManager 不会回调发现结果。用字符串避免旧 SDK 常量缺失。
        if (Build.VERSION.SDK_INT >= 37) add("android.permission.ACCESS_LOCAL_NETWORK")
    }.toTypedArray()

    private fun hasPermissions(context: Context): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** 主按钮在「未连接」状态下的入口：先尝试复用已配对的连接，否则引导配对。 */
    fun startPairingFlow(context: Context): StartState {
        if (!hasPermissions(context)) return StartState.NEED_PERMISSIONS
        if (connect(context)) {
            notifyResult(context, "内置授权已连接（无线调试）")
            return StartState.CONNECTED
        }
        showPairingNotification(context)
        openDeveloperSettings(context)
        return StartState.PAIRING
    }

    /** 配对码由通知栏远程输入提交后调用。 */
    fun pairAndConnect(context: Context, code: String): Result<String> = runCatching {
        val port = discoverPairingPort(context)
            ?: error("未发现无线调试配对服务，请确认「使用配对码配对设备」弹窗仍打开")
        val m = ensureManager(context)
        m.setHostAddress("127.0.0.1")
        var paired = false
        var lastError: Throwable? = null
        // 不同 ROM 的配对服务可能只监听回环或某个局域网地址，逐个候选尝试。
        for (host in candidateHosts()) {
            val ok = runCatching { m.pair(host, port, code) }
                .onFailure { lastError = it }
                .getOrDefault(false)
            if (ok) {
                paired = true
                break
            }
        }
        if (!paired) error(
            "配对失败：${lastError?.message ?: "配对码错误或已超时，关闭弹窗后重新打开再试"}"
        )
        val ok = m.connectTls(context.applicationContext, 15_000L)
        connected = ok && m.isConnected
        if (!isReady()) error("配对成功但连接失败，请确认「无线调试」开关已打开")
        "内置授权已连接（无线调试）"
    }

    @Synchronized
    fun connect(context: Context): Boolean {
        if (!hasPermissions(context)) return false
        val m = ensureManager(context)
        if (m.isConnected) {
            connected = true
            return true
        }
        val ok = runCatching { m.connectTls(context.applicationContext, 12_000L) }.getOrDefault(false)
        connected = ok && m.isConnected
        return connected
    }

    @Synchronized
    fun disconnect() {
        runCatching { manager?.disconnect() }
        manager = null
        connected = false
    }

    /**
     * 把 ADB shell 会话包装成 Shark 既有的 PrivProcess 契约：
     * 独立 stdout、空 stderr（stderr 已在 shell 内合并）、可等待的退出码。
     */
    fun newProcess(cmd: Array<String>, env: Array<String>?, dir: String?): PrivProcess {
        val m = manager ?: error("内置授权未连接")
        val stream = m.openStream("shell:")
        val output = stream.openOutputStream()
        val input = stream.openInputStream()
        val token = UUID.randomUUID().toString().replace("-", "")
        val marker = "__SHARK_RC_${token}__"
        val shellLine = buildShellLine(cmd, env, dir, marker)

        // 关闭 PTY 回显，避免命令本身被当成日志输出。
        output.write("stty -echo 2>/dev/null; export PS1=''; export PROMPT_COMMAND=''\n".toByteArray())
        output.flush()
        output.write((shellLine + "\n").toByteArray())
        output.flush()

        val pipe = PipeStream()
        val done = CountDownLatch(1)
        var exitCode = -1
        val rcPattern = Regex("${marker}(\\d+)")

        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.contains("stty -echo") || line.contains("PROMPT_COMMAND") || line.contains("export PS1")) continue
                    if (line.contains(marker)) {
                        val match = rcPattern.find(line)
                        if (match != null) {
                            exitCode = match.groupValues[1].toIntOrNull() ?: -1
                            break
                        }
                        // 命令回显也含 marker 但没有退出码，不作为日志输出。
                        continue
                    }
                    pipe.append((line + "\n").toByteArray(Charsets.UTF_8))
                }
            } catch (_: Throwable) {
                // 连接断开 / 流关闭：以 -1 结束，交由上层判失败。
            } finally {
                pipe.finish()
                runCatching { stream.close() }
                done.countDown()
            }
        }.apply { isDaemon = true; start() }

        return PrivProcess(
            pipe,
            NullOutputStream,
            ByteArrayInputStream(ByteArray(0))
        ) {
            if (!done.await(180, TimeUnit.SECONDS)) {
                runCatching { stream.close() }
                -1
            } else {
                exitCode
            }
        }
    }

    private fun buildShellLine(cmd: Array<String>, env: Array<String>?, dir: String?, marker: String): String {
        val sb = StringBuilder()
        if (!dir.isNullOrEmpty()) sb.append("cd ").append(shQuote(dir)).append(" 2>/dev/null; ")
        env?.forEach { entry ->
            val i = entry.indexOf('=')
            if (i > 0) {
                sb.append("export ").append(shQuote(entry.substring(0, i)))
                    .append('=').append(shQuote(entry.substring(i + 1))).append("; ")
            }
        }
        sb.append(cmd.joinToString(" ") { shQuote(it) })
        sb.append(" 2>&1; printf '\\n").append(marker).append("%s\\n' \"$?\"")
        return sb.toString()
    }

    private fun shQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun candidateHosts(): List<String> {
        val hosts = mutableListOf("127.0.0.1")
        runCatching {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val addresses = interfaces.nextElement().inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        addr.hostAddress?.let { hosts.add(it) }
                    }
                }
            }
        }
        return hosts.distinct()
    }

    private fun discoverPairingPort(context: Context): Int? {
        val found = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val listener = AdbMdns.OnAdbDaemonDiscoveredListener { _, port ->
            if (port > 0) {
                found.set(port)
                latch.countDown()
            }
        }
        val mdns = AdbMdns(
            context.applicationContext,
            AdbMdns.SERVICE_TYPE_TLS_PAIRING,
            listener
        )
        return try {
            mdns.start()
            latch.await(12, TimeUnit.SECONDS)
            found.get().takeIf { it > 0 }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { mdns.stop() }
        }
    }

    @Synchronized
    private fun ensureManager(context: Context): SharkAdbManager {
        manager?.let { return it }
        val (key, cert) = loadOrCreateIdentity(context)
        return SharkAdbManager(key, cert).also { manager = it }
    }

    private fun loadOrCreateIdentity(context: Context): Pair<PrivateKey, Certificate> {
        val dir = File(context.filesDir, "adblink").apply { mkdirs() }
        val keyFile = File(dir, KEY_FILE)
        val certFile = File(dir, CERT_FILE)
        if (keyFile.exists() && certFile.exists()) {
            return runCatching {
                val key = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
                val cert = CertificateFactory.getInstance("X.509")
                    .generateCertificate(certFile.inputStream())
                key to cert
            }.getOrNull() ?: run {
                keyFile.delete()
                certFile.delete()
                return createIdentity(dir, keyFile, certFile)
            }
        }
        return createIdentity(dir, keyFile, certFile)
    }

    private fun createIdentity(dir: File, keyFile: File, certFile: File): Pair<PrivateKey, Certificate> {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val pair = generator.generateKeyPair()
        val subject = X500Name("CN=Shark ADB Client")
        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            Date(now - 86_400_000L),
            Date(now + 10L * 365L * 86_400_000L),
            subject,
            pair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        keyFile.writeBytes(pair.private.encoded)
        certFile.writeBytes(cert.encoded)
        return pair.private to cert
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Shark 内置授权", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "无线调试配对码输入与连接结果"
                }
            )
        }
    }

    private fun showPairingNotification(context: Context) {
        ensureChannel(context)
        val intent = Intent(context, AdbPairingReceiver::class.java).setAction(ACTION_PAIR_CODE)
        val pending = PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val remoteInput = RemoteInput.Builder(KEY_PAIR_CODE).setLabel("六位配对码").build()
        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_dialog_info, "输入配对码", pending
        ).addRemoteInput(remoteInput).build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Shark 内置授权")
            .setContentText("打开「无线调试 → 使用配对码配对设备」，在此输入六位配对码")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "打开开发者选项中的「无线调试 → 使用配对码配对设备」，保持弹窗开启，然后点下方按钮输入六位数字。"
            ))
            .addAction(action)
            .setAutoCancel(false)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    fun notifyResult(context: Context, message: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Shark 内置授权")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun openDeveloperSettings(context: Context) {
        val candidates = listOf(
            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            Settings.ACTION_DEVICE_INFO_SETTINGS
        )
        for (action in candidates) {
            val ok = runCatching {
                context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
            if (ok) return
        }
    }

    private class SharkAdbManager(
        private val key: PrivateKey,
        private val cert: Certificate
    ) : AbsAdbConnectionManager() {
        init {
            setApi(Build.VERSION.SDK_INT)
            setTimeout(25, TimeUnit.SECONDS)
            setHostAddress("127.0.0.1")
            setThrowOnUnauthorised(false)
        }

        override fun getPrivateKey(): PrivateKey = key
        override fun getCertificate(): Certificate = cert
        override fun getDeviceName(): String = "Shark"
    }

    private object NullOutputStream : OutputStream() {
        override fun write(b: Int) = Unit
        override fun write(b: ByteArray, off: Int, len: Int) = Unit
    }

    /** 无界、非阻塞写入的管道流：即使调用方只 waitFor() 不读取也不会死锁。 */
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private class PipeStream : InputStream() {
        private val lock = java.lang.Object()
        private val buffer = ArrayDeque<Byte>()
        private var finished = false

        fun append(data: ByteArray) {
            synchronized(lock) {
                for (b in data) buffer.addLast(b)
                lock.notifyAll()
            }
        }

        fun finish() {
            synchronized(lock) {
                finished = true
                lock.notifyAll()
            }
        }

        override fun read(): Int = synchronized(lock) {
            while (buffer.isEmpty() && !finished) lock.wait()
            if (buffer.isEmpty()) -1 else (buffer.removeFirst().toInt() and 0xff)
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int = synchronized(lock) {
            while (buffer.isEmpty() && !finished) lock.wait()
            if (buffer.isEmpty()) {
                -1
            } else {
                val count = minOf(len, buffer.size)
                for (i in 0 until count) b[off + i] = buffer.removeFirst()
                count
            }
        }

        override fun available(): Int = synchronized(lock) { buffer.size }
        override fun close() = finish()
    }
}
