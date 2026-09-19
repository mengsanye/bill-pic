package com.next.billpic.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.next.billpic.core.model.AppConfig
import com.next.billpic.ui.components.ChoiceChips
import com.next.billpic.ui.components.PrimaryActionButton
import com.next.billpic.ui.components.StarSelector
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 反馈弹层。
 *
 * 自绘 Bottom Sheet 而不是用 M3 的 ModalBottomSheet：
 * 一来视觉与原型完全一致（圆角、抓手段、滑入时长），二来不依赖实验性 API。
 *
 * 反馈会一并记录「这个用户有没有真的拿到图片」——
 * 这是区分「没用懂」和「不好用」的关键字段。
 */
@Composable
fun FeedbackSheetOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, text: String, intent: String) -> Boolean,
    onRatingSelected: (Int) -> Unit,
    onIntentSelected: (String) -> Unit,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()

    var rating by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var intent by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            rating = 0
            text = ""
            intent = ""
            error = ""
        }
    }

    // 关键：edge-to-edge 下窗口不再被输入法自动压缩，
    // 必须显式让出 IME 高度，否则键盘会盖住「提交反馈」。
    BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding()) {
        val sheetMaxHeight = maxHeight * 0.9f

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(durationMillis = 160)),
            exit = fadeOut(tween(durationMillis = 160)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() },
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(durationMillis = 240)) { height -> height } +
                fadeIn(tween(durationMillis = 160)),
            exit = slideOutVertically(tween(durationMillis = 200)) { height -> height } +
                fadeOut(tween(durationMillis = 120)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(palette.card)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(0.1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.label3),
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    text = "这次体验怎么样？",
                    style = AppText.Title,
                    color = palette.label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "你的反馈直接进入验证看板，用来判断 BillPic 值不值得继续做。",
                    style = AppText.Caption,
                    color = palette.label2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                StarSelector(
                    rating = rating,
                    onRate = { value ->
                        rating = value
                        error = ""
                        onRatingSelected(value)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        error.isNotBlank() -> error
                        rating == 0 -> "点一下打分"
                        else -> AppConfig.RATING_LABELS.getOrElse(rating) { "" }
                    },
                    style = AppText.Caption,
                    color = if (error.isNotBlank()) palette.red else palette.label2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "哪里最别扭，或者哪里最顺？",
                    style = AppText.Note,
                    color = palette.label2,
                )
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = AppText.Body.copy(color = palette.label),
                    cursorBrush = SolidColor(palette.blue),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.fill)
                                .padding(12.dp),
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "例如：转出来的图有点糊 / 不知道选 JPG 还是 PNG",
                                    style = AppText.Sub,
                                    color = palette.label3,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "下次要转发票，还会打开它吗？",
                    style = AppText.Note,
                    color = palette.label2,
                )
                Spacer(Modifier.height(8.dp))
                ChoiceChips(
                    options = AppConfig.FEEDBACK_INTENTS,
                    selected = intent,
                    onSelect = { value ->
                        intent = value
                        onIntentSelected(value)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))
                PrimaryActionButton(
                    text = "提交反馈",
                    onClick = {
                        val accepted = onSubmit(rating, text, intent)
                        if (!accepted) error = "请先点一下星星打个分"
                    },
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptics()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "稍后再说", style = AppText.Sub, color = palette.label2)
                }
                Spacer(Modifier.height(4.dp))
                // 弹层在 Scaffold 之外，底部要自己让开系统导航栏
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
