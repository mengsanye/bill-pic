package com.next.billpic.core.data

import android.content.Context
import com.next.billpic.core.model.FeedbackEntry
import com.next.billpic.core.model.TelemetrySnapshot
import com.next.billpic.core.model.TrackedEvent
import com.next.billpic.core.model.TrialSession
import org.json.JSONArray
import org.json.JSONObject

/**
 * 走查数据的本地持久化。**只存在于 debug 源集**——上架包里没有这个类。
 *
 * 只存 JSON 到 SharedPreferences：数据量小、结构会随走查快速迭代，
 * 上 Room 反而拖慢速度。
 *
 * 边界：这里只存「会话 / 事件 / 反馈」。用户的转换记录与偏好走 [UserDataStore]，
 * 发票文件本身从不落库。
 */
class TelemetryStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): TelemetrySnapshot {
        val raw = prefs.getString(KEY_STATE, null) ?: return TelemetrySnapshot()
        return runCatching { decode(raw) }.getOrElse { TelemetrySnapshot() }
    }

    fun save(snapshot: TelemetrySnapshot) {
        runCatching { prefs.edit().putString(KEY_STATE, encode(snapshot)).apply() }
    }

    fun clear() {
        runCatching { prefs.edit().clear().apply() }
    }

    /* ------------------------------------------------------------------ */

    private fun encode(snapshot: TelemetrySnapshot): String {
        val root = JSONObject()
        root.put("currentSessionId", snapshot.currentSessionId ?: JSONObject.NULL)

        val sessions = JSONArray()
        snapshot.sessions.forEach { session ->
            val sessionObject = JSONObject()
            sessionObject.put("id", session.id)
            sessionObject.put("startedAt", session.startedAt)
            sessionObject.put("variant", session.variant)

            val events = JSONArray()
            session.events.forEach { event ->
                val eventObject = JSONObject()
                eventObject.put("t", event.timestamp)
                eventObject.put("name", event.name)
                eventObject.put("screen", event.screen)
                val props = JSONObject()
                event.props.forEach { (key, value) -> props.put(key, value) }
                eventObject.put("props", props)
                events.put(eventObject)
            }
            sessionObject.put("events", events)

            val feedback = JSONArray()
            session.feedback.forEach { entry ->
                val feedbackObject = JSONObject()
                feedbackObject.put("t", entry.timestamp)
                feedbackObject.put("rating", entry.rating)
                feedbackObject.put("text", entry.text)
                feedbackObject.put("intent", entry.intent)
                feedbackObject.put("converted", entry.converted)
                feedbackObject.put("formatId", entry.formatId)
                feedbackObject.put("pages", entry.pages)
                feedback.put(feedbackObject)
            }
            sessionObject.put("feedback", feedback)

            sessions.put(sessionObject)
        }
        root.put("sessions", sessions)
        return root.toString()
    }

    private fun decode(raw: String): TelemetrySnapshot {
        val root = JSONObject(raw)

        val sessions = mutableListOf<TrialSession>()
        val sessionArray = root.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sessionArray.length()) {
            val sessionObject = sessionArray.optJSONObject(i) ?: continue

            val events = mutableListOf<TrackedEvent>()
            val eventArray = sessionObject.optJSONArray("events") ?: JSONArray()
            for (j in 0 until eventArray.length()) {
                val eventObject = eventArray.optJSONObject(j) ?: continue
                val props = mutableMapOf<String, String>()
                eventObject.optJSONObject("props")?.let { propsObject ->
                    val keys = propsObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        props[key] = propsObject.optString(key)
                    }
                }
                events += TrackedEvent(
                    timestamp = eventObject.optLong("t"),
                    name = eventObject.optString("name"),
                    screen = eventObject.optString("screen"),
                    props = props,
                )
            }

            val feedback = mutableListOf<FeedbackEntry>()
            val feedbackArray = sessionObject.optJSONArray("feedback") ?: JSONArray()
            for (j in 0 until feedbackArray.length()) {
                val feedbackObject = feedbackArray.optJSONObject(j) ?: continue
                feedback += FeedbackEntry(
                    timestamp = feedbackObject.optLong("t"),
                    rating = feedbackObject.optInt("rating"),
                    text = feedbackObject.optString("text"),
                    intent = feedbackObject.optString("intent"),
                    converted = feedbackObject.optBoolean("converted"),
                    formatId = feedbackObject.optString("formatId"),
                    pages = feedbackObject.optInt("pages"),
                )
            }

            sessions += TrialSession(
                id = sessionObject.optString("id"),
                startedAt = sessionObject.optLong("startedAt"),
                variant = sessionObject.optString("variant"),
                events = events,
                feedback = feedback,
            )
        }

        val currentId =
            if (root.isNull("currentSessionId")) null else root.optString("currentSessionId")

        return TelemetrySnapshot(sessions = sessions, currentSessionId = currentId)
    }

    private companion object {
        const val PREFS_NAME = "billpic_telemetry"
        const val KEY_STATE = "state_v2"
    }
}
