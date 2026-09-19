package com.next.billpic.core.data

import android.content.Context
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.TelemetrySnapshot

/**
 * 上架包的采集实现注入点。
 *
 * 与 `src/debug` 同名，由构建变体决定谁参与编译。上架包拿到的是一个空实现：
 * 不记录、不落盘、不占存储，配合 R8 连事件名字符串都不会留在 dex 里。
 */
object TelemetryProvider {
    fun create(context: Context): Telemetry = NoOpTelemetry
}

/**
 * 空实现。**这是「上架包不含埋点」的落点。**
 *
 * 刻意保留同样的方法签名而不是删掉接口，目的是让 main 源集的调用代码
 * 在两个变体下完全一致——差异只体现在「有没有实现」，
 * 而不是「有没有一大堆 if (isDebug) 分支」。
 */
object NoOpTelemetry : Telemetry {

    /** 复用同一个空快照：上层会用引用比较判断「有没有新数据」，每次 new 会白白触发重组。 */
    private val empty = TelemetrySnapshot()

    override val enabled: Boolean = false

    override fun ensureSession(): String = AppConfig.RELEASE_CTA

    override fun startNewSession(): String = AppConfig.RELEASE_CTA

    override fun currentVariant(): String = AppConfig.RELEASE_CTA

    override fun track(name: String, screen: String, props: Map<String, String>) = Unit

    override fun recordFeedback(
        rating: Int,
        text: String,
        intent: String,
        converted: Boolean,
        formatId: String,
        pages: Int,
    ) = Unit

    override fun snapshot(): TelemetrySnapshot = empty

    override fun summaryText(): String = ""

    override fun exportJson(): String = ""

    override fun reset() = Unit
}
