package com.next.billpic.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.next.billpic.R
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.UserSnapshot
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.LinkTextButton
import com.next.billpic.ui.components.ListActionRow
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 我的。
 *
 * 信息架构按「用户会为什么来这里」组织：隐私与数据、帮助与反馈、关于。
 * 走查工具收在最后一节，且只在上架前的走查包里出现——上架包中这段实现不参与编译。
 */
@Composable
fun MineScreen(
    state: MainUiState,
    onPrivacy: () -> Unit,
    onPolicy: () -> Unit,
    onTerms: () -> Unit,
    onClearHistory: () -> Unit,
    onFeedbackEmail: () -> Unit,
    onFaq: () -> Unit,
    onAbout: () -> Unit,
    onValidationPanel: () -> Unit,
    onQuickRating: () -> Unit,
    onBeian: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val warn = palette.orange
    val needsContact = AppConfig.isPlaceholder(AppConfig.CONTACT_EMAIL)

    ScreenScroll(modifier = modifier) {
        SectionHeader("隐私与数据")
        SurfaceCard {
            ListActionRow(
                iconRes = R.drawable.ic_lock,
                title = "发票会上传吗？",
                subtitle = "全程本机处理，不申请联网权限",
                onClick = onPrivacy,
                accent = palette.green,
            )
            RowDivider()
            ListActionRow(
                iconRes = R.drawable.ic_shield,
                title = "隐私政策",
                subtitle = "数据如何被处理、保留多久",
                onClick = onPolicy,
            )
            RowDivider()
            ListActionRow(
                iconRes = R.drawable.ic_doc,
                title = "用户协议",
                subtitle = "使用条款与责任说明",
                onClick = onTerms,
            )
            RowDivider()
            ListActionRow(
                iconRes = R.drawable.ic_trash,
                title = "清除转换记录",
                subtitle = if (state.history.isEmpty()) {
                    "本机暂无记录"
                } else {
                    "本机已保存 ${state.history.size} 条记录"
                },
                onClick = onClearHistory,
                accent = palette.red,
            )
        }

        SectionHeader("帮助与反馈")
        SurfaceCard {
            ListActionRow(
                iconRes = R.drawable.ic_mail,
                title = "意见反馈",
                subtitle = AppConfig.CONTACT_EMAIL,
                onClick = onFeedbackEmail,
                accent = if (needsContact) warn else palette.blue,
            )
            RowDivider()
            ListActionRow(
                iconRes = R.drawable.ic_help,
                title = "常见问题",
                subtitle = "中文变问号、保存位置、页码选择",
                onClick = onFaq,
            )
        }

        SectionHeader("关于")
        SurfaceCard {
            ListActionRow(
                iconRes = R.drawable.ic_info,
                title = "关于 BillPic",
                subtitle = "版本 ${AppConfig.VERSION_NAME} · 开发者信息 · 开源许可",
                onClick = onAbout,
            )
        }

        if (state.panelEnabled) {
            SectionHeader("走查工具", trailing = "仅走查包可见")
            SurfaceCard {
                ListActionRow(
                    iconRes = R.drawable.ic_chart,
                    title = "验证看板",
                    subtitle = "完成率 / 漏斗 / A-B / 反馈汇总",
                    onClick = onValidationPanel,
                )
                RowDivider()
                ListActionRow(
                    iconRes = R.drawable.ic_star,
                    title = "给它打个分",
                    subtitle = "10 秒，结论留在本机供走查汇总",
                    onClick = onQuickRating,
                    accent = palette.orange,
                )
            }
        }

        /* ---------------- 页脚：版本 + 备案编号 + 主体 ---------------- */

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "BillPic v${AppConfig.VERSION_NAME}",
                style = AppText.Caption,
                color = palette.label3,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            // 工信部要求：App 显著位置标明备案编号，并在编号下方按要求链接备案系统网址
            Text(
                text = "备案编号：${AppConfig.ICP_BEIAN_NUMBER}",
                style = AppText.Caption,
                color = if (AppConfig.isPlaceholder(AppConfig.ICP_BEIAN_NUMBER)) warn else palette.label3,
                textAlign = TextAlign.Center,
            )
            LinkTextButton(text = "备案系统查询", onClick = onBeian)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "© 2026 ${AppConfig.DEVELOPER_NAME}",
                style = AppText.Caption,
                color = if (AppConfig.isPlaceholder(AppConfig.DEVELOPER_NAME)) warn else palette.label3,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "转换记录最多保留 ${UserSnapshot.MAX_HISTORY} 条，可随时清除",
                style = AppText.Caption,
                color = palette.label3,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}
