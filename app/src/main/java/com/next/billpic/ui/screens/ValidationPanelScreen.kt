package com.next.billpic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.FunnelStep
import com.next.billpic.core.model.FeedbackEntry
import com.next.billpic.core.model.TrackedEvent
import com.next.billpic.core.model.ValidationCalculator
import com.next.billpic.core.model.ValidationMetrics
import com.next.billpic.core.model.VariantStat
import com.next.billpic.core.util.Formatters
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SecondaryActionButton
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 验证看板。
 *
 * 这是整个原型存在的意义：让团队在走查结束后能直接拿到「继续 / 调整 / 停止」的依据，
 * 而不是靠感觉。判定规则与原型一致，比例全部以「会话」为单位。
 */
@Composable
fun ValidationPanelScreen(
    state: MainUiState,
    onNewSession: () -> Unit,
    onResetData: () -> Unit,
    onExportJson: () -> Unit,
    onCopySummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val metrics = remember(state.telemetry) { ValidationCalculator.compute(state.telemetry) }
    val feedback = state.telemetry.sessions
        .flatMap { it.feedback }
        .sortedBy { it.timestamp }
    val events = state.telemetry.sessions
        .flatMap { session -> session.events.map { session.id.takeLast(4) to it } }
        .sortedBy { it.second.timestamp }

    ScreenScroll(modifier = modifier) {
        /* ---------------- 假设与及格线 ---------------- */
        SectionHeader("核心假设")
        SurfaceCard(contentPadding = PaddingValues(14.dp)) {
            Text(text = AppConfig.HYPOTHESIS, style = AppText.Sub, color = palette.label)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "及格线：${AppConfig.SUCCESS_METRIC}",
                style = AppText.Caption,
                color = palette.label2,
            )
        }

        /* ---------------- 指标 ---------------- */
        SectionHeader("验证指标")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCell(
                value = metrics.totalSessions.toString(),
                label = "试用人数（会话）",
                accent = null,
                modifier = Modifier.weight(1f),
            )
            MetricCell(
                value = metrics.completedSessions.toString(),
                label = "成功拿到图片",
                accent = null,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCell(
                value = "${(metrics.completionRate * 100).toInt()}%",
                label = "完成率",
                accent = if (metrics.completionRate >= AppConfig.COMPLETE_RATE_TARGET) {
                    palette.green
                } else {
                    palette.red
                },
                modifier = Modifier.weight(1f),
            )
            MetricCell(
                value = if (metrics.feedbackCount > 0) {
                    String.format(java.util.Locale.CHINA, "%.1f", metrics.averageRating)
                } else {
                    "—"
                },
                label = "平均评分 / 5",
                accent = if (metrics.feedbackCount > 0 &&
                    metrics.averageRating >= AppConfig.RATING_TARGET
                ) {
                    palette.green
                } else {
                    palette.red
                },
                modifier = Modifier.weight(1f),
            )
        }

        /* ---------------- 漏斗 ---------------- */
        SectionHeader("转化漏斗")
        SurfaceCard(contentPadding = PaddingValues(14.dp)) {
            metrics.funnel.forEach { step ->
                FunnelBar(step = step, total = metrics.totalSessions)
            }
        }
        Spacer(Modifier.height(10.dp))
        SurfaceCard(contentPadding = PaddingValues(14.dp)) {
            Text(
                text = "样本 ${metrics.totalSessions} / 目标 ${AppConfig.SAMPLE_TARGET}" +
                    "　卡点事件（转换失败 / 选错文件 / 解析失败）：${metrics.errorCount} 次",
                style = AppText.Caption,
                color = palette.label2,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "结论：${metrics.conclusion}",
                style = AppText.BodyStrong,
                color = if (metrics.passed) palette.green else palette.orange,
            )
            if (metrics.averageDurationMs > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "平均转换耗时：${Formatters.seconds(metrics.averageDurationMs)}",
                    style = AppText.Caption,
                    color = palette.label2,
                )
            }
        }

        /* ---------------- A/B ---------------- */
        SectionHeader("A/B 测试：主按钮文案")
        SurfaceCard(contentPadding = PaddingValues(14.dp)) {
            if (metrics.totalSessions == 0) {
                Text(text = "暂无数据", style = AppText.Caption, color = palette.label3)
            } else {
                metrics.variants.forEachIndexed { index, stat ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    VariantRow(stat)
                }
            }
        }

        /* ---------------- 反馈 ---------------- */
        SectionHeader("用户反馈", if (feedback.isEmpty()) null else "${feedback.size} 条")
        SurfaceCard(contentPadding = PaddingValues(14.dp)) {
            if (feedback.isEmpty()) {
                Text(text = "还没有反馈", style = AppText.Caption, color = palette.label3)
            } else {
                feedback.reversed().forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    FeedbackItem(entry)
                }
            }
        }

        /* ---------------- 事件流 ---------------- */
        SectionHeader("事件流（实时）", "最近 40 条")
        SurfaceCard(contentPadding = PaddingValues(12.dp)) {
            if (events.isEmpty()) {
                Text(text = "等待用户操作……", style = AppText.Caption, color = palette.label3)
            } else {
                events.takeLast(40).reversed().forEach { (sessionSuffix, event) ->
                    EventLine(sessionSuffix = sessionSuffix, event = event)
                }
            }
        }

        /* ---------------- 操作 ---------------- */
        SectionHeader("走查操作")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(
                text = "导出 JSON",
                onClick = onExportJson,
                modifier = Modifier.weight(1f),
            )
            SecondaryActionButton(
                text = "复制摘要",
                onClick = onCopySummary,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(
                text = "开始新会话",
                onClick = onNewSession,
                modifier = Modifier.weight(1f),
            )
            SecondaryActionButton(
                text = "重置数据",
                onClick = onResetData,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "走查建议：让同事装同一份 APK，用自己的真实发票试，5 人即够；" +
                "「开始新会话」换人，数据自动隔离。",
            style = AppText.Caption,
            color = palette.label3,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun MetricCell(
    value: String,
    label: String,
    accent: Color?,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.card)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(text = value, style = AppText.Metric, color = accent ?: palette.label)
        Spacer(Modifier.height(3.dp))
        Text(text = label, style = AppText.Caption, color = palette.label2)
    }
}

@Composable
private fun FunnelBar(step: FunnelStep, total: Int) {
    val palette = AppColor
    val fraction = if (total <= 0) 0f else step.sessionCount.toFloat() / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = step.label,
                style = AppText.Note,
                color = palette.label,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${step.sessionCount} · ${(fraction * 100).toInt()}%",
                style = AppText.Caption,
                color = palette.label2,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.fill),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.blue),
                )
            }
        }
    }
}

@Composable
private fun VariantRow(stat: VariantStat) {
    val palette = AppColor
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stat.variant, style = AppText.BodyStrong, color = palette.label)
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (stat.sessions == 0) {
                "暂无试用者"
            } else {
                "${stat.sessions} 人 · 完成 ${(stat.completionRate * 100).toInt()}%"
            },
            style = AppText.Caption,
            color = palette.label2,
        )
    }
}

@Composable
private fun FeedbackItem(entry: FeedbackEntry) {
    val palette = AppColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.fill)
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Formatters.stars(entry.rating),
                style = AppText.Note,
                color = palette.orange,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formatters.time(entry.timestamp) +
                    (if (entry.converted) " · 已拿到图片" else " · 未完成转换") +
                    (if (entry.intent.isNotBlank()) " · ${entry.intent}" else ""),
                style = AppText.Caption,
                color = palette.label2,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.text.ifBlank { "（未填写文字）" },
            style = AppText.Sub,
            color = if (entry.text.isBlank()) palette.label3 else palette.label,
        )
    }
}

@Composable
private fun EventLine(sessionSuffix: String, event: TrackedEvent) {
    val palette = AppColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Formatters.clock(event.timestamp),
                style = AppText.Mono,
                fontFamily = FontFamily.Monospace,
                color = palette.label3,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = event.name,
                style = AppText.Mono,
                fontFamily = FontFamily.Monospace,
                color = palette.blue,
            )
        }
        val extra = event.extra
        if (extra.isNotBlank()) {
            Text(
                text = "[$sessionSuffix] $extra",
                style = AppText.Mono,
                fontFamily = FontFamily.Monospace,
                color = palette.label3,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
