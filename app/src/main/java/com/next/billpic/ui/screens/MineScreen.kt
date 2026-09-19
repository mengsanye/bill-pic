package com.next.billpic.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.next.billpic.ui.MainUiState
import com.next.billpic.ui.components.ListActionRow
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/** 我的。走查工具只在开启验证面板时出现，避免污染真实使用观感。 */
@Composable
fun MineScreen(
    state: MainUiState,
    onPrivacy: () -> Unit,
    onFeedback: () -> Unit,
    onAbout: () -> Unit,
    onValidationPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor

    ScreenScroll(modifier = modifier) {
        SectionHeader("隐私与安全")
        SurfaceCard {
            ListActionRow(
                emoji = "🔒",
                title = "发票会上传吗？",
                subtitle = "全程本机处理，不上传服务器",
                onClick = onPrivacy,
            )
        }

        SectionHeader("关于这个原型")
        SurfaceCard {
            ListActionRow(
                emoji = "💬",
                title = "给它打个分",
                subtitle = "10 秒，数据直接进入验证看板",
                onClick = onFeedback,
            )
            RowDivider()
            ListActionRow(
                emoji = "⚡",
                title = "这是个验证原型",
                subtitle = "不是正式产品，请按真实习惯使用",
                onClick = onAbout,
            )
        }

        if (state.panelEnabled) {
            SectionHeader("走查工具")
            SurfaceCard {
                ListActionRow(
                    emoji = "📊",
                    title = "验证看板",
                    subtitle = "完成率 / 漏斗 / A-B / 反馈汇总",
                    onClick = onValidationPanel,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "BillPic Android v1 · 仅用于内部走查",
            style = AppText.Caption,
            color = palette.label3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}
