package com.next.billpic.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.next.billpic.core.model.LegalText
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 通用法律文本页。
 *
 * 隐私政策与用户协议结构一致，共用一个渲染器——两份文本将来都要经法务改稿，
 * 共用一处版式可以保证改稿时只调一次。
 */
@Composable
fun LegalScreen(
    sections: List<LegalText.Section>,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor

    ScreenScroll(modifier = modifier) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "文本版本 ${LegalText.VERSION}",
            style = AppText.Caption,
            color = palette.label3,
        )
        Spacer(Modifier.height(14.dp))

        sections.forEach { section ->
            SurfaceCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = section.title,
                        style = AppText.BodyStrong,
                        color = palette.label,
                    )
                    Spacer(Modifier.height(8.dp))
                    section.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = AppText.Sub,
                            color = palette.label2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "本页为产品侧拟定初稿，正式发布前将经法务审核定稿；" +
                "应用商店描述中的数据说明与本政策保持一致。",
            style = AppText.Caption,
            color = palette.label3,
        )
        Spacer(Modifier.height(30.dp))
    }
}
