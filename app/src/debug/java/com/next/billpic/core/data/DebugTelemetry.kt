package com.next.billpic.core.data

import android.content.Context
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.FeedbackEntry
import com.next.billpic.core.model.TelemetrySnapshot
import com.next.billpic.core.model.TrackedEvent
import com.next.billpic.core.model.TrialSession
import com.next.billpic.core.model.ValidationCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * 走查包的采集实现。**只存在于 debug 源集。**
 *
 * 摘要与导出的计算放在这里而不是 ViewModel 里，是为了让 ViewModel 不依赖
 * [ValidationCalculator]——否则 release 编译时会因为找不到这个类而失败，
 * 结构上就把两个变体绑死了。
 */
class DebugTelemetry(context: Context) : Telemetry {

    private val store = TelemetryStore(context.applicationContext)
    private var snapshot: TelemetrySnapshot = store.load()

    override val enabled: Boolean = true

    override fun ensureSession(): String {
        snapshot.currentSession?.let { return it.variant }
        return startNewSession()
    }

    override fun startNewSession(): String {
        val id = "s" + System.currentTimeMillis().toString(36) + (100..999).random()
        val session = TrialSession(
            id = id,
            startedAt = System.currentTimeMillis(),
            variant = AppConfig.assignVariant(id),
        )
        snapshot = snapshot.copy(
            sessions = snapshot.sessions + session,
            currentSessionId = id,
        )
        store.save(snapshot)
        return session.variant
    }

    override fun currentVariant(): String =
        snapshot.currentSession?.variant ?: AppConfig.AB_VARIANTS.first()

    override fun track(name: String, screen: String, props: Map<String, String>) {
        val sessionId = snapshot.currentSessionId ?: return
        val event = TrackedEvent(
            timestamp = System.currentTimeMillis(),
            name = name,
            screen = screen,
            props = props,
        )
        snapshot = snapshot.copy(
            sessions = snapshot.sessions.map { session ->
                if (session.id == sessionId) session.copy(events = session.events + event) else session
            },
        )
        store.save(snapshot)
    }

    override fun recordFeedback(
        rating: Int,
        text: String,
        intent: String,
        converted: Boolean,
        formatId: String,
        pages: Int,
    ) {
        val sessionId = snapshot.currentSessionId ?: return
        val entry = FeedbackEntry(
            timestamp = System.currentTimeMillis(),
            rating = rating,
            text = text.trim(),
            intent = intent,
            converted = converted,
            formatId = formatId,
            pages = pages,
        )
        snapshot = snapshot.copy(
            sessions = snapshot.sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(feedback = session.feedback + entry)
                } else {
                    session
                }
            },
        )
        store.save(snapshot)
    }

    override fun snapshot(): TelemetrySnapshot = snapshot

    override fun summaryText(): String {
        val metrics = ValidationCalculator.compute(snapshot)
        val feedback = snapshot.sessions.flatMap { it.feedback }.sortedBy { it.timestamp }
        val lines = mutableListOf(
            "【BillPic Android】原型走查摘要",
            "试用会话：${metrics.totalSessions}",
            "成功拿到图片：${metrics.completedSessions}（完成率 " +
                "${(metrics.completionRate * 100).toInt()}%）",
            "平均转换耗时：" + if (metrics.averageDurationMs > 0) {
                String.format(Locale.CHINA, "%.2f 秒", metrics.averageDurationMs / 1000.0)
            } else {
                "—"
            },
            "反馈数：${metrics.feedbackCount}，平均评分：" +
                if (metrics.feedbackCount > 0) {
                    String.format(Locale.CHINA, "%.1f", metrics.averageRating)
                } else {
                    "—"
                } + " / 5",
            "一定会用：${metrics.wouldUseCount} 人",
            "转换失败次数：${metrics.errorCount}",
            "",
            "主要反馈：",
        )
        if (feedback.isEmpty()) {
            lines += "· （暂无）"
        } else {
            feedback.takeLast(6).forEach { entry ->
                lines += "· [${entry.rating}★] " +
                    entry.text.ifBlank { "（无文字）" } +
                    (if (entry.intent.isNotBlank()) " / ${entry.intent}" else "")
            }
        }
        return lines.joinToString("\n")
    }

    override fun exportJson(): String {
        val metrics = ValidationCalculator.compute(snapshot)
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("product", AppConfig.APP_NAME)
        root.put("platform", "android")

        val config = JSONObject()
        config.put("hypothesis", AppConfig.HYPOTHESIS)
        config.put("successMetric", AppConfig.SUCCESS_METRIC)
        config.put("completeRateTarget", AppConfig.COMPLETE_RATE_TARGET)
        config.put("ratingTarget", AppConfig.RATING_TARGET)
        config.put("sampleTarget", AppConfig.SAMPLE_TARGET)
        config.put("abTest", AppConfig.AB_TEST_NAME)
        root.put("config", config)

        val summary = JSONObject()
        summary.put("sessions", metrics.totalSessions)
        summary.put("completed", metrics.completedSessions)
        summary.put("completionRate", metrics.completionRate)
        summary.put("feedbackCount", metrics.feedbackCount)
        summary.put("avgRating", metrics.averageRating)
        summary.put("wouldUse", metrics.wouldUseCount)
        summary.put("errorEvents", metrics.errorCount)
        root.put("summary", summary)

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
                eventObject.put("event", event.name)
                eventObject.put("screen", event.screen)
                eventObject.put("props", JSONObject(event.props))
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

        return root.toString(2)
    }

    override fun reset() {
        store.clear()
        snapshot = TelemetrySnapshot()
    }
}
