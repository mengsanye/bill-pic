package com.next.billpic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * 这一屏同时承担三个上架必需职能：
 * 1. 版本号与技术来源（用户与客服判断问题都对得上）
 * 2. 开发者主体 + 联系方式（隐私政策必备要素，商店会交叉核对）
 * 3. 备案编号展示位（工信部要求显著位置标明，并在编号下方链接备案系统）
 * 此外提供用户主动发起的诊断导出，替代不能联网的崩溃上报。
 */
@Composable
fun AboutScreen(
    onFeedbackEmail: () -> Unit,
    onBeian: () -> Unit,
    onExportDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val warn = palette.orange

    val unresolved = buildList {
        if (AppConfig.isPlaceholder(AppConfig.DEVELOPER_NAME)) add("开发者主体")
        if (AppConfig.isPlaceholder(AppConfig.CONTACT_EMAIL)) add("反馈邮箱")
        if (AppConfig.isPlaceholder(AppConfig.ICP_BEIAN_NUMBER)) add("App 备案编号")
    }

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

        if (unresolved.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SurfaceCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "上架前需补齐",
                        style = AppText.BodyStrong,
                        color = warn,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = unresolved.joinToString("、") +
                            " 尚未填写，相关位置已用警示色标出。" +
                            "这些是应用商店审核的硬性要求，补齐后即可提交上架。",
                        style = AppText.Sub,
                        color = palette.label2,
                    )
                }
            }
        }

        SectionHeader("开发者")
        SurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            MetaRow("开发者主体", AppConfig.DEVELOPER_NAME)
            MetaRow("联系邮箱", AppConfig.CONTACT_EMAIL)
            MetaRow("应用名称", AppConfig.APP_NAME)
            Spacer(Modifier.height(6.dp))
            LinkTextButton(text = "发送反馈邮件", onClick = onFeedbackEmail)
            Spacer(Modifier.height(4.dp))
        }

        SectionHeader("备案信息")
        SurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            MetaRow("备案编号", AppConfig.ICP_BEIAN_NUMBER)
            Spacer(Modifier.height(4.dp))
            // 规范要求：备案编号下方提供备案系统链接，供公众查询核对
            LinkTextButton(text = "备案系统查询 ↗", onClick = onBeian)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "备案系统网址：${AppConfig.ICP_BEIAN_URL}",
                style = AppText.Caption,
                color = palette.label3,
            )
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
                title = "使用的开源组件",
                body = "AndroidX 与 Jetpack Compose、Kotlin 标准库与 kotlinx.coroutines，" +
                    "均以 Apache License 2.0 授权。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "完整许可文本",
                body = "Apache License 2.0 全文见 https://www.apache.org/licenses/LICENSE-2.0",
            )
        }

        SectionHeader("排查问题")
        SurfaceCard {
            ListRowButton(onClick = onExportDiagnostics)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "© 2026 ${AppConfig.DEVELOPER_NAME} · 保留所有权利",
            style = AppText.Caption,
            color = if (AppConfig.isPlaceholder(AppConfig.DEVELOPER_NAME)) warn else palette.label3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ListRowButton(onClick: () -> Unit) {
    val palette = AppColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = "导出诊断信息", style = AppText.BodyStrong, color = palette.label)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "生成一份含设备环境与本地转换记录的文件，用于反馈问题时附上。" +
                "内容仅来自本机，由你主动发起。",
            style = AppText.Sub,
            color = palette.label2,
        )
        Spacer(Modifier.height(8.dp))
        LinkTextButton(text = "生成并分享", onClick = onClick)
    }
}
