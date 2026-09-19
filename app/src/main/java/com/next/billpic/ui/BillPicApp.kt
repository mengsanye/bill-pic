package com.next.billpic.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.next.billpic.ui.components.BackTitleBar
import com.next.billpic.ui.components.HudOverlay
import com.next.billpic.ui.components.LargeTitleBar
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.screens.ConvertScreen
import com.next.billpic.ui.screens.MineScreen
import com.next.billpic.ui.screens.PrivacyScreen
import com.next.billpic.ui.screens.RecordsScreen
import com.next.billpic.ui.screens.ResultScreen
import com.next.billpic.ui.screens.ValidationPanelScreen
import com.next.billpic.ui.sheets.FeedbackSheetOverlay
import com.next.billpic.ui.sheets.ImageViewerOverlay
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText
import kotlinx.coroutines.delay

/**
 * 根容器。
 *
 * 弹层（HUD / 反馈 / 全屏看图）刻意放在 Scaffold 之外，
 * 这样它们能盖住底部 Tab 栏，符合手机上「全屏弹层」的预期。
 */
@Composable
fun BillPicApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = AppColor

    /* ---------------- 选 PDF（系统文件选择器，不申请任何读取权限） ---------------- */

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        viewModel.onFilePicked(uri)
    }

    /* ---------------- 写入相册权限（仅 Android 9 及以下需要） ---------------- */

    var pendingStorageAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted: Boolean ->
        val action = pendingStorageAction
        pendingStorageAction = null
        if (granted) action?.invoke() else viewModel.storagePermissionDenied()
    }

    val runWithStoragePermission: (() -> Unit) -> Unit = { action ->
        val alreadyAllowed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        if (alreadyAllowed) {
            action()
        } else {
            pendingStorageAction = action
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /* ---------------- 返回键 ---------------- */

    val canGoBack = state.viewerPage != null ||
        state.feedbackOpen ||
        state.mineSub != MineSub.NONE ||
        state.showResult ||
        state.tab != AppTab.CONVERT
    BackHandler(enabled = canGoBack) { viewModel.handleBack() }

    /* ---------------- HUD 自动消失 ---------------- */

    val hud = state.hud
    LaunchedEffect(hud) {
        if (hud != null) {
            delay(2200)
            viewModel.consumeHud()
        }
    }

    /* ---------------- 系统分享 / 导出 ---------------- */

    val shareRequest = state.shareRequest
    LaunchedEffect(shareRequest) {
        if (shareRequest != null) {
            runCatching { context.startActivity(shareRequest) }
            viewModel.consumeShareRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.bg,
            bottomBar = {
                AppTabBar(selected = state.tab, onSelect = viewModel::selectTab)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                TopNavBar(state = state, onBack = { viewModel.handleBack() })

                when {
                    state.mineSub == MineSub.PRIVACY -> PrivacyScreen(
                        onGoHome = {
                            viewModel.closeMineSub()
                            viewModel.selectTab(AppTab.CONVERT)
                        },
                    )

                    state.mineSub == MineSub.VALIDATION -> ValidationPanelScreen(
                        state = state,
                        onNewSession = viewModel::startNewTrialSession,
                        onResetData = viewModel::resetAllData,
                        onExportJson = viewModel::exportJson,
                        onCopySummary = viewModel::copySummary,
                    )

                    state.showResult -> ResultScreen(
                        state = state,
                        onOpenViewer = viewModel::openViewer,
                        onSaveOne = { page -> runWithStoragePermission { viewModel.saveOne(page) } },
                        onSaveAll = { runWithStoragePermission { viewModel.saveAll() } },
                        onShare = viewModel::shareAll,
                    )

                    state.tab == AppTab.RECORDS -> RecordsScreen(
                        state = state,
                        onRecordTap = viewModel::recordTap,
                        onOpenLatest = viewModel::goToResult,
                    )

                    state.tab == AppTab.MINE -> MineScreen(
                        state = state,
                        onPrivacy = viewModel::openPrivacy,
                        onFeedback = viewModel::openFeedback,
                        onAbout = viewModel::showAbout,
                        onValidationPanel = viewModel::openValidationPanel,
                    )

                    else -> ConvertScreen(
                        state = state,
                        onPick = {
                            viewModel.onPickTapped()
                            pickPdfLauncher.launch(arrayOf("application/pdf"))
                        },
                        onClear = viewModel::clearFile,
                        onFormat = viewModel::setFormat,
                        onScale = viewModel::setScale,
                        onGoResult = viewModel::goToResult,
                    )
                }
            }
        }

        HudOverlay(text = state.hud, modifier = Modifier.fillMaxSize())

        FeedbackSheetOverlay(
            visible = state.feedbackOpen,
            onDismiss = viewModel::closeFeedback,
            onSubmit = viewModel::submitFeedback,
            onRatingSelected = viewModel::onRatingSelected,
            onIntentSelected = viewModel::onIntentSelected,
        )

        ImageViewerOverlay(
            visible = state.viewerPage != null,
            page = state.viewerPage?.let { page ->
                state.results.firstOrNull { it.page == page }
            },
            onClose = viewModel::closeViewer,
            onSave = { state.viewerPage?.let { page -> runWithStoragePermission { viewModel.saveOne(page) } } },
        )
    }
}

@Composable
private fun TopNavBar(state: MainUiState, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            state.mineSub == MineSub.PRIVACY -> BackTitleBar(
                backLabel = "我的",
                title = "隐私说明",
                onBack = onBack,
            )

            state.mineSub == MineSub.VALIDATION -> BackTitleBar(
                backLabel = "我的",
                title = "验证看板",
                onBack = onBack,
            )

            state.showResult -> BackTitleBar(
                backLabel = "转换",
                title = "转换完成",
                onBack = onBack,
            )

            state.tab == AppTab.RECORDS -> LargeTitleBar(title = "记录")

            state.tab == AppTab.MINE -> LargeTitleBar(title = "我的")

            else -> LargeTitleBar(title = "发票变图片")
        }
    }
}

/**
 * 底部 Tab。
 * 自绘而非用 M3 NavigationBar：Material 的选中态用的是 secondaryContainer，
 * 与产品既定的 iOS 风格蓝色不一致；自绘可以精确控制，且不依赖主题里的额外颜色槽。
 */
@Composable
private fun AppTabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val palette = AppColor
    val haptics = rememberTapHaptics()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.separator),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                val isOn = tab == selected
                val tint = if (isOn) palette.blue else palette.label3
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (!isOn) {
                                haptics()
                                onSelect(tab)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(text = tab.label, style = AppText.Caption, color = tint)
                }
            }
        }
    }
}
