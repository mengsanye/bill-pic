package com.next.billpic.core.data

import android.content.Context
import com.next.billpic.core.model.ConversionRecord
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.model.OutputScale
import com.next.billpic.core.model.UserSnapshot
import org.json.JSONArray
import org.json.JSONObject

/**
 * 用户数据的本地存储：转换记录与输出偏好。
 *
 * 只存 JSON 到 SharedPreferences —— 数据量小（最多 50 条元信息），
 * 为这点数据上 Room 会平白增加启动开销。
 *
 * 隐私注意：history 含**发票文件名**，属个人信息。因此「我的 → 隐私与数据」必须提供
 * 清除入口，隐私说明也必须如实披露留存内容与条数。
 */
class UserDataStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): UserSnapshot {
        val raw = prefs.getString(KEY_STATE, null) ?: return UserSnapshot()
        return runCatching { decode(raw) }.getOrElse { UserSnapshot() }
    }

    fun save(snapshot: UserSnapshot) {
        runCatching { prefs.edit().putString(KEY_STATE, encode(snapshot)).apply() }
    }

    /** 追加一条转换记录并裁剪到上限，返回更新后的快照。 */
    fun appendRecord(record: ConversionRecord): UserSnapshot {
        val current = load()
        val updated = current.copy(
            history = (listOf(record) + current.history).take(UserSnapshot.MAX_HISTORY),
        )
        save(updated)
        return updated
    }

    /** 删除指定下标的一条记录（用户长按删除）。 */
    fun removeRecordAt(index: Int): UserSnapshot {
        val current = load()
        if (index !in current.history.indices) return current
        val updated = current.copy(history = current.history.filterIndexed { i, _ -> i != index })
        save(updated)
        return updated
    }

    /** 只清转换记录，保留输出偏好。 */
    fun clearHistory(): UserSnapshot {
        val updated = load().copy(history = emptyList())
        save(updated)
        return updated
    }

    fun clear() {
        runCatching { prefs.edit().clear().apply() }
    }

    /* ------------------------------------------------------------------ */

    private fun encode(snapshot: UserSnapshot): String {
        val root = JSONObject()
        root.put("preferredFormatId", snapshot.preferredFormatId)
        root.put("preferredScaleId", snapshot.preferredScaleId)

        val history = JSONArray()
        snapshot.history.forEach { record ->
            val item = JSONObject()
            item.put("t", record.timestamp)
            item.put("fileName", record.fileName)
            item.put("pages", record.pages)
            item.put("formatId", record.formatId)
            item.put("scaleValue", record.scaleValue.toDouble())
            item.put("totalBytes", record.totalBytes)
            item.put("durationMs", record.durationMs)
            history.put(item)
        }
        root.put("history", history)
        return root.toString()
    }

    private fun decode(raw: String): UserSnapshot {
        val root = JSONObject(raw)

        val history = mutableListOf<ConversionRecord>()
        val array = root.optJSONArray("history") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            history += ConversionRecord(
                timestamp = item.optLong("t"),
                fileName = item.optString("fileName"),
                pages = item.optInt("pages"),
                formatId = item.optString("formatId"),
                scaleValue = item.optDouble("scaleValue", 2.0).toFloat(),
                totalBytes = item.optLong("totalBytes"),
                durationMs = item.optLong("durationMs"),
            )
        }

        return UserSnapshot(
            history = history,
            preferredFormatId = OutputFormat.fromId(root.optString("preferredFormatId")).id,
            preferredScaleId = OutputScale.fromId(root.optString("preferredScaleId")).id,
        )
    }

    private companion object {
        const val PREFS_NAME = "billpic_user_data"
        const val KEY_STATE = "user_v1"
    }
}
