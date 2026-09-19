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
import com.next.billpic.core.model.AppConfig
import com.next.billpic.core.model.UserSnapshot
import com.next.billpic.ui.components.RowDivider
import com.next.billpic.ui.components.ScreenScroll
import com.next.billpic.ui.components.SectionHeader
import com.next.billpic.ui.components.SurfaceCard
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 常见问题。
 *
 * 选题标准：只收录**客服会被真的问到**的问题——转换效果、保存位置、页码与上限
 * 这类用户自己就能验证的事，放在这里可以减少一次提问往返。
 */
@Composable
fun FaqScreen(modifier: Modifier = Modifier) {
    val palette = AppColor

    ScreenScroll(modifier = modifier) {
        SectionHeader("转换效果")
        SurfaceCard {
            FaqGroup(
                listOf(
                    "转出来的中文变成了「?」或方块" to
                        "这说明原始 PDF 里没有内嵌中文字体。渲染引擎只能忠实呈现文件本身的内容，" +
                        "不会替换成别的字体。换一个内嵌字体的发票文件即可正常显示。",
                    "文字有点糊，能更清楚吗" to
                        "把「输出档位」调到「高清」。若原文件本身是低分辨率扫描件，" +
                        "提高档位不会凭空增加细节。",
                ),
            )
        }

        SectionHeader("文件与保存")
        SurfaceCard {
            FaqGroup(
                listOf(
                    "图片存到哪里了" to
                        "存到系统相册的 BillPic 相册，可以直接在相册或文件管理里找到。" +
                        "应用内只保留转换记录，不额外存一份图片副本。",
                    "为什么最多只能转 ${AppConfig.MAX_PAGES} 页" to
                        "防止一次渲染过多页面把手机内存占满。如果发票很长，可以在「页码范围」里分批转，" +
                        "例如先转 1-15，再转 16-30。",
                    "能只转其中几页吗" to
                        "可以。在首页「输出设置 → 页码范围」里填，例如 1-3,5,8-10；留空表示转换全部页面。",
                    "加密的 PDF 能转吗" to
                        "不能。请先用其他工具解除密码后再转换。",
                    "转出来文件太大，报销平台传不上去" to
                        "把「输出档位」调到「省空间」。这一档同时降低渲染倍数与压缩质量，文件会明显变小。",
                ),
            )
        }

        SectionHeader("隐私与数据")
        SurfaceCard {
            FaqGroup(
                listOf(
                    "会用我的流量吗" to
                        "不会。这个应用没有申请联网权限，转换全程在本机完成，断网也能用。",
                    "转换记录会一直留着吗" to
                        "最多保留最近 ${UserSnapshot.MAX_HISTORY} 条，超出后自动丢弃最旧的。" +
                        "也可以随时在「我的 → 隐私与数据 → 清除转换记录」里一键清空。",
                    "卸载应用会怎样" to
                        "本机保存的转换记录会一并删除。已保存到系统相册的图片不受影响，需要你自行处理。",
                ),
            )
        }

        SurfaceCard(modifier = Modifier.padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(text = "没找到答案？", style = AppText.BodyStrong, color = palette.label)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "在「我的 → 意见反馈」里把问题发给我们，附上「关于」页导出的诊断信息，" +
                        "可以帮我们更快定位。",
                    style = AppText.Sub,
                    color = palette.label2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

/** 一组问答，组内自动补分隔线（最后一条不加）。 */
@Composable
private fun FaqGroup(items: List<Pair<String, String>>) {
    val palette = AppColor
    items.forEachIndexed { index, (question, answer) ->
        if (index > 0) RowDivider(startPadding = 16)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(text = question, style = AppText.BodyStrong, color = palette.label)
            Spacer(Modifier.height(6.dp))
            Text(text = answer, style = AppText.Sub, color = palette.label2)
        }
    }
}
