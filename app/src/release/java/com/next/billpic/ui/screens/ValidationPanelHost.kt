package com.next.billpic.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.next.billpic.ui.MainUiState

/**
 * 上架包的验证看板占位。
 *
 * 与 `src/debug` 里的同名函数构成一对：main 源集只调用这个名字，
 * 具体是谁由构建变体决定。上架包里整屏实现（含指标计算、事件流、反馈汇总）
 * 都不参与编译，因此不存在「忘了关开关」导致内部工具外泄的可能。
 *
 * 正常情况下这个函数不会被调用——入口已被 `AppConfig.VALIDATION_PANEL_ENABLED=false` 屏蔽。
 * 保留它是为了让 main 源集的 `when` 分支在两个变体下保持同一份代码。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ValidationPanelHost(
    state: MainUiState,
    onNewSession: () -> Unit,
    onResetData: () -> Unit,
    onExportJson: () -> Unit,
    onCopySummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    /* 上架包无验证看板 */
}
