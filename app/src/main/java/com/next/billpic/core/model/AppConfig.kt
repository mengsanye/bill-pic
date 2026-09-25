package com.next.billpic.core.model

import com.next.billpic.BuildConfig

/**
 * 全局可调参数。
 *
 * 想改产品行为时先看这里：页数上限、渲染保护、主按钮文案、项目链接。
 * 不需要动转换引擎、界面结构或其它文案。
 */
object AppConfig {

    const val APP_NAME = "BillPic"

    /** 单次最多转多少页。超过会截断并在界面上提示，避免一次点爆内存。 */
    const val MAX_PAGES = 30

    /** 渲染保护：单边不超过 4096px，总像素不超过 1200 万，防止老机型 OOM。 */
    const val MAX_RENDER_DIM = 4096
    const val MAX_RENDER_PIXELS = 12_000_000L

    /* ---------------- 项目信息 ----------------
     * fork 或改名后需要同步这一处，以及 README 与 LICENSE 里的链接。
     */

    const val PROJECT_URL = "https://github.com/mengsanye/bill-pic"

    /** 问题反馈入口。开源项目用 issue 代替应用内反馈表单。 */
    const val ISSUES_URL = "$PROJECT_URL/issues"

    /** 许可证名称。界面上只展示名字，完整文本见仓库根目录 LICENSE。 */
    const val LICENSE_NAME = "MIT"

    /* ---------------- 文案 ----------------
     * 首页主按钮同时承担「选文件」与「重新选择」，文案只此一处。
     */

    const val PRIMARY_CTA = "选择 PDF 发票"

    /** 版本号单一来源：直接读构建产物，避免界面上再硬编码一份导致漂移。 */
    val VERSION_NAME: String = BuildConfig.VERSION_NAME
}
