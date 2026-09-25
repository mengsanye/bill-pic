package com.next.billpic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.next.billpic.R
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.model.OutputScale
import com.next.billpic.core.model.PageRange
import com.next.billpic.core.model.PdfSource
import com.next.billpic.core.util.Formatters
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.LinkTextButton
import com.next.billpic.ui.components.PdfBadge
import com.next.billpic.ui.components.PrimaryActionButton
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SecondaryActionButton
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SegmentedControl
import com.next.billpic.ui.components.SettingRow
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.components.TextInputRow
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 转换（首页）。
 *
 * 主按钮同时承担「选文件」与「重新选择」，减少一次点击；
 * 输出设置里除了格式与档位，新增**页码范围**——报销场景经常是
 * 「这份 PDF 里只有第 3 页是我的」，不给选择权会逼用户去别的工具。
 */
@Composable
fun ConvertScreen(
    state: MainUiState,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onFormat: (OutputFormat) -> Unit,
    onScale: (OutputScale) -> Unit,
    onPageRange: (String) -> Unit,
    onConvert: () -> Unit,
    onGoResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()

    ScreenScroll(modifier = modifier) {
        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.blue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_doc),
                    contentDescription = null,
                    tint = palette.blue,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "把发票变成图片",
                style = AppText.HeroTitle,
                color = palette.label,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "PDF 发票一键转成 JPG / PNG，全程在这台手机上处理，不上传服务器。",
                style = AppText.Sub,
                color = palette.label2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        PrimaryActionButton(
            text = AppConfig.PRIMARY_CTA,
            onClick = onPick,
            enabled = !state.converting && !state.parsing,
            leadingIconRes = R.drawable.ic_plus,
        )

        val source = state.source
        if (source != null) {
            Spacer(Modifier.height(12.dp))
            SurfaceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PdfBadge()
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.displayName,
                            style = AppText.BodyStrong,
                            color = palette.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = sourceMeta(source, state.parsing),
                            style = AppText.Caption,
                            color = palette.label2,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.fill)
                            .clickable {
                                haptics()
                                onClear()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "移除已选文件",
                            tint = palette.label2,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        SectionHeader("输出设置")
        SurfaceCard {
            SettingRow("格式") {
                SegmentedControl(
                    labels = OutputFormat.entries.map { it.label },
                    selectedIndex = OutputFormat.entries.indexOf(state.format),
                    onSelect = { index -> onFormat(OutputFormat.entries[index]) },
                    modifier = Modifier.width(150.dp),
                )
            }
            RowDivider(startPadding = 16)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "输出档位",
                        style = AppText.Body,
                        color = palette.label,
                        modifier = Modifier.weight(1f),
                    )
                    SegmentedControl(
                        labels = OutputScale.entries.map { it.label },
                        selectedIndex = OutputScale.entries.indexOf(state.scale),
                        onSelect = { index -> onScale(OutputScale.entries[index]) },
                        modifier = Modifier.width(204.dp),
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(text = state.scale.hint, style = AppText.Caption, color = palette.label3)
            }

            if (source != null) {
                RowDivider(startPadding = 16)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "页码范围",
                        style = AppText.Body,
                        color = palette.label,
                    )
                    Spacer(Modifier.height(9.dp))
                    TextInputRow(
                        value = state.pageRangeInput,
                        placeholder = "留空 = 全部（如 1-3,5）",
                        onValueChange = onPageRange,
                        enabled = !state.converting && !state.parsing,
                        error = state.pageRangeError,
                        helper = pageRangeHelper(state),
                    )
                }
            }
        }

        // 多页 PDF 停在这里等用户确认页码（单页已自动转换）。
        // 主操作刻意放在设置区**下方**：顺序是「先配置、再执行」，
        // 按钮在上方会诱导用户在没看到页码输入框之前就按下转换。
        if (state.needsExplicitConvert) {
            Spacer(Modifier.height(14.dp))
            val pages = state.selectedPages
            PrimaryActionButton(
                text = when {
                    pages.isEmpty() -> "开始转换"
                    state.pageRangeInput.isBlank() -> "开始转换（全部 ${pages.size} 页）"
                    else -> "开始转换（${pages.size} 页）"
                },
                onClick = onConvert,
                enabled = pages.isNotEmpty(),
            )
        }

        if (state.settingsDirty) {
            Spacer(Modifier.height(14.dp))
            SecondaryActionButton(
                text = "用当前设置重新转换",
                onClick = onConvert,
            )
        }

        if (state.results.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.green.copy(alpha = 0.10f))
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "上次转换：${state.results.size} 张图片",
                    style = AppText.Sub,
                    color = palette.label,
                )
                LinkTextButton(text = "查看结果 ›", onClick = onGoResult)
            }
        }

        if (state.parsing) {
            Spacer(Modifier.height(14.dp))
            Text(text = "正在读取发票…", style = AppText.Caption, color = palette.label3)
        }

        Spacer(Modifier.height(28.dp))
    }
}

private fun sourceMeta(source: PdfSource, parsing: Boolean): String {
    val size = Formatters.bytes(source.sizeBytes)
    if (parsing) return "$size · 读取中…"
    val warning = if (source.pageCount > AppConfig.MAX_PAGES) {
        "　共 ${source.pageCount} 页，可在下方选页码"
    } else {
        ""
    }
    return "$size · 共 ${source.pageCount} 页$warning"
}

/** 页码输入框下方的一行说明：合法时告知会转几页，非法时由 error 接管。 */
private fun pageRangeHelper(state: MainUiState): String? {
    val source = state.source ?: return null
    if (state.pageRangeError != null) return null
    if (state.pageRangeInput.isBlank()) {
        return "留空 = 转换全部 ${minOf(source.pageCount, AppConfig.MAX_PAGES)} 页"
    }
    val base = "将转换 ${state.selectedPages.size} 页：" +
        PageRange.describe(state.selectedPages)
    return if (state.pageRangeTruncated) {
        "$base（超出单次上限，只取前 ${AppConfig.MAX_PAGES} 页）"
    } else {
        base
    }
}
