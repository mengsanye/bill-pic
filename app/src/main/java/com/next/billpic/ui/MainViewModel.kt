package com.next.billpic.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.next.billpic.R
import com.next.billpic.core.data.TelemetryStore
import com.next.billpic.core.io.PickedFile
import com.next.billpic.core.media.MediaSaver
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.ConversionRecord
import com.next.billpic.core.model.ConvertedPage
import com.next.billpic.core.model.FeedbackEntry
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.model.OutputScale
import com.next.billpic.core.model.PdfSource
import com.next.billpic.core.model.TelemetrySnapshot
import com.next.billpic.core.model.TrackedEvent
import com.next.billpic.core.model.TrialSession
import com.next.billpic.core.model.ValidationCalculator
import com.next.billpic.core.pdf.PdfConversionException
import com.next.billpic.core.pdf.PdfConverter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** 底部 Tab。刻意只有三个，超过 5 个会让主导航变糊。 */
enum class AppTab(val label: String, val iconRes: Int) {
    CONVERT("转换", R.drawable.ic_tab_convert),
    RECORDS("记录", R.drawable.ic_tab_records),
    MINE("我的", R.drawable.ic_tab_mine),
}

/** 「我的」下的二级页面 */
enum class MineSub { NONE, PRIVACY, VALIDATION }

/** 界面状态。单一数据源，所有交互都通过 ViewModel 改这里。 */
data class MainUiState(
    val tab: AppTab = AppTab.CONVERT,
    val showResult: Boolean = false,
    val mineSub: MineSub = MineSub.NONE,
    val format: OutputFormat = OutputFormat.JPG,
    val scale: OutputScale = OutputScale.HIGH,
    val source: PdfSource? = null,
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
    val variant: String = AppConfig.AB_VARIANTS.first(),
    val history: List<ConversionRecord> = emptyList(),
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val shareRequest: Intent? = null,
) {
    val panelEnabled: Boolean get() = AppConfig.VALIDATION_PANEL_ENABLED
    val progress: Float
        get() = if (progressTotal <= 0) 0f else progressDone.toFloat() / progressTotal.toFloat()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TelemetryStore(application)
    private var snapshot: TelemetrySnapshot = store.load()
    private var stagedFile: File? = null

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var convertJob: Job? = null

    init {
        val session = snapshot.currentSession ?: createSession()
        _state.value = _state.value.copy(
            variant = session.variant,
            format = OutputFormat.fromId(snapshot.preferredFormatId),
            scale = OutputScale.fromId(snapshot.preferredScaleId),
            history = snapshot.history,
            telemetry = snapshot,
        )
        track("session_start", mapOf("variant" to session.variant, "engine" to "pdfrenderer"))
    }

    /* ------------------------------ 埋点 ------------------------------ */

    private fun track(name: String, props: Map<String, String> = emptyMap()) {
        val sessionId = snapshot.currentSessionId ?: return
        val event = TrackedEvent(
            timestamp = System.currentTimeMillis(),
            name = name,
            screen = currentScreenId(),
            props = props,
        )
        snapshot = snapshot.copy(
            sessions = snapshot.sessions.map { session ->
                if (session.id == sessionId) session.copy(events = session.events + event) else session
            },
        )
        store.save(snapshot)
        _state.value = _state.value.copy(telemetry = snapshot)
    }

    private fun currentScreenId(): String {
        val current = _state.value
        return when {
            current.viewerPage != null -> "view-viewer"
            current.showResult -> "view-result"
            current.mineSub == MineSub.PRIVACY -> "view-privacy"
            current.mineSub == MineSub.VALIDATION -> "view-panel"
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

    fun consumeShareRequest() {
        if (_state.value.shareRequest != null) _state.value = _state.value.copy(shareRequest = null)
    }

    /** Android 9 及以下拒绝存储权限时的提示 */
    fun storagePermissionDenied() {
        hud("没有存储权限，无法写入相册，请在系统设置里允许后重试")
    }

    private fun createSession(): TrialSession {
        val id = "s" + System.currentTimeMillis().toString(36) + (100..999).random()
        val session = TrialSession(
            id = id,
            startedAt = System.currentTimeMillis(),
            variant = AppConfig.assignVariant(id),
        )
        snapshot = snapshot.copy(
            sessions = snapshot.sessions + session,
            currentSessionId = id,
        )
        return session
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
        _state.value = _state.value.copy(tab = AppTab.CONVERT, showResult = true, mineSub = MineSub.NONE)
    }

    fun closeResult() {
        _state.value = _state.value.copy(showResult = false, tab = AppTab.CONVERT)
    }

    /** 返回键统一处理，返回 true 表示已消费 */
    fun handleBack(): Boolean {
        val current = _state.value
        return when {
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

    fun openPrivacy() {
        track("privacy_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.PRIVACY)
    }

    fun openValidationPanel() {
        track("validation_panel_open")
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.VALIDATION)
    }

    fun closeMineSub() {
        _state.value = _state.value.copy(tab = AppTab.MINE, mineSub = MineSub.NONE)
    }

    fun showAbout() {
        track("about_open")
        hud("这是验证用原型，按真实习惯使用即可")
    }

    fun recordTap() {
        track("record_tap")
        hud("图片在系统相册里，这里只保留转换记录")
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
            track("file_reject", mapOf("ext" to name.substringAfterLast('.', "").lowercase(Locale.ROOT)))
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
        )

        viewModelScope.launch {
            try {
                val staged = PickedFile.stagePdf(context, uri, name)
                val pageCount = PdfConverter.pageCount(staged)
                stagedFile = staged
                _state.value = _state.value.copy(
                    parsing = false,
                    source = PdfSource(
                        uri = uri,
                        displayName = name,
                        sizeBytes = staged.length(),
                        pageCount = pageCount,
                    ),
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
        )
    }

    /* ------------------------------ 输出设置 ------------------------------ */

    fun setFormat(format: OutputFormat) {
        if (_state.value.format == format) return
        track("format_select", mapOf("format" to format.id))
        snapshot = snapshot.copy(preferredFormatId = format.id)
        store.save(snapshot)
        _state.value = _state.value.copy(format = format)
    }

    fun setScale(scale: OutputScale) {
        if (_state.value.scale == scale) return
        track("scale_select", mapOf("scale" to scale.id))
        snapshot = snapshot.copy(preferredScaleId = scale.id)
        store.save(snapshot)
        _state.value = _state.value.copy(scale = scale)
    }

    /* ------------------------------ 转换 ------------------------------ */

    fun convertNow() {
        val current = _state.value
        val source = current.source ?: return
        if (current.converting) return
        val file = stagedFile
        if (file == null || !file.exists()) {
            hud("文件已失效，请重新选择发票")
            return
        }

        val format = current.format
        val scale = current.scale
        _state.value = current.copy(
            converting = true,
            progressDone = 0,
            progressTotal = 1,
            showResult = false,
        )
        track(
            "conversion_start",
            mapOf(
                "format" to format.id,
                "scale" to scale.id,
                "size_kb" to (source.sizeBytes / 1024).toString(),
            ),
        )

        convertJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                val pages = PdfConverter.convert(
                    file = file,
                    displayName = source.displayName,
                    format = format,
                    scale = scale,
                    onProgress = { done, total ->
                        _state.value = _state.value.copy(progressDone = done, progressTotal = total)
                    },
                )
                if (pages.isEmpty()) throw PdfConversionException("一页都没渲染出来，换一个文件试试")

                val duration = System.currentTimeMillis() - startedAt
                val totalBytes = pages.sumOf { it.bytes.size.toLong() }
                val record = ConversionRecord(
                    timestamp = System.currentTimeMillis(),
                    fileName = source.displayName,
                    pages = pages.size,
                    formatId = format.id,
                    scaleValue = scale.value,
                    totalBytes = totalBytes,
                    durationMs = duration,
                )
                snapshot = snapshot.copy(history = (listOf(record) + snapshot.history).take(50))
                store.save(snapshot)

                _state.value = _state.value.copy(
                    results = pages,
                    lastDurationMs = duration,
                    lastTotalBytes = totalBytes,
                    showResult = true,
                    tab = AppTab.CONVERT,
                    mineSub = MineSub.NONE,
                    history = snapshot.history,
                )

                track(
                    "conversion_done",
                    mapOf(
                        "pages" to pages.size.toString(),
                        "format" to format.id,
                        "scale" to scale.id,
                        "ms" to duration.toString(),
                        "out_kb" to (totalBytes / 1024).toString(),
                    ),
                )
                track("flow_complete", mapOf("pages" to pages.size.toString(), "format" to format.id))
                hud("${pages.size} 张图片已生成")
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
        val pages = current.results
        val format = current.format
        viewModelScope.launch {
            val uris = runCatching { MediaSaver.shareUris(getApplication(), pages) }.getOrNull()
            if (uris.isNullOrEmpty()) {
                hud("分享准备失败，请重试")
                return@launch
            }
            track("share_images", mapOf("pages" to uris.size.toString(), "format" to format.id))
            _state.value = _state.value.copy(shareRequest = MediaSaver.shareIntent(uris, format))
        }
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
        val converted = current.results.isNotEmpty()
        val sessionId = snapshot.currentSessionId
        val entry = FeedbackEntry(
            timestamp = System.currentTimeMillis(),
            rating = rating,
            text = text.trim(),
            intent = intent,
            converted = converted,
            formatId = current.format.id,
            pages = current.source?.pageCount ?: 0,
        )
        snapshot = snapshot.copy(
            sessions = snapshot.sessions.map { session ->
                if (session.id == sessionId) session.copy(feedback = session.feedback + entry) else session
            },
        )
        store.save(snapshot)
        _state.value = _state.value.copy(feedbackOpen = false, telemetry = snapshot)

        track(
            "feedback_submit",
            mapOf(
                "rating" to rating.toString(),
                "has_text" to text.isNotBlank().toString(),
                "intent" to intent,
                "converted" to converted.toString(),
            ),
        )
        hud("反馈已记录，感谢！")
        return true
    }

    /* ------------------------------ 走查数据 ------------------------------ */

    fun startNewTrialSession() {
        val session = createSession()
        store.save(snapshot)
        stagedFile = null
        viewModelScope.launch { PickedFile.clearStaged(getApplication()) }
        _state.value = _state.value.copy(
            tab = AppTab.CONVERT,
            showResult = false,
            mineSub = MineSub.NONE,
            source = null,
            results = emptyList(),
            lastDurationMs = 0L,
            lastTotalBytes = 0L,
            converting = false,
            progressDone = 0,
            progressTotal = 0,
            viewerPage = null,
            feedbackOpen = false,
            variant = session.variant,
            history = snapshot.history,
            telemetry = snapshot,
        )
        track("session_start", mapOf("variant" to session.variant, "engine" to "pdfrenderer"))
        hud("已开始新的试用会话，请交给下一位同事")
    }

    fun resetAllData() {
        store.clear()
        snapshot = TelemetrySnapshot()
        val session = createSession()
        store.save(snapshot)
        stagedFile = null
        viewModelScope.launch { PickedFile.clearStaged(getApplication()) }
        _state.value = MainUiState(
            variant = session.variant,
            history = emptyList(),
            telemetry = snapshot,
        )
        track("session_start", mapOf("variant" to session.variant, "engine" to "pdfrenderer"))
        hud("已清空全部走查数据")
    }

    fun exportJson() {
        val payload = buildExportJson()
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
            _state.value = _state.value.copy(
                shareRequest = MediaSaver.exportShareIntent(uri, fileName),
            )
        }
    }

    fun copySummary() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("BillPic 走查摘要", buildSummaryText()))
        track("summary_copy")
        hud("走查摘要已复制")
    }

    private fun buildSummaryText(): String {
        val metrics = ValidationCalculator.compute(snapshot)
        val feedback = snapshot.sessions.flatMap { it.feedback }.sortedBy { it.timestamp }
        val lines = mutableListOf(
            "【BillPic Android】原型走查摘要",
            "试用会话：${metrics.totalSessions}",
            "成功拿到图片：${metrics.completedSessions}（完成率 " +
                "${(metrics.completionRate * 100).toInt()}%）",
            "平均转换耗时：" + if (metrics.averageDurationMs > 0) {
                String.format(Locale.CHINA, "%.2f 秒", metrics.averageDurationMs / 1000.0)
            } else {
                "—"
            },
            "反馈数：${metrics.feedbackCount}，平均评分：" +
                if (metrics.feedbackCount > 0) {
                    String.format(Locale.CHINA, "%.1f", metrics.averageRating)
                } else {
                    "—"
                } + " / 5",
            "一定会用：${metrics.wouldUseCount} 人",
            "转换失败次数：${metrics.errorCount}",
            "",
            "主要反馈：",
        )
        if (feedback.isEmpty()) {
            lines += "· （暂无）"
        } else {
            feedback.takeLast(6).forEach { entry ->
                lines += "· [${entry.rating}★] " +
                    (entry.text.ifBlank { "（无文字）" }) +
                    (if (entry.intent.isNotBlank()) " / ${entry.intent}" else "")
            }
        }
        return lines.joinToString("\n")
    }

    private fun buildExportJson(): String {
        val metrics = ValidationCalculator.compute(snapshot)
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("product", AppConfig.APP_NAME)
        root.put("platform", "android")

        val config = JSONObject()
        config.put("hypothesis", AppConfig.HYPOTHESIS)
        config.put("successMetric", AppConfig.SUCCESS_METRIC)
        config.put("completeRateTarget", AppConfig.COMPLETE_RATE_TARGET)
        config.put("ratingTarget", AppConfig.RATING_TARGET)
        config.put("sampleTarget", AppConfig.SAMPLE_TARGET)
        config.put("abTest", AppConfig.AB_TEST_NAME)
        root.put("config", config)

        val summary = JSONObject()
        summary.put("sessions", metrics.totalSessions)
        summary.put("completed", metrics.completedSessions)
        summary.put("completionRate", metrics.completionRate)
        summary.put("feedbackCount", metrics.feedbackCount)
        summary.put("avgRating", metrics.averageRating)
        summary.put("wouldUse", metrics.wouldUseCount)
        summary.put("errorEvents", metrics.errorCount)
        root.put("summary", summary)

        val sessions = JSONArray()
        snapshot.sessions.forEach { session ->
            val sessionObject = JSONObject()
            sessionObject.put("id", session.id)
            sessionObject.put("startedAt", session.startedAt)
            sessionObject.put("variant", session.variant)

            val events = JSONArray()
            session.events.forEach { event ->
                val eventObject = JSONObject()
                eventObject.put("t", event.timestamp)
                eventObject.put("event", event.name)
                eventObject.put("screen", event.screen)
                eventObject.put("props", JSONObject(event.props))
                events.put(eventObject)
            }
            sessionObject.put("events", events)

            val feedback = JSONArray()
            session.feedback.forEach { entry ->
                val feedbackObject = JSONObject()
                feedbackObject.put("t", entry.timestamp)
                feedbackObject.put("rating", entry.rating)
                feedbackObject.put("text", entry.text)
                feedbackObject.put("intent", entry.intent)
                feedbackObject.put("converted", entry.converted)
                feedbackObject.put("formatId", entry.formatId)
                feedbackObject.put("pages", entry.pages)
                feedback.put(feedbackObject)
            }
            sessionObject.put("feedback", feedback)
            sessions.put(sessionObject)
        }
        root.put("sessions", sessions)

        return root.toString(2)
    }
}
