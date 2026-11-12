package com.shark.kty

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

        if (code.length != 6 || !code.all { it.isDigit() }) {
            AdbAuth.notifyResult(context, "配对码应为六位数字，请重新输入")
            return
        }

        val appContext = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                val result = AdbAuth.pairAndConnect(appContext, code)
                runCatching {
                    AdbAuth.notifyResult(
                        appContext,
                        result.getOrElse { "内置授权失败：${it.message ?: it.javaClass.simpleName}" }
                    )
                }
            } finally {
                pending.finish()
            }
        }.apply { isDaemon = true; start() }
    }
}
