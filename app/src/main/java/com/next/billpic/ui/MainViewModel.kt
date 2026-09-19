package com.next.billpic.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.next.billpic.BuildConfig
import com.next.billpic.R
import com.next.billpic.core.data.Telemetry
import com.next.billpic.core.data.TelemetryProvider
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
import com.next.billpic.core.model.TelemetrySnapshot
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 底部 Tab。刻意只有三个，超过 5 个会让主导航变糊。 */
enum class AppTab(val label: String, val iconRes: Int) {
    CONVERT("转换", R.drawable.ic_tab_convert),
    RECORDS("记录", R.drawable.ic_tab_records),
    MINE("我的", R.drawable.ic_tab_mine),
}

/** 「我的」下的二级页面 */
enum class MineSub { NONE, PRIVACY, POLICY, TERMS, FAQ, ABOUT, VALIDATION }

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
    val feedbackOpen: Boolean = false,
    val hud: String? = null,
    val variant: String = AppConfig.RELEASE_CTA,
    val history: List<ConversionRecord> = emptyList(),
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val permissionGuideVisible: Boolean = false,
    val clearHistoryVisible: Boolean = false,
    val deleteRecordIndex: Int? = null,
    /** 上次转换所用的设置指纹，用来判断改了设置后要不要给「重新转换」出口 */
    val lastSignature: String = "",
) {
    val panelEnabled: Boolean get() = AppConfig.VALIDATION_PANEL_ENABLED

    val progress: Float
        get() = if (progressTotal <= 0) 0f else progressDone.toFloat() / progressTotal.toFloat()

    /** 结果页是否还能「换一份发票」——正在转换时不该让用户重复点。 */
    val canPickAnother: Boolean get() = !converting && !parsing

    /** 当前输出设置的指纹 */
    val currentSignature: String
        get() = format.id + "|" + scale.id + "|" + selectedPages.joinToString(",")

    /**
     * 是否该给用户一个「重新转换」的出口。
     *
     * 选好文件是自动开转的（少一步操作），但用户随后改了页码范围或输出档位时，
     * 必须有个地方让他把新设置跑一遍——否则设置改了却不生效，是个死胡同。
     */
    val settingsDirty: Boolean
        get() = source != null && !converting && !parsing && currentSignature != lastSignature
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    /** 用户自己的数据（上架包也有） */
    private val userData = UserDataStore(application)

    /**
     * 走查采集。debug 是真实现，release 是空对象——
     * 这里看不出区别，差异在构建期就决定了。
     */
    private val telemetry: Telemetry = TelemetryProvider.create(application)

    private var stagedFile: File? = null

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var convertJob: Job? = null

    init {
        val user = userData.load()
        val variant = telemetry.ensureSession()
        _state.value = _state.value.copy(
            variant = variant,
            format = OutputFormat.fromId(user.preferredFormatId),
            scale = OutputScale.fromId(user.preferredScaleId),
            history = user.history,
            telemetry = telemetry.snapshot(),
        )
        track("session_start", mapOf("variant" to variant, "engine" to "pdfrenderer"))
    }

    /* ------------------------------ 埋点 ------------------------------ */

    /**
     * 事件入口。
     *
     * 第一行的常量判断不是「防呆」而是**给 R8 看的**：
     * release 里 `BuildConfig.VALIDATION_PANEL` 是编译期常量 false，
     * R8 会把它折叠成空函数、内联掉所有调用点，于是连事件名字符串
     * （"conversion_start" 之类）都不会留在 dex 里。
     * 没有这行时，字符串会作为 no-op 调用的参数被保留下来——
     * 行为上无害，但反编译能看到，不利于「不含埋点」这个结论。
     */
    private fun track(name: String, props: Map<String, String> = emptyMap()) {
        if (!BuildConfig.VALIDATION_PANEL) return
        telemetry.track(name, currentScreenId(), props)
        val snapshot = telemetry.snapshot()
        if (snapshot !== _state.value.telemetry) {
            _state.value = _state.value.copy(telemetry = snapshot)
        }
    }

    private fun currentScreenId(): String {
        val current = _state.value
        return when {
            current.viewerPage != null -> "view-result"
            current.showResult -> "view-result"
            current.mineSub == MineSub.PRIVACY -> "view-mine"
            current.mineSub == MineSub.POLICY -> "view-mine"
            current.mineSub == MineSub.TERMS -> "view-mine"
            current.mineSub == MineSub.FAQ -> "view-mine"
            current.mineSub == MineSub.ABOUT -> "view-mine"
            current.mineSub == MineSub.VALIDATION -> "view-mine"
            current.tab == AppTab.RECORDS -> "view-records"
            current.tab == AppTab.MINE -> "view-mine"
            else -> "view-home"
        }
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
        track("tab_switch", mapOf("tab" to tab.name.lowercase(Locale.ROOT)))
        _state.value = _state.value.copy(tab = tab, mineSub = MineSub.NONE, showResult = false)
    }

    fun goToResult() {
        if (_state.value.results.isEmpty()) return
        track("go_result")
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
            current.feedbackOpen -> {
                closeFeedback()
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
        track("privacy_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.PRIVACY)
    }

    fun openPolicy() {
        track("policy_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.POLICY)
    }

    fun openTerms() {
        track("terms_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.TERMS)
    }

    fun openFaq() {
        track("faq_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.FAQ)
    }

    fun openAbout() {
        track("about_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.ABOUT)
    }

    fun openValidationPanel() {
        track("validation_panel_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.VALIDATION)
    }

    fun closeMineSub() {
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.NONE)
    }

    /** 打开备案系统网站，供用户按备案编号核查。 */
    fun openBeianPage() {
        launchExternal(
            Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.ICP_BEIAN_URL)),
            "没有可用的浏览器",
        )
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
        track("pick_tap")
    }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        val context = getApplication<Application>()
        val name = PickedFile.displayName(context, uri)

        if (!PickedFile.isPdf(context, uri, name)) {
            track(
                "file_reject",
                mapOf("ext" to name.substringAfterLast('.', "").lowercase(Locale.ROOT)),
            )
            hud("这不是 PDF 文件，请选择 .pdf 发票")
            return
        }

        track(
            "file_select",
            mapOf("name" to name, "size_kb" to (PickedFile.sizeBytes(context, uri) / 1024).toString()),
        )
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
                track("file_parsed", mapOf("pages" to pageCount.toString()))
                // 移动端少一步操作：选好文件直接开转，与原型一致
                delay(180)
                convertNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(parsing = false)
                val message = e.message ?: "读不出这个 PDF，换一个文件试试"
                track("file_parse_error", mapOf("message" to message.take(120)))
                hud(message)
            }
        }
    }

    /** 结果页「换一份发票」与首页「移除」共用。 */
    fun clearFile() {
        track("file_clear")
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
        track("format_select", mapOf("format" to format.id))
        userData.save(userData.load().copy(preferredFormatId = format.id))
        _state.value = _state.value.copy(format = format)
    }

    fun setScale(scale: OutputScale) {
        if (_state.value.scale == scale) return
        track("scale_select", mapOf("scale" to scale.id))
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
        track(
            "conversion_start",
            mapOf(
                "format" to format.id,
                "scale" to scale.id,
                "size_kb" to (source.sizeBytes / 1024).toString(),
                "selected_pages" to pages.size.toString(),
            ),
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

                track(
                    "conversion_done",
                    mapOf(
                        "pages" to converted.size.toString(),
                        "format" to format.id,
                        "scale" to scale.id,
                        "ms" to duration.toString(),
                        "out_kb" to (totalBytes / 1024).toString(),
                    ),
                )
                track("flow_complete", mapOf("pages" to converted.size.toString(), "format" to format.id))
                hud("${converted.size} 张图片已生成")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "转换失败，请换一个文件再试"
                track("conversion_error", mapOf("message" to message.take(160)))
                hud(message)
            } finally {
                _state.value = _state.value.copy(converting = false, progressDone = 0, progressTotal = 0)
            }
        }
    }

    /* ------------------------------ 结果与保存 ------------------------------ */

    fun openViewer(page: Int) {
        track("viewer_open", mapOf("page" to page.toString()))
        _state.value = _state.value.copy(viewerPage = page)
    }

    fun closeViewer() {
        val page = _state.value.viewerPage ?: return
        track("viewer_close", mapOf("page" to page.toString()))
        _state.value = _state.value.copy(viewerPage = null)
    }

    fun saveOne(page: Int) {
        val current = _state.value
        val target = current.results.firstOrNull { it.page == page } ?: return
        val format = current.format
        viewModelScope.launch {
            val result = MediaSaver.saveToGallery(getApplication(), target, format)
            if (result.isSuccess) {
                track("download_single", mapOf("page" to page.toString(), "format" to format.id))
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
        track("download_all", mapOf("pages" to pages.size.toString(), "format" to format.id))

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
            track("share_images", mapOf("pages" to uris.size.toString(), "format" to format.id))
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
        track("open_gallery")
        hud("图片在系统相册的 BillPic 相册里")
        launchExternal(MediaSaver.openGalleryIntent(), "没有找到可用的相册应用")
    }

    /* ------------------------------ 记录管理 ------------------------------ */

    fun recordTap(index: Int) {
        track("record_tap", mapOf("index" to index.toString()))
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
     * 这是用户对自己数据的控制权入口——上一版只有一个藏在验证看板里、
     * 文案叫「清空走查数据」的按钮，等于用户没有任何途径清掉自己的记录。
     * 记录里含发票文件名，属个人信息，这个入口是合规必需项。
     */
    fun confirmClearHistory() {
        val updated = userData.clearHistory()
        _state.value = _state.value.copy(
            history = updated.history,
            clearHistoryVisible = false,
        )
        track("history_clear")
        hud("转换记录已清空，相册里的图片不受影响")
    }

    /* ------------------------------ 反馈 ------------------------------ */

    fun openFeedback() {
        track("feedback_open")
        _state.value = _state.value.copy(feedbackOpen = true)
    }

    fun closeFeedback() {
        track("feedback_close")
        _state.value = _state.value.copy(feedbackOpen = false)
    }

    fun onRatingSelected(rating: Int) {
        track("rating_select", mapOf("rating" to rating.toString()))
    }

    fun onIntentSelected(intent: String) {
        track("intent_select", mapOf("value" to intent))
    }

    /** @return 是否提交成功（未打分时为 false，由界面提示用户） */
    fun submitFeedback(rating: Int, text: String, intent: String): Boolean {
        if (rating <= 0) return false
        val current = _state.value
        telemetry.recordFeedback(
            rating = rating,
            text = text,
            intent = intent,
            converted = current.results.isNotEmpty(),
            formatId = current.format.id,
            pages = current.source?.pageCount ?: 0,
        )
        _state.value = _state.value.copy(
            feedbackOpen = false,
            telemetry = telemetry.snapshot(),
        )
        track(
            "feedback_submit",
            mapOf(
                "rating" to rating.toString(),
                "has_text" to text.isNotBlank().toString(),
                "intent" to intent,
                "converted" to current.results.isNotEmpty().toString(),
            ),
        )
        hud("反馈已记录，感谢！")
        return true
    }

    /* ------------------------------ 意见反馈（上架通道） ------------------------------ */

    /**
     * 给真实用户的反馈通道。
     *
     * 与走查期的星级打分是两回事：前者是把意见送出去，后者是把结论留在本机。
     * 邮箱未配置时明确提示，不假装成功。
     */
    fun openFeedbackEmail() {
        track("feedback_email_open")
        val email = AppConfig.CONTACT_EMAIL
        if (AppConfig.isPlaceholder(email)) {
            hud("反馈邮箱还没配置，请开发同学填写 AppConfig.CONTACT_EMAIL")
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "BillPic 使用反馈（v${AppConfig.VERSION_NAME}）")
            putExtra(
                Intent.EXTRA_TEXT,
                "\n\n——\n设备：${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "系统：Android ${Build.VERSION.RELEASE}\n" +
                    "版本：${AppConfig.VERSION_NAME}",
            )
        }
        launchExternal(intent, "没有可用的邮件应用，可直接发到 $email")
    }

    /* ------------------------------ 走查数据 ------------------------------ */

    fun startNewTrialSession() {
        val variant = telemetry.startNewSession()
        stagedFile = null
        viewModelScope.launch { PickedFile.clearStaged(getApplication()) }
        _state.value = _state.value.copy(
            tab = AppTab.CONVERT,
            showResult = false,
            mineSub = MineSub.NONE,
            source = null,
            pageRangeInput = "",
            pageRangeError = null,
            pageRangeTruncated = false,
            selectedPages = emptyList(),
            results = emptyList(),
            lastDurationMs = 0L,
            lastTotalBytes = 0L,
            converting = false,
            progressDone = 0,
            progressTotal = 0,
            viewerPage = null,
            feedbackOpen = false,
            variant = variant,
            telemetry = telemetry.snapshot(),
            lastSignature = "",
        )
        track("session_start", mapOf("variant" to variant, "engine" to "pdfrenderer"))
        hud("已开始新的试用会话，请交给下一位同事")
    }

    /** 清空全部走查数据（验证看板专用）。用户数据请用 [confirmClearHistory]。 */
    fun resetAllData() {
        telemetry.reset()
        userData.clear()
        val variant = telemetry.startNewSession()
        stagedFile = null
        viewModelScope.launch { PickedFile.clearStaged(getApplication()) }
        _state.value = MainUiState(
            variant = variant,
            history = emptyList(),
            telemetry = telemetry.snapshot(),
        )
        track("session_start", mapOf("variant" to variant, "engine" to "pdfrenderer"))
        hud("已清空全部走查数据")
    }

    fun exportJson() {
        val payload = telemetry.exportJson()
        if (payload.isBlank()) return
        val fileName = "billpic-android-testdata-" + System.currentTimeMillis() + ".json"
        viewModelScope.launch {
            val uri = runCatching {
                MediaSaver.writeExport(getApplication(), payload, fileName)
            }.getOrNull()
            if (uri == null) {
                hud("导出失败，请重试")
                return@launch
            }
            track("data_export")
            launchExternal(
                MediaSaver.exportShareIntent(uri, fileName, "application/json", "导出走查数据"),
                "没有可用的分享应用",
            )
        }
    }

    fun copySummary() {
        val summary = telemetry.summaryText()
        if (summary.isBlank()) return
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("BillPic 走查摘要", summary))
        track("summary_copy")
        hud("走查摘要已复制")
    }

    /* ------------------------------ 诊断信息 ------------------------------ */

    /**
     * 导出诊断信息。
     *
     * 在没有联网权限的前提下，这是排查用户问题的唯一可靠路径：
     * **由用户主动发起**，内容只有设备环境 + 他自己的转换记录（本来就看得到）。
     * 不做任何被动采集，因此不违背「不联网」的承诺。
     */
    fun exportDiagnostics() {
        val current = _state.value
        val context = getApplication<Application>()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

        val text = buildString {
            appendLine("BillPic 诊断信息")
            appendLine("生成时间：${stamp.format(Date())}")
            appendLine()
            appendLine("【应用】")
            appendLine("名称：${AppConfig.APP_NAME}")
            appendLine("版本：${AppConfig.VERSION_NAME}（versionCode ${BuildConfig.VERSION_CODE}）")
            appendLine("转换引擎：Android PdfRenderer（本机渲染，无联网权限）")
            appendLine()
            appendLine("【设备】")
            appendLine("厂商型号：${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("系统版本：Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）")
            appendLine("系统架构：${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine()
            appendLine("【当前输出设置】")
            appendLine("格式：${current.format.label}")
            appendLine("档位：${current.scale.label}（${current.scale.value}x / 质量 ${current.scale.jpegQuality}）")
            appendLine()
            appendLine("【最近转换记录】共 ${current.history.size} 条")
            if (current.history.isEmpty()) {
                appendLine("（无）")
            } else {
                current.history.forEach { record ->
                    appendLine(
                        "· ${stamp.format(Date(record.timestamp))} | ${record.fileName} | " +
                            "${record.pages} 张 | ${record.totalBytes / 1024} KB | ${record.durationMs} ms",
                    )
                }
            }
        }

        viewModelScope.launch {
            val fileName = "billpic-diagnostics-" + System.currentTimeMillis() + ".txt"
            val uri = runCatching { MediaSaver.writeExport(context, text, fileName) }.getOrNull()
            if (uri == null) {
                hud("诊断信息导出失败，请重试")
                return@launch
            }
            track("diagnostics_export")
            launchExternal(
                MediaSaver.exportShareIntent(uri, fileName, "text/plain", "发送诊断信息"),
                "没有可用的分享应用",
            )
        }
    }
}
