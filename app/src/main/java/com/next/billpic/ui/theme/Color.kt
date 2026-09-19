package com.next.billpic.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BillPic 设计令牌。
 *
 * 取自产品原型（mobile.css）中的语义色，而不是 M3 默认的紫色主题：
 * 保留产品既定视觉，同时用 Compose 的语义化方式表达，方便日后统一调整。
 */

/* ---------- 浅色 ---------- */
internal val BgLight = Color(0xFFF2F2F7)
internal val CardLight = Color(0xFFFFFFFF)
internal val FillLight = Color(0x1F767680)
internal val SepLight = Color(0x243C3C43)
internal val LabelLight = Color(0xFF000000)
internal val Label2Light = Color(0x993C3C43)
internal val Label3Light = Color(0x523C3C43)
internal val BlueLight = Color(0xFF007AFF)
internal val GreenLight = Color(0xFF34C759)
internal val OrangeLight = Color(0xFFFF9500)
internal val RedLight = Color(0xFFFF3B30)
internal val OnBlueLight = Color(0xFFFFFFFF)

/* ---------- 深色 ---------- */
internal val BgDark = Color(0xFF000000)
internal val CardDark = Color(0xFF1C1C1E)
internal val FillDark = Color(0x3D767680)
internal val SepDark = Color(0x80545458)
internal val LabelDark = Color(0xFFFFFFFF)
internal val Label2Dark = Color(0x99EBEBF5)
internal val Label3Dark = Color(0x4DEBEBF5)
internal val BlueDark = Color(0xFF0A84FF)
internal val GreenDark = Color(0xFF30D158)
internal val OrangeDark = Color(0xFFFF9F0A)
internal val RedDark = Color(0xFFFF453A)
internal val OnBlueDark = Color(0xFFFFFFFF)

/** 半透明遮罩：Bottom Sheet、全屏看图、转换中等弹层共用 */
internal val ScrimLight = Color(0x4D000000)
internal val ScrimDark = Color(0x99000000)
