package com.next.billpic.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.next.billpic.R
import com.next.billpic.core.data.UserDataStore
import com.next.billpic.core.io.PickedFile
import com.next.billpic.core.media.MediaSaver
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.ConversionRecord
import com.next.billpic.core.model.ConvertedPage
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.model.OutputScale
import com.next.billpic.core.model.PageRange
import com.next.billpic.core.model.PdfSource
import com.next.billpic.core.pdf.PdfConversionException
import com.next.billpic.core.pdf.PdfConverter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** 底部 Tab。刻意只有三个，超过 5 个会让主导航变糊。 */
enum class AppTab(val label: String, val iconRes: Int) {
    CONVERT("转换", R.drawable.ic_tab_convert),
    RECORDS("记录", R.drawable.ic_tab_records),
    MINE("我的", R.drawable.ic_tab_mine),
}

/** 「我的」下的二级页面 */
enum class MineSub { NONE, PRIVACY, FAQ, ABOUT }

/** 界面状态。单一数据源，所有交互都通过 ViewModel 改这里。 */
data class MainUiState(
    val tab: AppTab = AppTab.CONVERT,
    val showResult: Boolean = false,
    val mineSub: MineSub = MineSub.NONE,
    val format: OutputFormat = OutputFormat.JPG,
    val scale: OutputScale = OutputScale.STANDARD,
    val source: PdfSource? = null,
    val pageRangeInput: String = "",
    val pageRangeError: String? = null,
    val pageRangeTruncated: Boolean = false,
    val selectedPages: List<Int> = emptyList(),
    val parsing: Boolean = false,
    val converting: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val results: List<ConvertedPage> = emptyList(),
    val lastDurationMs: Long = 0L,
    val lastTotalBytes: Long = 0L,
    val savingAll: Boolean = false,
    val viewerPage: Int? = null,
    val hud: String? = null,
    val history: List<ConversionRecord> = emptyList(),
    val permissionGuideVisible: Boolean = false,
    val clearHistoryVisible: Boolean = false,
    val deleteRecordIndex: Int? = null,
    /** 上次转换所用的设置指纹，用来判断改了设置后要不要给「重新转换」出口 */
    val lastSignature: String = "",
) {
    val progress: Float
        get() = if (progressTotal <= 0) 0f else progressDone.toFloat() / progressTotal.toFloat()

    /** 结果页是否还能「换一份发票」——正在转换时不该让用户重复点。 */
    val canPickAnother: Boolean get() = !converting && !parsing

    /** 当前输出设置的指纹 */
    val currentSignature: String
        get() = format.id + "|" + scale.id + "|" + selectedPages.joinToString(",")

    /**
     * 是否该让用户显式点一次「开始转换」。
     *
     * 单页 PDF 解析完直接开转 —— 没有可做的决定，省一步是净收益。
     * 多于 1 页时停下来等确认：**页码设置到这一刻才第一次有意义**，
     * 先按默认值转完再让用户改，等于让他「修正」而不是「配置」。
     */
    val needsExplicitConvert: Boolean
        get() = source != null && results.isEmpty() && !converting && !parsing

    /**
     * 是否该给用户一个「重新转换」的出口。
     *
     * 用户事后改了页码范围或输出档位时，必须有个地方让他把新设置跑一遍 ——
     * 否则设置改了却不生效，是个死胡同。
     *
     * 前提是**已经成功转换过一次**（`lastSignature` 在成功分支才写入）：
     * 少了这个判断，在「等用户点开始转换」的阶段会冒出一个功能重复的按钮。
     */
    val settingsDirty: Boolean
        get() = source != null && lastSignature.isNotEmpty() &&
            !converting && !parsing && currentSignature != lastSignature
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    /** 用户数据：转换记录与输出偏好 */
    private val userData = UserDataStore(application)

    private var stagedFile: File? = null

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var convertJob: Job? = null

    init {
        val user = userData.load()
        _state.value = _state.value.copy(
            format = OutputFormat.fromId(user.preferredFormatId),
            scale = OutputScale.fromId(user.preferredScaleId),
            history = user.history,
        )
    }

    private fun hud(text: String) {
        _state.value = _state.value.copy(hud = text)
    }

    fun consumeHud() {
        if (_state.value.hud != null) _state.value = _state.value.copy(hud = null)
    }

    /* ------------------------------ 外部跳转 ------------------------------ */

    /**
     * 启动外部 Activity。
     *
     * 刻意用 try/catch 而不是 `resolveActivity`：Android 11 起应用可见性受限，
     * 没在 `<queries>` 里声明的 intent 用 resolveActivity 会误判为「无应用可处理」，
     * 直接 startActivity 让系统判定更可靠。
     */
    private fun launchExternal(intent: Intent, failureMessage: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
            .onFailure { hud(failureMessage) }
    }

    /* ------------------------------ 导航 ------------------------------ */

    fun selectTab(tab: AppTab) {
        if (_state.value.tab == tab) return
        _state.value = _state.value.copy(tab = tab, mineSub = MineSub.NONE, showResult = false)
    }

    fun goToResult() {
        if (_state.value.results.isEmpty()) return
        _state.value = _state.value.copy(
            tab = AppTab.CONVERT,
            showResult = true,
            mineSub = MineSub.NONE,
        )
    }

    fun closeResult() {
        _state.value = _state.value.copy(showResult = false, tab = AppTab.CONVERT)
    }

    /** 返回键统一处理，返回 true 表示已消费 */
    fun handleBack(): Boolean {
        val current = _state.value
        return when {
            current.permissionGuideVisible -> {
                dismissPermissionGuide()
                true
            }
            current.clearHistoryVisible -> {
                dismissClearHistory()
                true
            }
            current.deleteRecordIndex != null -> {
                dismissDeleteRecord()
                true
            }
            current.viewerPage != null -> {
                closeViewer()
                true
            }
            current.mineSub != MineSub.NONE -> {
                closeMineSub()
                true
            }
            current.showResult -> {
                closeResult()
                true
            }
            current.tab != AppTab.CONVERT -> {
                _state.value = current.copy(tab = AppTab.CONVERT)
                true
            }
            else -> false
        }
    }

    /* ------------------------------ 我的：二级页面 ------------------------------ */

    fun openPrivacy() {
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.PRIVACY)
    }

    fun openFaq() {
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.FAQ)
    }

    fun openAbout() {
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.ABOUT)
    }

    fun closeMineSub() {
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.NONE)
    }

    /** 打开系统「应用详情」页，用户可在那里重新授予权限。 */
    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", getApplication<Application>().packageName, null),
        )
        launchExternal(intent, "打不开系统设置，请手动到「设置 → 应用」里开启存储权限")
    }

    /* ------------------------------ 选文件 ------------------------------ */

    fun onPickTapped() {
    }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        val context = getApplication<Application>()
        val name = PickedFile.displayName(context, uri)

        if (!PickedFile.isPdf(context, uri, name)) {
            hud("这不是 PDF 文件，请选择 .pdf 发票")
            return
        }

        _state.value = _state.value.copy(
            parsing = true,
            results = emptyList(),
            showResult = false,
            lastDurationMs = 0L,
            lastTotalBytes = 0L,
            pageRangeInput = "",
            pageRangeError = null,
            pageRangeTruncated = false,
            selectedPages = emptyList(),
            // 换成新文件，上一次的转换指纹随之作废。
            // 不重置的话，新文件的页数/格式与上一份不同，立刻会被误判成
            // 「设置已被修改」，「重新转换」按钮会和「开始转换」同时冒出来。
            lastSignature = "",
        )

        viewModelScope.launch {
            try {
                val staged = PickedFile.stagePdf(context, uri, name)
                val pageCount = PdfConverter.pageCount(staged)
                stagedFile = staged
                val allPages = (1..pageCount).take(AppConfig.MAX_PAGES)
                _state.value = _state.value.copy(
                    parsing = false,
                    source = PdfSource(
                        uri = uri,
                        displayName = name,
                        sizeBytes = staged.length(),
                        pageCount = pageCount,
                    ),
                    selectedPages = allPages,
                    pageRangeTruncated = pageCount > AppConfig.MAX_PAGES,
                )
                // 只在「没有可做的决定」时才自动开转：
                //   单页 → 没什么可配置的，省一步是净收益（增值税电子普通发票基本都是一页）
                //   多页 → 页码此刻才第一次有意义，先转完再让用户改是本末倒置
                if (pageCount <= 1) {
                    delay(180)
                    convertNow()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(parsing = false)
                val message = e.message ?: "读不出这个 PDF，换一个文件试试"
                hud(message)
            }
        }
    }

    /** 结果页「换一份发票」与首页「移除」共用。 */
    fun clearFile() {
        stagedFile = null
        viewModelScope.launch { PickedFile.clearStaged(getApplication()) }
        _state.value = _state.value.copy(
            source = null,
            results = emptyList(),
            showResult = false,
            lastDurationMs = 0L,
            lastTotalBytes = 0L,
            pageRangeInput = "",
            pageRangeError = null,
            pageRangeTruncated = false,
            selectedPages = emptyList(),
            tab = AppTab.CONVERT,
            lastSignature = "",
        )
    }

    /* ------------------------------ 输出设置 ------------------------------ */

    fun setFormat(format: OutputFormat) {
        if (_state.value.format == format) return
        userData.save(userData.load().copy(preferredFormatId = format.id))
        _state.value = _state.value.copy(format = format)
    }

    fun setScale(scale: OutputScale) {
        if (_state.value.scale == scale) return
        userData.save(userData.load().copy(preferredScaleId = scale.id))
        _state.value = _state.value.copy(scale = scale)
    }

    /**
     * 页码范围输入。
     *
     * 边输边校验：失败时把原因写在输入框下方，而不是等用户点了转换才告诉他。
     * 输入非法时 `selectedPages` 置空，[convertNow] 据此拦住转换。
     */
    fun setPageRange(input: String) {
        val source = _state.value.source ?: return
        when (val result = PageRange.parse(input, source.pageCount)) {
            is PageRange.Result.Success -> _state.value = _state.value.copy(
                pageRangeInput = input,
                pageRangeError = null,
                pageRangeTruncated = result.truncated,
                selectedPages = result.pages,
            )

            is PageRange.Result.Failure -> _state.value = _state.value.copy(
                pageRangeInput = input,
                pageRangeError = result.message,
                pageRangeTruncated = false,
                selectedPages = emptyList(),
            )
        }
    }

    /* ------------------------------ 转换 ------------------------------ */

    /**
     * 执行转换。两个入口共用：用户点「开始转换 / 重新转换」，
     * 或单页 PDF 选完即转（见 [onFilePicked]——只有一页时没有可做的决定，省掉那一步）。
     */
    fun convertNow() {
        val current = _state.value
        val source = current.source ?: return
        if (current.converting) return

        if (current.selectedPages.isEmpty()) {
            hud(current.pageRangeError ?: "请先确认要转换的页码")
            return
        }

        val file = stagedFile
        if (file == null || !file.exists()) {
            hud("文件已失效，请重新选择发票")
            return
        }

        val format = current.format
        val scale = current.scale
        val pages = current.selectedPages
        _state.value = current.copy(
            converting = true,
            progressDone = 0,
            progressTotal = pages.size,
            showResult = false,
        )

        convertJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                val converted = PdfConverter.convert(
                    file = file,
                    displayName = source.displayName,
                    format = format,
                    scale = scale,
                    pageIndices = pages,
                    onProgress = { done, total ->
                        _state.value = _state.value.copy(progressDone = done, progressTotal = total)
                    },
                )
                if (converted.isEmpty()) throw PdfConversionException("一页都没渲染出来，换一个文件试试")

                val duration = System.currentTimeMillis() - startedAt
                val totalBytes = converted.sumOf { it.bytes.size.toLong() }
                val record = ConversionRecord(
                    timestamp = System.currentTimeMillis(),
                    fileName = source.displayName,
                    pages = converted.size,
                    formatId = format.id,
                    scaleValue = scale.value,
                    totalBytes = totalBytes,
                    durationMs = duration,
                )
                val updatedUser = userData.appendRecord(record)

                _state.value = _state.value.copy(
                    results = converted,
                    lastDurationMs = duration,
                    lastTotalBytes = totalBytes,
                    showResult = true,
                    tab = AppTab.CONVERT,
                    mineSub = MineSub.NONE,
                    history = updatedUser.history,
                    lastSignature = format.id + "|" + scale.id + "|" +
                        pages.joinToString(","),
                )

                hud("${converted.size} 张图片已生成")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "转换失败，请换一个文件再试"
                hud(message)
            } finally {
                _state.value = _state.value.copy(converting = false, progressDone = 0, progressTotal = 0)
            }
        }
    }

    /* ------------------------------ 结果与保存 ------------------------------ */

    fun openViewer(page: Int) {
        _state.value = _state.value.copy(viewerPage = page)
    }

    fun closeViewer() {
        val page = _state.value.viewerPage ?: return
        _state.value = _state.value.copy(viewerPage = null)
    }

    fun saveOne(page: Int) {
        val current = _state.value
        val target = current.results.firstOrNull { it.page == page } ?: return
        val format = current.format
        viewModelScope.launch {
            val result = MediaSaver.saveToGallery(getApplication(), target, format)
            if (result.isSuccess) {
                hud("已保存到相册，可在「相册」中查看")
            } else {
                hud("保存失败：" + (result.exceptionOrNull()?.message ?: "未知原因"))
            }
        }
    }

    fun saveAll() {
        val current = _state.value
        if (current.results.isEmpty()) {
            hud("还没有可保存的图片")
            return
        }
        if (current.savingAll) return

        val pages = current.results
        val format = current.format
        _state.value = current.copy(savingAll = true)

        viewModelScope.launch {
            var succeeded = 0
            pages.forEach { page ->
                if (MediaSaver.saveToGallery(getApplication(), page, format).isSuccess) {
                    succeeded++
                }
                delay(120)
            }
            _state.value = _state.value.copy(savingAll = false)
            hud(
                if (succeeded == pages.size) {
                    "$succeeded 张已保存到相册，可在「相册」中查看"
                } else {
                    "保存了 $succeeded/${pages.size} 张，失败的可以再存一次"
                },
            )
        }
    }

    fun shareAll() {
        val current = _state.value
        if (current.results.isEmpty()) {
            hud("还没有可分享的图片")
            return
        }
        sharePages(current.results, current.format)
    }

    /** 单页分享：一页发票要单独发给同事时不必把整份都发出去。 */
    fun shareOne(page: Int) {
        val current = _state.value
        val target = current.results.firstOrNull { it.page == page } ?: return
        sharePages(listOf(target), current.format)
    }

    private fun sharePages(pages: List<ConvertedPage>, format: OutputFormat) {
        viewModelScope.launch {
            val uris = runCatching { MediaSaver.shareUris(getApplication(), pages) }.getOrNull()
            if (uris.isNullOrEmpty()) {
                hud("分享准备失败，请重试")
                return@launch
            }
            _state.value = _state.value.copy(viewerPage = null)
            launchExternal(MediaSaver.shareIntent(uris, format), "没有可用的分享应用")
        }
    }

    /* ------------------------------ 存储权限 ------------------------------ */

    /** Android 9 及以下拒绝存储权限时：给一条能真的走下去的路，而不只是提示。 */
    fun storagePermissionDenied() {
        _state.value = _state.value.copy(permissionGuideVisible = true)
    }

    fun dismissPermissionGuide() {
        _state.value = _state.value.copy(permissionGuideVisible = false)
    }

    /** 记录页：图片在系统相册，带用户去能看到图的地方。 */
    fun openGallery() {
        hud("图片在系统相册的 BillPic 相册里")
        launchExternal(MediaSaver.openGalleryIntent(), "没有找到可用的相册应用")
    }

    /* ------------------------------ 记录管理 ------------------------------ */

    fun recordTap(index: Int) {
        val current = _state.value
        // 最近一次且结果还在内存里，直接回结果页；否则带用户去相册看原图
        if (index == 0 && current.results.isNotEmpty()) {
            goToResult()
        } else {
            openGallery()
        }
    }

    fun requestDeleteRecord(index: Int) {
        if (index !in _state.value.history.indices) return
        _state.value = _state.value.copy(deleteRecordIndex = index)
    }

    fun dismissDeleteRecord() {
        _state.value = _state.value.copy(deleteRecordIndex = null)
    }

    fun confirmDeleteRecord() {
        val index = _state.value.deleteRecordIndex ?: return
        val updated = userData.removeRecordAt(index)
        _state.value = _state.value.copy(
            history = updated.history,
            deleteRecordIndex = null,
        )
        hud("已删除这条记录")
    }

    /* ------------------------------ 数据控制权 ------------------------------ */

    fun requestClearHistory() {
        _state.value = _state.value.copy(clearHistoryVisible = true)
    }

    fun dismissClearHistory() {
        _state.value = _state.value.copy(clearHistoryVisible = false)
    }

    /**
     * 清除转换记录。
     *
     * 记录里含发票文件名，属个人信息，所以必须给用户一个明确的删除入口。
     * 只管应用内的记录，不动相册里的图片——两者本就是分开的，界面文案也这么说。
     */
    fun confirmClearHistory() {
        val updated = userData.clearHistory()
        _state.value = _state.value.copy(
            history = updated.history,
            clearHistoryVisible = false,
        )
        hud("转换记录已清空，相册里的图片不受影响")
    }

    /* ------------------------------ 外部链接 ------------------------------ */

    /** 打开项目主页。 */
    fun openProjectPage() {
        launchExternal(
            Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PROJECT_URL)),
            "没有可用的浏览器，可手动访问 ${AppConfig.PROJECT_URL}",
        )
    }

    /**
     * 打开项目的 issue 列表。
     *
     * 开源项目不需要应用内反馈表单：意见写在 issue 里可被所有人检索，
     * 也省掉「提交 → 本地存储 → 导出 JSON」这一整条链路。
     */
    fun openIssues() {
        launchExternal(
            Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.ISSUES_URL)),
            "没有可用的浏览器，可手动访问 ${AppConfig.ISSUES_URL}",
        )
    }
}
