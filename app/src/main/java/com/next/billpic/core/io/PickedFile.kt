package com.next.billpic.core.io

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.next.billpic.core.pdf.PdfConversionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 选中文件的元信息读取与本地暂存。 */
object PickedFile {

    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|]")
    private val PDF_SUFFIX = Regex("(?i)\\.pdf$")
    private const val PDF_MIME = "application/pdf"

    /** 去掉扩展名、清除文件系统非法字符，作为输出文件名基名 */
    fun safeBaseName(name: String): String {
        val stripped = name.replace(PDF_SUFFIX, "").replace(ILLEGAL_CHARS, "_")
        return stripped.take(60).ifBlank { "invoice" }
    }

    fun displayName(context: Context, uri: Uri): String {
        val fromResolver = runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()

        return fromResolver?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "invoice.pdf"
    }

    fun sizeBytes(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else 0L
                } else {
                    0L
                }
            } ?: 0L
    }.getOrDefault(0L)

    fun mimeType(context: Context, uri: Uri): String =
        runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()

    fun isPdf(context: Context, uri: Uri, name: String): Boolean =
        mimeType(context, uri) == PDF_MIME || name.endsWith(".pdf", ignoreCase = true)

    /**
     * 把选中的文件复制到应用缓存目录再交给渲染引擎。
     *
     * 系统 [android.graphics.pdf.PdfRenderer] 要求文件描述符可随机读取，
     * 而部分「文档提供方」给出的 Uri 只支持流式读取，直接渲染会失败；
     * 先落盘既解决这个问题，也让「文件大小」有一个可靠来源。
     */
    suspend fun stagePdf(context: Context, uri: Uri, displayName: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "pdf_in")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            } else {
                dir.mkdirs()
            }
            val target = File(dir, safeBaseName(displayName) + ".pdf")
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw PdfConversionException("读不到这个文件，换一个试试")
                input.use { source ->
                    target.outputStream().use { output -> source.copyTo(output) }
                }
            } catch (e: PdfConversionException) {
                throw e
            } catch (e: Exception) {
                throw PdfConversionException("读不到这个文件，换一个试试")
            }
            if (target.length() == 0L) throw PdfConversionException("这个文件是空的")
            target
        }

    suspend fun clearStaged(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "pdf_in")
        if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
    }
}
