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
 * 按「用户会为什么来这里」组织：隐私与数据、帮助与反馈、关于。
 * 每一项都对应一个真实存在的去处，不放点不出结果的入口。
 */
@Composable
fun MineScreen(
    state: MainUiState,
    onPrivacy: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenIssues: () -> Unit,
    onFaq: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor

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
                title = "问题反馈",
                subtitle = "在 GitHub issue 里提出，可被所有人检索",
                onClick = onOpenIssues,
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
                subtitle = "版本 ${AppConfig.VERSION_NAME} · 项目主页 · 开源许可",
                onClick = onAbout,
            )
        }

        /* ---------------- 页脚：版本 + 许可证 + 留存说明 ---------------- */

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${AppConfig.APP_NAME} v${AppConfig.VERSION_NAME}",
                style = AppText.Caption,
                color = palette.label3,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${AppConfig.LICENSE_NAME} License",
                style = AppText.Caption,
                color = palette.label3,
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
