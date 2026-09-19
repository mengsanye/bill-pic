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
import androidx.compose.ui.unit.sp
import com.next.billpic.R
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.model.OutputScale
import com.next.billpic.core.model.PdfSource
import com.next.billpic.core.util.Formatters
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.LinkTextButton
import com.next.billpic.ui.components.PdfBadge
import com.next.billpic.ui.components.PrimaryActionButton
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SegmentedControl
import com.next.billpic.ui.components.SettingRow
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 转换（首页）。
 *
 * 与原型一致：价值主张 → 主按钮（A/B 文案）→ 已选文件 → 输出设置 → 「已生成 N 张」入口。
 * 主按钮同时承担「选文件」与「重新选择」，减少一次点击。
 */
@Composable
fun ConvertScreen(
    state: MainUiState,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onFormat: (OutputFormat) -> Unit,
    onScale: (OutputScale) -> Unit,
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
                Text(text = "📄", fontSize = 30.sp)
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
            text = state.variant,
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
                        text = "清晰度",
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
                    text = "已生成 ${state.results.size} 张图片",
                    style = AppText.Sub,
                    color = palette.label,
                )
                LinkTextButton(text = "去看看 ›", onClick = onGoResult)
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
        "　仅转前 ${AppConfig.MAX_PAGES} 页"
    } else {
        ""
    }
    return "$size · 共 ${source.pageCount} 页$warning"
}
