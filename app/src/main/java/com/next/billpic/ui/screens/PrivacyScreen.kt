package com.next.billpic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.next.billpic.ui.components.PlainInfoRow
import com.next.billpic.ui.components.PrimaryActionButton
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 隐私说明。
 *
 * 这屏是刻意加的：发票上全是个人信息，「不上传服务器」很可能是 BillPic 的核心差异点，
 * 所以把它做成可点击的一屏，走查时观察用户会不会主动来看、看完信不信。
 */
@Composable
fun PrivacyScreen(onGoHome: () -> Unit, modifier: Modifier = Modifier) {
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
                Text(text = "🔒", fontSize = 28.sp)
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
                title = "留存记录",
                body = "我们不保存你的发票内容。转换结果只写进你自己的相册，应用内只留转换记录。",
            )
            RowDivider(startPadding = 16)
            PlainInfoRow(
                title = "怎么验证",
                body = "这个 App 根本没有申请联网权限 —— 开飞行模式再转一次，功能照常，" +
                    "这就是最直接的证明。",
            )
        }

        Spacer(Modifier.height(22.dp))
        PrimaryActionButton(text = "明白了，去选发票", onClick = onGoHome)
        Spacer(Modifier.height(30.dp))
    }
}
