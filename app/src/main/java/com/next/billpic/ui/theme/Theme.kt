package com.next.billpic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * BillPic 的语义色板。
 *
 * M3 的 ColorScheme 无法表达原型里「分隔线 / 填充 / 三级文字」这类语义，
 * 所以这里额外提供一层业务色板，界面统一从 [AppColor] 取值。
 */
@Immutable
data class BillPicPalette(
    val bg: Color,
    val card: Color,
    val fill: Color,
    val separator: Color,
    val label: Color,
    val label2: Color,
    val label3: Color,
    val blue: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val onBlue: Color,
    val scrim: Color,
    val isDark: Boolean,
)

internal val LightPalette = BillPicPalette(
    bg = BgLight,
    card = CardLight,
    fill = FillLight,
    separator = SepLight,
    label = LabelLight,
    label2 = Label2Light,
    label3 = Label3Light,
    blue = BlueLight,
    green = GreenLight,
    orange = OrangeLight,
    red = RedLight,
    onBlue = OnBlueLight,
    scrim = ScrimLight,
    isDark = false,
)

internal val DarkPalette = BillPicPalette(
    bg = BgDark,
    card = CardDark,
    fill = FillDark,
    separator = SepDark,
    label = LabelDark,
    label2 = Label2Dark,
    label3 = Label3Dark,
    blue = BlueDark,
    green = GreenDark,
    orange = OrangeDark,
    red = RedDark,
    onBlue = OnBlueDark,
    scrim = ScrimDark,
    isDark = true,
)

val LocalBillPicPalette = staticCompositionLocalOf { LightPalette }

/** 取当前主题下的业务色板：`AppColor.blue`、`AppColor.label2` ... */
val AppColor: BillPicPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalBillPicPalette.current

private val LightScheme = androidx.compose.material3.lightColorScheme(
    primary = BlueLight,
    onPrimary = OnBlueLight,
    secondary = GreenLight,
    onSecondary = OnBlueLight,
    background = BgLight,
    onBackground = LabelLight,
    surface = CardLight,
    onSurface = LabelLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = Label2Light,
    outline = Label3Light,
    error = RedLight,
    onError = OnBlueLight,
)

private val DarkScheme = androidx.compose.material3.darkColorScheme(
    primary = BlueDark,
    onPrimary = OnBlueDark,
    secondary = GreenDark,
    onSecondary = OnBlueDark,
    background = BgDark,
    onBackground = LabelDark,
    surface = CardDark,
    onSurface = LabelDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = Label2Dark,
    outline = Label3Dark,
    error = RedDark,
    onError = OnBlueDark,
)

@Composable
fun BillPicTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalBillPicPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(),
            content = content,
        )
    }
}
