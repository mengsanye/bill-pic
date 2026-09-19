package com.next.billpic.core.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.ConvertedPage
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.model.OutputScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 面向用户的转换失败原因，直接用于 HUD 提示。 */
class PdfConversionException(message: String) : Exception(message)

/**
 * PDF → 图片的转换引擎。
 *
 * 用 Android 系统自带的 [PdfRenderer] 在本机渲染，不依赖任何第三方库、不联网。
 * 这既是「不上传服务器」的技术保证，也让 APK 体积和启动耗时保持在很低的水平。
 */
object PdfConverter {

    /** 只读页数与基本合法性检查 */
    suspend fun pageCount(file: File): Int = withContext(Dispatchers.IO) {
        val renderer = openRenderer(file)
        try {
            renderer.pageCount
        } finally {
            renderer.close()
        }
    }

    /**
     * 逐页渲染并编码。
     *
     * @param onProgress (已完成页数, 总页数)，回调发生在 IO 线程，调用方自行切回主线程。
     */
    suspend fun convert(
        file: File,
        displayName: String,
        format: OutputFormat,
        scale: OutputScale,
        maxPages: Int = AppConfig.MAX_PAGES,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<ConvertedPage> = withContext(Dispatchers.IO) {
        val base = com.next.billpic.core.io.PickedFile.safeBaseName(displayName)
        val renderer = openRenderer(file)
        try {
            val total = min(renderer.pageCount, maxPages).coerceAtLeast(1)
            onProgress(0, total)

            val results = ArrayList<ConvertedPage>(total)
            for (index in 0 until total) {
                coroutineContext.ensureActive()

                val page = renderer.openPage(index)
                try {
                    val size = renderSize(page.width, page.height, scale.value)
                    val bitmap = createBitmap(size.first, size.second)
                    try {
                        // 发票底色是白的；显式铺白可避免 PNG 出现透明背景、JPG 出现黑底
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val bytes = bitmap.encode(format)
                        val suffix = if (total > 1) "-p" + (index + 1) else ""
                        results += ConvertedPage(
                            page = index + 1,
                            fileName = base + suffix + "." + format.ext,
                            bytes = bytes,
                            width = size.first,
                            height = size.second,
                        )
                    } finally {
                        bitmap.recycle()
                    }
                } finally {
                    page.close()
                }
                onProgress(index + 1, total)
            }
            results
        } finally {
            renderer.close()
        }
    }

    /* ------------------------------------------------------------------ */

    private fun openRenderer(file: File): PdfRenderer {
        if (!file.exists() || file.length() == 0L) {
            throw PdfConversionException("这个文件是空的")
        }
        val descriptor = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            throw PdfConversionException("读不到这个文件，换一个试试")
        }
        return try {
            PdfRenderer(descriptor)
        } catch (e: SecurityException) {
            descriptor.close()
            throw PdfConversionException("这个 PDF 有密码保护，请先解除密码")
        } catch (e: Exception) {
            descriptor.close()
            throw PdfConversionException("这个文件不是有效的 PDF，或已损坏")
        }
    }

    /** 由页面点尺寸推出输出像素尺寸，并做显存保护。 */
    private fun renderSize(pointWidth: Int, pointHeight: Int, scale: Float): Pair<Int, Int> {
        var width = (pointWidth * scale).roundToInt().coerceAtLeast(1)
        var height = (pointHeight * scale).roundToInt().coerceAtLeast(1)

        val dimRatio = min(
            AppConfig.MAX_RENDER_DIM / width.toFloat(),
            AppConfig.MAX_RENDER_DIM / height.toFloat(),
        )
        if (dimRatio < 1f) {
            width = (width * dimRatio).toInt().coerceAtLeast(1)
            height = (height * dimRatio).toInt().coerceAtLeast(1)
        }

        val pixels = width.toLong() * height.toLong()
        if (pixels > AppConfig.MAX_RENDER_PIXELS) {
            val ratio = sqrt(AppConfig.MAX_RENDER_PIXELS.toDouble() / pixels.toDouble())
            width = (width * ratio).toInt().coerceAtLeast(1)
            height = (height * ratio).toInt().coerceAtLeast(1)
        }
        return width to height
    }

    private fun createBitmap(width: Int, height: Int): Bitmap = try {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        throw PdfConversionException("图片尺寸太大，请把清晰度调低一档")
    }

    private fun Bitmap.encode(format: OutputFormat): ByteArray {
        val out = ByteArrayOutputStream(512 * 1024)
        val compressFormat = if (format == OutputFormat.PNG) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        val quality = if (format == OutputFormat.PNG) 100 else AppConfig.JPEG_QUALITY
        compress(compressFormat, quality, out)
        return out.toByteArray()
    }
}
