package com.next.billpic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.next.billpic.R
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText
import kotlinx.coroutines.delay

/**
 * 「下拉快捷入口」引导弹框。
 *
 * 按系统能力分两条路：
 *  - Android 13+ ：系统提供一键添加 → 主按钮「立即添加」+ 次按钮「以后再说」；
 *  - Android 12 及以下：没有该 API，只能照着引导图手动添加 → 单按钮「我知道了」。
 *
 * 动图当前由代码绘制的三步示意动画承担（[QuickSettingsDemo]），不依赖任何外部素材；
 * 若设计后续产出正式动图（animated webp），替换该组件即可，调用方无需改动。
 */
@Composable
fun QuickSettingsGuideDialog(
    canOneTapAdd: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AppColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "把「发票转图片」放到下拉栏",
                style = AppText.Title,
                color = palette.label,
            )
        },
        text = {
            Column {
                QuickSettingsDemo()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "下拉通知栏，点一下就能直接转发票，不用再翻找 App。",
                    style = AppText.Sub,
                    color = palette.label2,
                )
                if (!canOneTapAdd) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "添加方法：下拉通知栏 → 点右上角编辑 → 把「发票转图片」拖进快捷栏。",
                        style = AppText.Caption,
                        color = palette.label3,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = if (canOneTapAdd) onAdd else onDismiss) {
                Text(
                    text = if (canOneTapAdd) "立即添加" else "我知道了",
                    style = AppText.BodyStrong,
                    color = palette.blue,
                )
            }
        },
        dismissButton = if (canOneTapAdd) {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = "以后再说", style = AppText.Body, color = palette.label2)
                }
            }
        } else {
            null
        },
    )
}

/**
 * 三步示意动画：下拉通知栏 → 点编辑 → 拖入图块。
 *
 * 用循环高亮当前步骤来表达「操作顺序」，比一张静态图更能说明动作；
 * 纯本机绘制，无网络、无外部资源。
 *
 * 实现刻意不依赖 animation-core（各版本 API 差异大）：用协程循环推进下标即可，
 * 效果等价且没有版本兼容风险。
 */
@Composable
private fun QuickSettingsDemo(modifier: Modifier = Modifier) {
    val palette = AppColor
    val steps = listOf(
        R.drawable.ic_chevron_right to "下拉通知栏",
        R.drawable.ic_plus to "点击编辑",
        R.drawable.ic_qs_tile to "拖入图块",
    )

    var activeStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            activeStep = (activeStep + 1) % steps.size
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.fill)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            DemoStep(
                iconRes = step.first,
                label = step.second,
                active = index == activeStep,
            )
            if (index != steps.lastIndex) {
                Text(text = "→", style = AppText.Caption, color = palette.label3)
            }
        }
    }
}

@Composable
private fun DemoStep(iconRes: Int, label: String, active: Boolean) {
    val palette = AppColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (active) palette.blue.copy(alpha = 0.14f) else palette.card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (active) palette.blue else palette.label3,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = AppText.Caption,
            color = if (active) palette.label else palette.label2,
        )
    }
}
