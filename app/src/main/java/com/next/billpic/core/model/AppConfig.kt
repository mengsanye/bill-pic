package com.next.billpic.core.model

import com.next.billpic.BuildConfig

/**
 * 验证原型的全部可调参数集中在这里。
 *
 * 换产品想法时，只需要改这个文件 + 界面文案，转换引擎、埋点、漏斗、A/B 都不用动。
 */
object AppConfig {

    const val APP_NAME = "BillPic"
    const val TAGLINE = "发票变图片"

    /** 单次最多转多少页。超过会截断并提示，避免一次点爆内存。 */
    const val MAX_PAGES = 30

    /** JPEG 编码质量，与原型一致 */
    const val JPEG_QUALITY = 92

    /** 渲染保护：单边不超过 4096px，总像素不超过 1200 万，防止老机型 OOM。 */
    const val MAX_RENDER_DIM = 4096
    const val MAX_RENDER_PIXELS = 12_000_000L

    /* ---------------- 验证假设与及格线 ---------------- */

    const val HYPOTHESIS =
        "需要把 PDF 发票变成图片的手机用户，愿意用 BillPic 完成转换，" +
            "并认为它比自己截图或找别的工具更省事。"

    const val SUCCESS_METRIC = "完成率 ≥ 60%，平均评分 ≥ 4.0，且 ≥ 3 人表示「一定会用」。"
    const val COMPLETE_RATE_TARGET = 0.6
    const val RATING_TARGET = 4.0
    const val SAMPLE_TARGET = 5

    /* ---------------- A/B 测试：主按钮文案 ---------------- */

    const val AB_TEST_NAME = "primary_cta_copy"
    val AB_VARIANTS = listOf("选择 PDF 发票", "把 PDF 变成图片")

    /** 与原型同一套哈希算法：同一个会话 id 永远分到同一组，保证数据可比。 */
    fun assignVariant(seed: String): String {
        var hash = 0
        for (ch in seed) {
            hash = (hash shl 5) - hash + ch.code
        }
        val unsigned = hash.toLong() and 0xFFFFFFFFL
        return AB_VARIANTS[(unsigned % AB_VARIANTS.size).toInt()]
    }

    /* ---------------- 反馈选项 ---------------- */

    val FEEDBACK_INTENTS = listOf("一定会用", "可能会用", "不会用")

    val RATING_LABELS = listOf(
        "",
        "完全没解决问题",
        "有点失望",
        "一般般",
        "还不错",
        "很有用，愿意继续用",
    )

    /** 验证看板开关：对外演示时改成 false 编译即可隐藏。 */
    val VALIDATION_PANEL_ENABLED: Boolean = BuildConfig.VALIDATION_PANEL
}
