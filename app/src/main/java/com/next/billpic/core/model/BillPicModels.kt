package com.next.billpic.core.model

import android.net.Uri

/** 输出格式。原型只支持 JPG / PNG 两种，这里保持一致，不做「图片互转」。 */
enum class OutputFormat(val id: String, val ext: String, val mime: String, val label: String) {
    JPG("jpg", "jpg", "image/jpeg", "JPG"),
    PNG("png", "png", "image/png", "PNG"),
    ;

    companion object {
        fun fromId(id: String?): OutputFormat = entries.firstOrNull { it.id == id } ?: JPG
    }
}

/**
 * 输出清晰度。倍数直接作用在 PDF 页面的点尺寸上：
 * 一页 595×842pt 的 A4 发票，高清档输出即 1190×1684 px，与原型一致。
 */
enum class OutputScale(val id: String, val value: Float, val label: String, val hint: String) {
    STANDARD("1.5", 1.5f, "标准", "标准：体积小，适合微信直接发"),
    HIGH("2", 2f, "高清", "高清：发票文字清晰，体积适中"),
    ULTRA("3", 3f, "超清", "超清：适合打印或放大核对"),
    ;

    companion object {
        fun fromId(id: String?): OutputScale = entries.firstOrNull { it.id == id } ?: HIGH
    }
}

/** 用户选中的 PDF 文件（已复制到应用缓存，保证可随机读取）。 */
data class PdfSource(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val pageCount: Int,
)

/** 单页转换结果。bytes 已按目标格式编码，可直接预览、保存、分享。 */
class ConvertedPage(
    val page: Int,
    val fileName: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

/** 一条转换记录（仅存元信息，图片本身交给系统相册） */
data class ConversionRecord(
    val timestamp: Long,
    val fileName: String,
    val pages: Int,
    val formatId: String,
    val scaleValue: Float,
    val totalBytes: Long,
    val durationMs: Long,
)

/** 用户反馈。converted 标记该用户是否真的拿到了图片——用于区分「没用懂」和「不好用」。 */
data class FeedbackEntry(
    val timestamp: Long,
    val rating: Int,
    val text: String,
    val intent: String,
    val converted: Boolean,
    val formatId: String,
    val pages: Int,
)

/** 埋点事件 */
data class TrackedEvent(
    val timestamp: Long,
    val name: String,
    val screen: String,
    val props: Map<String, String>,
) {
    /** 事件流展示用：把 props 拼成 key=value, key=value */
    val extra: String get() = props.entries.joinToString(", ") { entry -> entry.key + "=" + entry.value }
}

/** 一次试用会话。A/B 变体按会话稳定分流，便于 5 位同事各走一遍互不干扰。 */
data class TrialSession(
    val id: String,
    val startedAt: Long,
    val variant: String,
    val events: List<TrackedEvent> = emptyList(),
    val feedback: List<FeedbackEntry> = emptyList(),
)

/** 本地持久化的全部验证数据 */
data class TelemetrySnapshot(
    val sessions: List<TrialSession> = emptyList(),
    val currentSessionId: String? = null,
    val history: List<ConversionRecord> = emptyList(),
    val preferredFormatId: String = OutputFormat.JPG.id,
    val preferredScaleId: String = OutputScale.HIGH.id,
) {
    val currentSession: TrialSession?
        get() = sessions.firstOrNull { it.id == currentSessionId }
}
