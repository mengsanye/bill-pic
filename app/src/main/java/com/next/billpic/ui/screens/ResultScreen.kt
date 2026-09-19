package com.next.billpic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.next.billpic.R
import com.next.billpic.core.model.ConvertedPage
import com.next.billpic.core.model.OutputFormat
import com.next.billpic.core.util.Formatters
import com.next.billpic.core.util.Images
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.MetaRow
import com.next.billpic.ui.components.PrimaryActionButton
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SecondaryActionButton
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 转换结果。
 *
 * 唯一一屏带底部操作栏：主操作固定「保存全部到相册」，分享作为次操作。
 * 缩略图统一按 280dp 高度等比缩放（不裁剪），保证长发票也能整页看全。
 */
@Composable
fun ResultScreen(
    state: MainUiState,
    onOpenViewer: (Int) -> Unit,
    onSaveOne: (Int) -> Unit,
    onSaveAll: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val first = state.results.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        ScreenScroll(modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(palette.green),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${state.results.size} 张图片已生成",
                    style = AppText.HeroTitle,
                    color = palette.label,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.source?.displayName ?: "—",
                    style = AppText.Sub,
                    color = palette.label2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))
            SurfaceCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                MetaRow("发票文件", state.source?.displayName ?: "—")
                MetaRow(
                    "输出尺寸",
                    if (first != null) "${first.width} × ${first.height} px" else "—",
                )
                MetaRow(
                    "生成图片",
                    "${state.results.size} 张 · ${Formatters.bytes(state.lastTotalBytes)} · ${state.format.label}",
                )
                MetaRow("转换耗时", Formatters.seconds(state.lastDurationMs))
            }

            SectionHeader(title = "图片预览", trailing = "点开可放大")
            state.results.forEach { page ->
                PageCard(
                    page = page,
                    onOpen = { onOpenViewer(page.page) },
                    onSave = { onSaveOne(page.page) },
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryActionButton(
                text = if (state.savingAll) "正在保存…" else "保存全部到相册",
                onClick = onSaveAll,
                enabled = !state.savingAll,
                modifier = Modifier.weight(1.5f),
            )
            SecondaryActionButton(
                text = "分享",
                onClick = onShare,
                leadingIconRes = R.drawable.ic_share,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PageCard(
    page: ConvertedPage,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    // 采样解码：缩略图不需要全尺寸，30 页也不至于把内存顶爆
    val image = remember(page) { Images.decodeSampled(page.bytes, maxDimension = 720) }

    SurfaceCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(palette.fill)
                .clickable {
                    haptics()
                    onOpen()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "第 ${page.page} 页发票图片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            } else {
                Text(
                    text = "这张图解码失败",
                    style = AppText.Note,
                    color = palette.label2,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "第 ${page.page} 页 · ${Formatters.bytes(page.bytes.size.toLong())} · " +
                    "${page.width}×${page.height}",
                style = AppText.Caption,
                color = palette.label2,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.blue.copy(alpha = 0.12f))
                    .clickable {
                        haptics()
                        onSave()
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    text = "保存",
                    style = AppText.Note,
                    color = palette.blue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
