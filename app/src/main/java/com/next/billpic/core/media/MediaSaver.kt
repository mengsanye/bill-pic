package com.next.billpic.core.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.next.billpic.core.model.ConvertedPage
import com.next.billpic.core.model.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 保存到相册与分享。
 *
 * 图片统一落到系统相册的 BillPic 目录，用户能直接在「相册 / 文件」里找到，
 * 这比自己维护一个应用内图库更符合手机使用习惯。
 */
object MediaSaver {

    private const val ALBUM = "BillPic"
    private const val SHARE_DIR = "share"

    /** 单张保存到系统相册 */
    suspend fun saveToGallery(
        context: Context,
        page: ConvertedPage,
        format: OutputFormat,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, page, format)
            } else {
                saveToPublicDir(context, page, format)
            }
        }
    }

    private fun saveWithMediaStore(context: Context, page: ConvertedPage, format: OutputFormat) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, page.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mime)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + ALBUM,
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: throw IOException("相册写入失败")
        try {
            resolver.openOutputStream(uri)?.use { output -> output.write(page.bytes) }
                ?: throw IOException("相册写入失败")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToPublicDir(context: Context, page: ConvertedPage, format: OutputFormat) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            ALBUM,
        )
        if (!dir.exists() && !dir.mkdirs()) throw IOException("相册目录创建失败")
        val file = File(dir, page.fileName)
        file.outputStream().use { output -> output.write(page.bytes) }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(format.mime),
            null,
        )
    }

    /** 把结果写到缓存并生成可分享的 FileProvider Uri */
    suspend fun shareUris(
        context: Context,
        pages: List<ConvertedPage>,
    ): List<Uri> = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, SHARE_DIR)
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        } else {
            dir.mkdirs()
        }
        val authority = context.packageName + ".fileprovider"
        pages.map { page ->
            val file = File(dir, page.fileName)
            file.outputStream().use { output -> output.write(page.bytes) }
            FileProvider.getUriForFile(context, authority, file)
        }
    }

    fun shareIntent(uris: List<Uri>, format: OutputFormat): Intent {
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = format.mime
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = format.mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(intent, "分享发票图片").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** 导出走查数据为 JSON 文件，返回可分享的 Uri */
    suspend fun writeExport(
        context: Context,
        json: String,
        fileName: String,
    ): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "export")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.writeText(json)
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    fun exportShareIntent(uri: Uri, fileName: String): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(intent, "导出走查数据").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
