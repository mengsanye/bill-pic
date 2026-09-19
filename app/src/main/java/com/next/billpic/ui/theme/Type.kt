package com.next.billpic.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * BillPic 的字号体系，对齐原型中的层级：
 * 大标题 32 / 卡片标题 17 / 正文 15 / 说明 13 / 脚注 12。
 * 字号在这里集中定义，界面里只引用常量，避免散落的魔法数字。
 */
object AppText {
    val LargeTitle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 37.sp)
    val NavTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val HeroTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
    val Title = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val BodyStrong = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val Sub = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val Note = TextStyle(fontSize = 13.sp, lineHeight = 19.sp)
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
    val SectionTitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
    val Button = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val Metric = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold)
    val Mono = TextStyle(fontSize = 11.5.sp, lineHeight = 16.sp)
}
