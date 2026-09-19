package com.next.billpic.core.model

import com.next.billpic.BuildConfig

/**
 * 全局可调参数。
 *
 * 验证期与上架期共用一个 AppConfig：走查专用参数集中标注「走查专用」，
 * 上架包把它们编译成固定值（如 CTA 文案从 A/B 收敛为单选）。
 */
object AppConfig {

    const val APP_NAME = "BillPic"
    const val TAGLINE = "发票变图片"

    /** 单次最多转多少页。超过会截断并提示，避免一次点爆内存。 */
    const val MAX_PAGES = 30

    /** 渲染保护：单边不超过 4096px，总像素不超过 1200 万，防止老机型 OOM。 */
    const val MAX_RENDER_DIM = 4096
    const val MAX_RENDER_PIXELS = 12_000_000L

    /* ---------------- 主体信息（上架必备，需替换为真实信息） ---------------- */

    /**
     * 开发者主体。必须与「关于」页、隐私政策、应用商店开发者名称三者一致。
     * 商店审核会交叉核对，不一致会被驳回。
     */
    const val DEVELOPER_NAME = "待填写：开发者主体名称"

    /**
     * 反馈与隐私咨询邮箱。应用商店要求隐私政策必须包含
     * 「开发者信息 + 隐私问题联系人 / 咨询机制」。
     */
    const val CONTACT_EMAIL = "待填写：反馈邮箱"

    /**
     * App 备案编号。工信部《关于开展移动互联网应用程序备案工作的通知》要求
     * 「APP主办者应当在APP显著位置标明其备案编号，并在备案编号下方按要求链接备案系统网址」。
     * 未取得备案号前**不得上架**——这个占位符会以警示色显示在「关于」页，避免被漏做。
     */
    const val ICP_BEIAN_NUMBER = "备案编号待补充"

    /** 备案系统网址，按规范需置于备案编号下方供公众核查。 */
    const val ICP_BEIAN_URL = "https://beian.miit.gov.cn/"

    /** 占位符检测：界面据此把未填项标成警示态，避免带着占位符上线。 */
    fun isPlaceholder(value: String): Boolean =
        value.isBlank() || value.startsWith("待填写") || value.contains("待补充")

    /* ---------------- 走查专用：验证假设与及格线 ---------------- */

    const val HYPOTHESIS =
        "需要把 PDF 发票变成图片的手机用户，愿意用 BillPic 完成转换，" +
            "并认为它比自己截图或找别的工具更省事。"

    const val SUCCESS_METRIC = "完成率 ≥ 60%，平均评分 ≥ 4.0，且 ≥ 3 人表示「一定会用」。"
    const val COMPLETE_RATE_TARGET = 0.6
    const val RATING_TARGET = 4.0
    const val SAMPLE_TARGET = 5

    /* ---------------- 主按钮文案 ---------------- */

    /**
     * 上架包固定使用的主按钮文案。
     * A/B 测试是走查期的手段，结论落地后收敛为一个，避免上架包继续随机分流。
     */
    const val RELEASE_CTA = "选择 PDF 发票"

    const val AB_TEST_NAME = "primary_cta_copy"

    /** 走查期的两个候选文案 */
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

    /* ---------------- 反馈选项（走查专用） ---------------- */

    val FEEDBACK_INTENTS = listOf("一定会用", "可能会用", "不会用")

    val RATING_LABELS = listOf(
        "",
        "完全没解决问题",
        "有点失望",
        "一般般",
        "还不错",
        "很有用，愿意继续用",
    )

    /**
     * 走查开关。
     *
     * debug=true / release=false 由 buildType 注入。但请注意：
     * **它只决定界面显示，不负责「上架包是否采集」**——那件事由 src/release 源集
     * 把 TelemetryProvider 换成空实现来保证，是结构性事实。
     */
    val VALIDATION_PANEL_ENABLED: Boolean = BuildConfig.VALIDATION_PANEL

    /** 版本号单一来源：直接读构建产物，避免界面上再硬编码一份导致漂移。 */
    val VERSION_NAME: String = BuildConfig.VERSION_NAME
}
