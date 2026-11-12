package com.android.snake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

/** 接收通知栏远程输入的六位无线调试配对码，在后台线程完成配对与连接。 */
class AdbPairingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        // 用户点通知上的「关闭」：只收起通知，不动授权状态。
        if (intent.action == AdbAuth.ACTION_DISMISS) {
            AdbAuth.dismissPairingNotification(appContext)
            return
        }
        if (intent.action != AdbAuth.ACTION_PAIR_CODE) return
        val bundle = RemoteInput.getResultsFromIntent(intent)
        val code = (bundle?.getCharSequence(AdbAuth.KEY_PAIR_CODE)?.toString()
            ?: intent.getStringExtra(AdbAuth.KEY_PAIR_CODE))
            ?.trim().orEmpty()

        // 用户提交配对码后立刻收起输入通知；结果通知短暂停留后自动消失。
        AdbAuth.dismissPairingNotification(appContext)

        if (code.length != 6 || !code.all { it.isDigit() }) {
            runCatching { AdbAuth.notifyResult(appContext, "配对码应为六位数字，请重新输入") }
            return
        }
        // 不用 goAsync()：配对包含 mDNS/TLS 等待，常超过前台广播 10 秒时限，
        // 持有 PendingResult 会触发 BroadcastTimeout ANR。广播立即结束，
        // 由独立（非 daemon）线程继续配对，避免线程随进程回收被中断。
        Thread {
            val message = try {
                AdbAuth.pairAndConnect(appContext, code)
                    .getOrElse { "内置授权失败：${it.message ?: it.javaClass.simpleName}" }
            } catch (t: Throwable) {
                "内置授权失败：${t.message ?: t.javaClass.simpleName}"
            }
            runCatching { AdbAuth.notifyResult(appContext, message) }
        }.apply { name = "shark-adb-pairing"; start() }
    }
}
