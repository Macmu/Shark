package com.android.snake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

/** 接收通知栏远程输入的六位无线调试配对码，在后台线程完成配对与连接。 */
class AdbPairingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AdbAuth.ACTION_PAIR_CODE) return
        val bundle = RemoteInput.getResultsFromIntent(intent)
        val code = (bundle?.getCharSequence(AdbAuth.KEY_PAIR_CODE)?.toString()
            ?: intent.getStringExtra(AdbAuth.KEY_PAIR_CODE))
            ?.trim().orEmpty()

        val appContext = context.applicationContext
        // 一收到配对码就把输入通知收掉，不等配对结果。
        AdbAuth.dismissPairingNotification(appContext)

        if (code.length != 6 || !code.all { it.isDigit() }) {
            runCatching { AdbAuth.notifyResult(appContext, "配对码应为六位数字，请重新输入") }
            return
        }
        // 不用 goAsync()：配对包含 mDNS/TLS 等待，常超过前台广播 10 秒时限，
        // 持有 PendingResult 会触发 BroadcastTimeout ANR。广播立即结束，
        // 由独立（非 daemon）线程继续配对，避免线程随进程回收被中断。
        Thread {
            try {
                AdbAuth.pairAndConnect(appContext, code)
                    .onSuccess {
                        // 配对成功不再额外弹一条结果通知，通知栏保持干净；
                        // 收到码时通知已在上面清过一次，这里再兜一次。
                        AdbAuth.dismissPairingNotification(appContext)
                    }
                    .onFailure { error ->
                        runCatching {
                            AdbAuth.notifyResult(
                                appContext,
                                "内置授权失败：${error.message ?: error.javaClass.simpleName}"
                            )
                        }
                    }
            } catch (t: Throwable) {
                runCatching {
                    AdbAuth.notifyResult(
                        appContext,
                        "内置授权失败：${t.message ?: t.javaClass.simpleName}"
                    )
                }
            }
        }.apply { name = "snake-adb-pairing"; start() }
    }
}
