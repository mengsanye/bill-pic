package com.next.billpic.core.data

import com.next.billpic.core.model.TelemetrySnapshot

/**
 * 走查数据采集接口。
 *
 * **这是「上架包不含埋点」的技术支点。**
 *
 * 接口定义在 main 源集，实现按构建变体注入：
 * - `src/debug`  → DebugTelemetry：真采集、真落盘，供内部走查
 * - `src/release` → NoOpTelemetry：空实现，什么都不记、什么都不写
 *
 * 于是上架包里 `TelemetryStore`、`ValidationCalculator`、验证看板这些类
 * **根本不参与编译**——「不采集」是构建产物上的结构性事实，
 * 而不是一个改错了就静默失效的布尔开关。
 *
 * 注意与 [UserDataStore] 的分工：用户自己的转换记录与偏好走 UserDataStore，
 * 上架包同样需要；这里只管走查用的会话、事件与反馈。
 */
interface Telemetry {

    /** 走查包为 true，上架包为 false。 */
    val enabled: Boolean

    /**
     * 确保存在一个试用会话：已有则复用，没有才新建。
     *
     * 刻意**不是**每次启动都新建——漏斗与完成率都以「会话」为单位统计，
     * 每次冷启动都开新会话会把样本数抬虚。
     *
     * @return 当前会话被分到的 A/B 文案；上架包返回固定文案。
     */
    fun ensureSession(): String

    /** 强制开启新一轮会话（走查专员换下一位同事时使用）。 */
    fun startNewSession(): String

    /** 当前会话已分配的 A/B 文案。 */
    fun currentVariant(): String

    /** 记录事件。上架包为空实现。 */
    fun track(name: String, screen: String, props: Map<String, String> = emptyMap())

    /** 记录一条走查反馈。上架包为空实现。 */
    fun recordFeedback(
        rating: Int,
        text: String,
        intent: String,
        converted: Boolean,
        formatId: String,
        pages: Int,
    )

    /** 当前数据快照，供验证看板展示。上架包返回空快照。 */
    fun snapshot(): TelemetrySnapshot

    /** 走查摘要文本（复制到剪贴板用）。上架包返回空串。 */
    fun summaryText(): String

    /** 导出用 JSON。上架包返回空串。 */
    fun exportJson(): String

    /** 清空全部走查数据。上架包为空实现。 */
    fun reset()
}
