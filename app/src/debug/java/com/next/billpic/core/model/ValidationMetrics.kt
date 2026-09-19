package com.next.billpic.core.model

/** 转化漏斗的一步：多少个会话走到了这一步 */
data class FunnelStep(val label: String, val eventName: String, val sessionCount: Int)

/** 单个 A/B 变体的表现 */
data class VariantStat(val variant: String, val sessions: Int, val completed: Int) {
    val completionRate: Double get() = if (sessions == 0) 0.0 else completed.toDouble() / sessions
}

/** 验证看板需要的全部指标 */
data class ValidationMetrics(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val feedbackCount: Int = 0,
    val averageRating: Double = 0.0,
    val wouldUseCount: Int = 0,
    val errorCount: Int = 0,
    val averageDurationMs: Long = 0L,
    val funnel: List<FunnelStep> = emptyList(),
    val variants: List<VariantStat> = emptyList(),
) {
    val completionRate: Double
        get() = if (totalSessions == 0) 0.0 else completedSessions.toDouble() / totalSessions

    /** 及格判定：样本够 + 完成率达标。评分与意愿只做展示，不参与硬判定。 */
    val passed: Boolean
        get() = totalSessions >= AppConfig.SAMPLE_TARGET &&
            completionRate >= AppConfig.COMPLETE_RATE_TARGET

    val conclusion: String
        get() = if (passed) "达到及格线，可进入下一轮" else "样本或完成率不足，先补数据、别急着加功能"
}

/**
 * 指标计算逻辑。纯函数，不依赖 Android，方便直接写单测。
 * 口径与原型完全一致：所有比例都以「会话」为单位，而不是以事件次数为单位，
 * 否则同一个人多点几次就会把完成率抬高。
 */
object ValidationCalculator {

    private val FUNNEL_STEPS = listOf(
        "打开 App" to "session_start",
        "选中发票文件" to "file_select",
        "发起转换" to "conversion_start",
        "成功拿到图片" to "flow_complete",
        "提交反馈" to "feedback_submit",
    )

    private val ERROR_EVENTS = setOf("conversion_error", "file_reject", "file_parse_error")

    fun compute(snapshot: TelemetrySnapshot): ValidationMetrics {
        val sessions = snapshot.sessions
        val total = sessions.size

        fun sessionsWith(eventName: String): Int =
            sessions.count { session -> session.events.any { it.name == eventName } }

        val completed = sessionsWith("flow_complete")
        val allFeedback = sessions.flatMap { it.feedback }

        val averageRating = if (allFeedback.isEmpty()) {
            0.0
        } else {
            allFeedback.map { it.rating }.average()
        }

        val conversions = sessions
            .flatMap { it.events }
            .filter { it.name == "conversion_done" }
            .mapNotNull { it.props["ms"]?.toLongOrNull() }

        val variants = AppConfig.AB_VARIANTS.map { variant ->
            val group = sessions.filter { it.variant == variant }
            VariantStat(
                variant = variant,
                sessions = group.size,
                completed = group.count { session ->
                    session.events.any { it.name == "flow_complete" }
                },
            )
        }

        return ValidationMetrics(
            totalSessions = total,
            completedSessions = completed,
            feedbackCount = allFeedback.size,
            averageRating = averageRating,
            wouldUseCount = allFeedback.count { it.intent == AppConfig.FEEDBACK_INTENTS.first() },
            errorCount = sessions.flatMap { it.events }.count { it.name in ERROR_EVENTS },
            averageDurationMs = if (conversions.isEmpty()) 0L else conversions.average().toLong(),
            funnel = FUNNEL_STEPS.map { (label, eventName) ->
                FunnelStep(label = label, eventName = eventName, sessionCount = sessionsWith(eventName))
            },
            variants = variants,
        )
    }
}
