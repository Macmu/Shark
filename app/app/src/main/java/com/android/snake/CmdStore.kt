package com.android.snake

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 一条用户自定义的常用命令。
 *
 * id 只用于编辑/删除时定位，取保存时的时间戳，不做业务含义。
 */
data class SavedCmd(
    val id: Long,
    val nick: String,
    val cmd: String,
    val confirm: Boolean = false
)

/**
 * 常用命令的持久化。
 *
 * 单独放一个 SharedPreferences（snake_cmds），不和越狱偏好（pyb110）混在一起——
 * 那里面存的是开关和授权状态，清授权或重置时不应该把用户攒的命令一起带走。
 */
object CmdStore {

    private const val PREFS = "snake_cmds"
    private const val KEY = "items"
    private const val MAX = 80

    fun load(ctx: Context): List<SavedCmd> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val cmd = o.optString("cmd")
                if (cmd.isBlank()) return@mapNotNull null
                SavedCmd(
                    id = o.optLong("id"),
                    nick = o.optString("nick").ifBlank { defaultNick(cmd) },
                    cmd = cmd,
                    confirm = o.optBoolean("confirm")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(ctx: Context, items: List<SavedCmd>) {
        val arr = JSONArray()
        items.take(MAX).forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("nick", c.nick)
                    put("cmd", c.cmd)
                    put("confirm", c.confirm)
                }
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    /**
     * 昵称留空时的兜底。
     * 取命令的前两个词（`pm clear`、`uname -r`）比截断整行好认，
     * 单词命令（`getenforce`）就整体保留。
     */
    fun defaultNick(cmd: String): String {
        val one = cmd.trim().replace(Regex("\\s+"), " ")
        val tokens = one.split(' ').filter { it.isNotEmpty() }
        val guess = if (tokens.size >= 2) tokens.take(2).joinToString(" ") else one
        return if (guess.length <= 14) guess else guess.take(13) + "…"
    }

    /** 这类命令误触代价大，保存时默认勾上「执行前确认」。 */
    fun looksRisky(cmd: String): Boolean {
        val l = cmd.lowercase()
        return RISKY.any { l.contains(it) }
    }

    private val RISKY = listOf(
        "rm -", "rm ", "pm clear", "reboot", "killall", "mkfs", "dd if=", "dd of=",
        "chmod -r", "chown -r", "setenforce", "stop ", "format", "wipe"
    )
}
