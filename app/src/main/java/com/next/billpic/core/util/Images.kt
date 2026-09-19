package com.next.billpic.core.util

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 图片解码。列表用采样解码，避免 30 页全尺寸位图把内存顶爆。 */
object Images {

    fun decode(bytes: ByteArray): ImageBitmap? = runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()

    /**
     * 按最长边采样解码。
     * @param maxDimension 期望的最长边像素；实际结果会落在 (maxDimension, maxDimension*2] 之间。
     */
    fun decodeSampled(bytes: ByteArray, maxDimension: Int): ImageBitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sampleSize = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }.getOrNull()
}

/** 展示用格式化，与原型保持一致的口径。 */
object Formatters {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    fun bytes(value: Long): String = when {
        value <= 0L -> "—"
        value < 1024 -> "$value B"
        value < 1024 * 1024 -> "${value / 1024} KB"
        else -> String.format(Locale.CHINA, "%.2f MB", value / 1024.0 / 1024.0)
    }

    fun seconds(millis: Long): String = String.format(Locale.CHINA, "%.2f 秒", millis / 1000.0)

    fun oneDecimalSeconds(millis: Long): String =
        String.format(Locale.CHINA, "%.1f 秒", millis / 1000.0)

    fun time(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun clock(timestamp: Long): String = clockFormat.format(Date(timestamp))

    fun stars(rating: Int): String = "★".repeat(rating.coerceIn(0, 5)) +
        "☆".repeat((5 - rating).coerceIn(0, 5))
}
