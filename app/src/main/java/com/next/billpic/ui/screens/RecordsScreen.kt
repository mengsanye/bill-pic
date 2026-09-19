package com.next.billpic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.next.billpic.R
import com.next.billpic.core.model.ConversionRecord
import com.next.billpic.core.util.Formatters
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.EmptyState
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText
import java.util.Locale

/**
 * 记录。
 *
 * 只留元信息（哪个文件、几页、多大、多快），图片本身在系统相册里 ——
 * 应用内再维护一份图片副本既浪费空间，也会让用户搞不清「图到底存哪了」。
 *
 * 上一版的卡片带箭头却点不出任何东西，是可预期的落空；现在点击有真实去向：
 * 最近一次且结果还在内存里就回结果页，否则带用户去系统相册。长按可删单条。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordsScreen(
    state: MainUiState,
    onRecordTap: (Int) -> Unit,
    onRecordLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val records = state.history

    if (records.isEmpty()) {
        ScreenScroll(modifier = modifier) {
            EmptyState(
                iconRes = R.drawable.ic_folder,
                title = "还没有转换记录",
                description = "转过的发票会出现在这里；图片本身存在系统相册",
            )
        }
        return
    }

    ScreenScroll(modifier = modifier) {
        SectionHeader("最近 ${records.size} 次转换", trailing = "长按可删除单条")
        records.forEachIndexed { index, record ->
            RecordCard(
                record = record,
                onClick = { onRecordTap(index) },
                onLongClick = { onRecordLongPress(index) },
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "这里只保存转换记录，图片在系统相册的 BillPic 相册里。",
            style = AppText.Caption,
            color = AppColor.label3,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(22.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordCard(
    record: ConversionRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    SurfaceCard(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        haptics()
                        onClick()
                    },
                    onLongClick = {
                        haptics()
                        onLongClick()
                    },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(palette.fill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = record.formatId.uppercase(Locale.ROOT),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.label2,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = AppText.BodyStrong,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${Formatters.time(record.timestamp)} · ${record.pages} 张 · " +
                        "${Formatters.bytes(record.totalBytes)} · " +
                        Formatters.oneDecimalSeconds(record.durationMs),
                    style = AppText.Caption,
                    color = palette.label2,
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = palette.label3,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
