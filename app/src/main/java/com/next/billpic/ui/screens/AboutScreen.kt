package com.next.billpic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.next.billpic.R
import com.next.billpic.core.model.AppConfig
import com.next.billpic.ui.components.LinkTextButton
import com.next.billpic.ui.components.MetaRow
import com.next.billpic.ui.components.PlainInfoRow
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 关于。
 *
 * 面向开源项目的使用者，只放三类信息：
 * 版本号（对 issue 时用得上）、项目主页与问题反馈入口、许可证与所依赖的开源组件。
 */
@Composable
fun AboutScreen(
    onOpenRepo: () -> Unit,
    onOpenIssues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor

    ScreenScroll(modifier = modifier) {
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.blue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_doc),
                    contentDescription = null,
                    tint = palette.blue,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(text = AppConfig.APP_NAME, style = AppText.HeroTitle, color = palette.label)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "版本 ${AppConfig.VERSION_NAME}",
                style = AppText.Sub,
                color = palette.label2,
                textAlign = TextAlign.Center,
            )
        }

        SectionHeader("项目")
        SurfaceCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            MetaRow("应用名称", AppConfig.APP_NAME)
            MetaRow("项目主页", AppConfig.PROJECT_URL)
            Spacer(Modifier.height(8.dp))
            LinkTextButton(text = "在浏览器中打开 ↗", onClick = onOpenRepo)
            Spacer(Modifier.height(12.dp))
            LinkTextButton(text = "问题反馈 / 功能建议 ↗", onClick = onOpenIssues)
            Spacer(Modifier.height(8.dp))
        }

        SectionHeader("技术说明")
        SurfaceCard {
            PlainInfoRow(
                title = "转换引擎",
                body = "使用 Android 系统自带的 PDF 渲染能力在本机完成转换，" +
                    "没有引入任何第三方 PDF 库。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "权限",
                body = "仅 Android 9 及以下需要「存储」权限用于写入相册。未申请网络权限，" +
                    "因此应用不具备把数据传出的技术途径。",
            )
        }

        SectionHeader("开源许可")
        SurfaceCard {
            PlainInfoRow(
                title = "本项目",
                body = "${AppConfig.LICENSE_NAME} License，全文见仓库根目录 LICENSE。" +
                    "可自由使用、修改与再分发，请保留版权声明。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "依赖的组件",
                body = "AndroidX 与 Jetpack Compose、Kotlin 标准库与 kotlinx.coroutines，" +
                    "均以 Apache License 2.0 授权。",
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "© 2026 BillPic contributors · ${AppConfig.LICENSE_NAME} License",
            style = AppText.Caption,
            color = palette.label3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(30.dp))
    }
}
