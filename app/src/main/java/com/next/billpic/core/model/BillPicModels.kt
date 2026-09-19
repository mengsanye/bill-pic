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
 * 输出档位。
 *
 * 命名刻意从「清晰度」改成「体积取向」——报销场景真正的拦路虎不是不够清晰，
 * 而是**平台有上传大小限制，转出来传不上去**。所以第一档直接叫「省空间」，
 * 并且档位同时控制渲染倍数与 JPEG 质量，两个杠杆一起用。
 */
enum class OutputScale(
    val id: String,
    val value: Float,
    val jpegQuality: Int,
    val label: String,
    val hint: String,
) {
    COMPACT("compact", 1.2f, 80, "省空间", "文件最小，适合有上传大小限制的报销平台"),
    STANDARD("standard", 2f, 90, "标准", "A4 发票约 1190×1684，日常够用"),
    HIGH("high", 3f, 92, "高清", "发票文字最锐利，文件也最大"),
    ;

    companion object {
        /** 兼容早期版本用倍数当 id 的记录（"1.5" / "2" / "3"）。 */
        fun fromId(id: String?): OutputScale = entries.firstOrNull { it.id == id } ?: when (id) {
            "1.5" -> COMPACT
            "3" -> HIGH
            else -> STANDARD
        }
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

/**
 * 走查数据。
 *
 * 刻意**不含 history**——转换记录是用户自己的数据，上架包也要用；
 * 会话与事件是走查专用的，上架包里根本不存在（见 src/release 源集）。
 * 两者粒度不同、生命周期不同、合规要求也不同，混在一个快照里是上一版的隐患。
 */
data class TelemetrySnapshot(
    val sessions: List<TrialSession> = emptyList(),
    val currentSessionId: String? = null,
) {
    val currentSession: TrialSession?
        get() = sessions.firstOrNull { it.id == currentSessionId }
}

/**
 * 用户自己的数据。**上架包与走查包都需要**，与走查埋点严格分开存放。
 *
 * history 里含发票文件名——属个人信息，所以「我的」页必须提供清除入口，
 * 隐私说明也必须如实披露留存内容与条数。
 */
data class UserSnapshot(
    val history: List<ConversionRecord> = emptyList(),
    val preferredFormatId: String = OutputFormat.JPG.id,
    val preferredScaleId: String = OutputScale.STANDARD.id,
) {
    companion object {
        /** 转换记录保留上限。超过后丢弃最旧的一条。 */
        const val MAX_HISTORY = 50
    }
}
