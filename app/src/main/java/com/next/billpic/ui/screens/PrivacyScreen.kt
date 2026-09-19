package com.next.billpic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.next.billpic.core.model.UserSnapshot
import com.next.billpic.ui.components.PlainInfoRow
import com.next.billpic.ui.components.PrimaryActionButton
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SecondaryActionButton
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 隐私说明（信任页）。
 *
 * 与「隐私政策」分工不同：这一屏**对用户讲人话**，用可验证的方式回答
 * 「我的发票会不会被传走」；政策页是**合规文本**，讲清数据处理的法律口径。
 * 两者都保留，不互相替代。
 */
@Composable
fun PrivacyScreen(
    onGoHome: () -> Unit,
    onOpenPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor

    ScreenScroll(modifier = modifier) {
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.green.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null,
                    tint = palette.green,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "发票不会离开你的手机",
                style = AppText.HeroTitle,
                color = palette.label,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "发票上有个人信息，这个问题值得认真回答。",
                style = AppText.Sub,
                color = palette.label2,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(22.dp))
        SurfaceCard {
            PlainInfoRow(
                title = "文件去向",
                body = "不上传服务器。转换由手机系统自带的 PDF 渲染引擎完成，断网也能用。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "怎么验证",
                body = "这个 App 根本没有申请联网权限 —— 开飞行模式再转一次，功能照常，" +
                    "这就是最直接的证明。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "本机留了什么",
                body = "不保存发票内容。应用内只留最近 ${UserSnapshot.MAX_HISTORY} 条转换记录，" +
                    "包含发票文件名、页数、格式、体积与耗时——文件名里可能带公司名称或日期，" +
                    "所以这些记录你可以随时清除。图片本身写在系统相册里，由相册管理。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "权限",
                body = "读取发票：由你通过系统文件选择器主动选取，应用只拿得到你选中的那一个文件。" +
                    "写入相册：Android 10 及以上无需权限；Android 9 及以下需要存储权限。" +
                    "不申请定位、通讯录、相机、麦克风权限。",
            )
        }

        Spacer(Modifier.height(22.dp))
        PrimaryActionButton(text = "明白了，去选发票", onClick = onGoHome)
        Spacer(Modifier.height(10.dp))
        SecondaryActionButton(text = "查看完整隐私政策", onClick = onOpenPolicy)
        Spacer(Modifier.height(30.dp))
    }
}
