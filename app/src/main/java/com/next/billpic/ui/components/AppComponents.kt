package com.next.billpic.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.next.billpic.R
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 触觉反馈。
 * 直接走 View 的原生 API，不额外申请 VIBRATE 权限，并且会遵循系统「触感反馈」开关。
 */
@Composable
fun rememberTapHaptics(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}

/* ------------------------------ 容器 ------------------------------ */

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColor.card)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun ScreenScroll(
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 20,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    val palette = AppColor
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = AppText.SectionTitle, color = palette.label2)
        if (!trailing.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(text = trailing, style = AppText.Caption, color = palette.label3)
        }
    }
}

@Composable
fun RowDivider(startPadding: Int = 60, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding.dp)
            .height(0.5.dp)
            .background(AppColor.separator),
    )
}

/* ------------------------------ 导航栏 ------------------------------ */

@Composable
fun LargeTitleBar(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = AppText.LargeTitle,
        color = AppColor.label,
        modifier = modifier
            .fillMaxWidth()
            // 上间距 12dp 不是随手取的：状态栏 inset 已由 Scaffold 让出（实测 24dp），
            // 32sp 标题行框内还有约 6dp 行距，两者相加让字形顶边落在屏幕 42dp 处 ——
            // 与子页面返回栏（46dp 栏内居中）的内容顶边对齐，切页时页头不会跳。
            // 这个值曾为 4dp，那是 edge-to-edge 之前的遗留，会让标题贴住状态栏。
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
    )
}

/**
 * 导航栏（带返回）。
 *
 * `trailing` 用于放页面级次要动作，例如结果页的「换一份发票」——
 * 放在导航栏而不是底部操作栏，是为了不挤占「保存全部」这个主操作的位置。
 */
@Composable
fun BackTitleBar(
    backLabel: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    haptics()
                    onBack()
                }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = null,
                tint = palette.blue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(text = backLabel, style = AppText.Body, color = palette.blue)
        }
        Text(text = title, style = AppText.NavTitle, color = palette.label)
        if (trailing != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

/* ------------------------------ 按钮 ------------------------------ */

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIconRes: Int? = null,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    val container = if (enabled) palette.blue else palette.fill
    val contentColor = if (enabled) palette.onBlue else palette.label3
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(enabled = enabled) {
                haptics()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIconRes != null) {
            Icon(
                painter = painterResource(id = leadingIconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = AppText.Button,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIconRes: Int? = null,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.fill)
            .clickable(enabled = enabled) {
                haptics()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIconRes != null) {
            Icon(
                painter = painterResource(id = leadingIconRes),
                contentDescription = null,
                tint = palette.blue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = AppText.BodyStrong, color = palette.blue, textAlign = TextAlign.Center)
    }
}

@Composable
fun LinkTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AppColor
    Text(
        text = text,
        style = AppText.BodyStrong,
        color = palette.blue,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

/* ------------------------------ 分段控件 ------------------------------ */

/**
 * 自绘分段控件。
 * 原型的滑块是「跟着选中项平移」的，这种动效在手机上比 Material 的 ToggleButton 更好读，
 * 而且自绘可以完全控制 32dp 高度与圆角，视觉与原型一致。
 */
@Composable
fun SegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    val count = labels.size.coerceAtLeast(1)
    val safeIndex = selectedIndex.coerceIn(0, count - 1)

    BoxWithConstraints(modifier = modifier.height(32.dp)) {
        val thumbWidth = (maxWidth - 4.dp) / count
        val offset by animateDpAsState(
            targetValue = thumbWidth * safeIndex,
            animationSpec = tween(durationMillis = 180),
            label = "segmentThumb",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(9.dp))
                .background(palette.fill)
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.card),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                labels.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (index != safeIndex) {
                                    haptics()
                                    onSelect(index)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = AppText.Note,
                            color = if (index == safeIndex) palette.label else palette.label2,
                            fontWeight = if (index == safeIndex) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isOn = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isOn) palette.blue.copy(alpha = 0.12f) else palette.fill)
                    .border(
                        width = 1.dp,
                        color = if (isOn) palette.blue else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable {
                        if (!isOn) {
                            haptics()
                            onSelect(option)
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = AppText.Note,
                    color = if (isOn) palette.blue else palette.label2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/* ------------------------------ 行 ------------------------------ */

@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppText.Body,
            color = AppColor.label,
            modifier = Modifier.weight(1f),
        )
        content()
    }
}

@Composable
fun MetaRow(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = AppColor
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppText.Sub,
            color = palette.label2,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = AppText.BodyStrong,
            color = palette.label,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f),
        )
    }
}

/**
 * 列表动作行。
 *
 * 图标用矢量而不是 emoji：emoji 在各厂商 ROM 上会被替换成自家字形（华为/小米/三星
 * 三套风格），且无法跟随主题 tint，在「我的」这类系统感强的页面里尤其突兀。
 */
@Composable
fun ListActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val palette = AppColor
    val tone = accent ?: palette.blue
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tone.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppText.BodyStrong, color = palette.label)
            Text(text = subtitle, style = AppText.Caption, color = palette.label2)
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = palette.label3,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun PlainInfoRow(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(text = title, style = AppText.BodyStrong, color = palette.label)
        Spacer(Modifier.height(4.dp))
        Text(text = body, style = AppText.Sub, color = palette.label2)
    }
}

/* ------------------------------ 其它 ------------------------------ */

@Composable
fun PdfBadge(label: String = "PDF", modifier: Modifier = Modifier) {
    val palette = AppColor
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.red.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.red,
        )
    }
}

@Composable
fun EmptyState(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 72.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = palette.label3,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(text = title, style = AppText.Title, color = palette.label)
        Spacer(Modifier.height(6.dp))
        Text(text = description, style = AppText.Sub, color = palette.label2)
    }
}

/**
 * 单行文本输入框。
 *
 * 自绘而不是用 M3 的 OutlinedTextField：后者的浮动标签与默认高度会让
 * 「输出设置」卡片里突然冒出一个 Material 味很重的控件，破坏既定视觉。
 * 校验失败时边框转红、下方给一句人话说明——不只画个红框了事。
 */
@Composable
fun TextInputRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    helper: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = AppColor
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        error != null -> palette.red
        focused -> palette.blue
        else -> palette.separator
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(palette.fill)
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = AppText.Body, color = palette.label3)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = AppText.Body.copy(color = palette.label),
                    cursorBrush = SolidColor(palette.blue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                trailing()
            }
        }
        val note = error ?: helper
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = note,
                style = AppText.Caption,
                color = if (error != null) palette.red else palette.label3,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/** 转换进度环 */
@Composable
fun ProgressRing(
    progress: Float,
    centerText: String,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220),
        label = "ringProgress",
    )
    Box(modifier = modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = palette.fill,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = palette.blue,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(text = centerText, style = AppText.BodyStrong, color = palette.label)
    }
}

/** 系统级提示（HUD）。不拦截触摸，2.2 秒后自动消失，与原型一致。 */
@Composable
fun HudOverlay(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    val palette = AppColor
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = AppText.Note,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (palette.isDark) Color(0xF2363638) else Color(0xF21F1F1F))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

/** 星级评分：每颗星都保证 48dp 触摸目标 */
@Composable
fun StarSelector(
    rating: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        (1..5).forEach { index ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics()
                        onRate(index)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (index <= rating) "★" else "☆",
                    fontSize = 30.sp,
                    color = if (index <= rating) palette.orange else palette.label3,
                )
            }
        }
    }
}
