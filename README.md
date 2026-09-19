# BillPic · Android 客户端

把 PDF 格式发票一键转成图片（JPG / PNG）。
**转换全程在手机上完成，应用没有申请联网权限** —— 发票在系统层面就不可能被上传。

---

## 1. 技术选型

| 维度 | 选择 | 为什么 |
|---|---|---|
| 语言 / UI | Kotlin + Jetpack Compose (Material 3) | 声明式 UI，状态驱动，改一处交互不用动三处 XML |
| 架构 | 单 Activity + 单一 `MainUiState` + `StateFlow` | 只有 4 屏，引 Navigation / DI 框架属于负债 |
| PDF 渲染 | **`android.graphics.pdf.PdfRenderer`（系统原生）** | 零第三方依赖；APK 小、启动快、无 CVE 面；渲染质量足够 |
| 图片编码 | `Bitmap.compress`（JPEG 92 / PNG 无损） | 与原型参数一致 |
| 相册写入 | `MediaStore`（API 29+）/ 公共目录 + 扫描（API 24–28） | 对接系统相册，用户能直接在相册、微信里找到 |
| 本地存储 | `SharedPreferences` 里存 JSON | 数据量小、结构会随走查迭代；数据模型稳定后再换 Room |
| 最低版本 | minSdk 24（Android 7.0）/ targetSdk 36 | 覆盖存量设备，同时满足最新上架要求 |

**关键决策：不做「图片互转」之外的任何格式支持。** 原型验证的就是「选 PDF → 拿到图」这一条路径，
Word / Excel / 图片互转全部砍掉，避免范围蔓延把验证拖垮。

---

## 2. 工程结构

```
app/src/main/java/com/next/billpic/
├── MainActivity.kt                 入口（edge-to-edge）
├── core/
│   ├── model/
│   │   ├── BillPicModels.kt        领域模型（格式、清晰度、结果页、反馈、会话…）
│   │   ├── AppConfig.kt            ★ 全部可调参数：假设、及格线、A/B 文案、限制
│   │   └── ValidationMetrics.kt    指标与漏斗计算（纯函数，可单测）
│   ├── pdf/PdfConverter.kt         PdfRenderer 逐页渲染 + 尺寸/显存保护
│   ├── io/PickedFile.kt            SAF Uri → 缓存文件（保证可随机读取）
│   ├── media/MediaSaver.kt         写相册 / FileProvider 分享 / 导出 JSON
│   ├── data/TelemetryStore.kt      走查数据持久化
│   └── util/Images.kt              采样解码 + 格式化
└── ui/
    ├── MainViewModel.kt            全部交互与埋点（唯一数据源）
    ├── BillPicApp.kt               Scaffold + 底部 Tab + 弹层编排
    ├── components/AppComponents.kt 分段控件、进度环、HUD、行、星级…
    ├── screens/                    转换 / 结果 / 记录 / 我的 / 隐私 / 验证看板
    ├── sheets/                     反馈 Bottom Sheet、全屏看图
    └── theme/                      设计令牌（取自原型的语义色板）
```

**想换产品想法时只需要动 `AppConfig.kt` + 界面文案**，转换引擎、埋点、漏斗、A/B 都不用改。

---

## 3. 与原型的关系

### 完整保留
- 4 屏结构：转换 → 结果 → 记录 → 我的，外加隐私说明页
- 底部 Tab、大标题导航栏、分段控件（滑块平移动效）、转换进度环、HUD 提示
- 应用内反馈：星级 + 文字 + 「下次还会用吗」+ **是否真的拿到图片**
- 全部埋点事件、转化漏斗、A/B 分流、及格线判定、导出 JSON / 复制摘要 / 新会话

### 按 Android 习惯做的调整
| 原型（Web） | Android 实现 | 原因 |
|---|---|---|
| 拖拽上传 | 系统文件选择器（SAF） | 手机上没有拖拽；SAF 还可免声明读取权限 |
| 浏览器下载 | MediaStore 写相册 | 相册是手机上「找图」的默认位置 |
| 「长按保存」 | 新增**分享**按钮（`ACTION_SEND`） | 发票的真实去向是微信 / 邮箱，直接分享比先存再发少两步 |
| 桌面右侧抽屉式看板 | 「我的 → 验证看板」页 | 手机屏幕放不下侧栏 |
| 拖拽 + 点击两种入口 | 单一主按钮（点按即选） | 移动端只有一个自然操作 |
| pdf.js worker | 无 | 换成系统 PdfRenderer，无 JS 引擎开销 |

### 新增（原型没有、但手机场景需要）
- **分享到微信/邮件**：这是发票图片最主要的去向
- **图片放大核对**：发票字号小，全屏看图支持双指缩放至 8 倍
- **转换记录持久化**：跨重启保留最近 50 次，方便回查

---

## 4. 隐私设计（可被验证，不是口号）

1. `AndroidManifest.xml` **没有 `INTERNET` 权限** —— 应用在系统层面无法联网
2. 选文件用 SAF，**不申请任何存储读取权限**
3. 转换产物只写进你自己的相册；应用内只留文件名、页数、体积、耗时这类元信息
4. 唯一的写入权限（`WRITE_EXTERNAL_STORAGE`）限定 `maxSdkVersion="28"`，只在 Android 9 及以下需要

> 走查时可以现场演示：**开飞行模式再转一次，功能完全正常。**

---

## 5. 构建与安装

```bash
cd /Users/donshen/Documents/AndroidProjects/BillPic

# 编译 Debug 包
./gradlew assembleDebug

# 安装到已连接的设备 / 模拟器
./gradlew installDebug
# 或
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

**环境要求**：JDK 17+、Android SDK Platform 36、Gradle 由 wrapper 自动管理（9.4.1）。

### 关闭验证看板（对外演示时）
`app/build.gradle.kts` 里把 `buildConfigField("Boolean", "VALIDATION_PANEL", "true")` 改为 `"false"`，
重新编译即可隐藏「验证看板」入口，埋点数据照常记录。

---

## 6. 埋点事件字典

| 事件名 | 触发时机 | 关键字段 |
|---|---|---|
| `session_start` | 会话开始 / 新会话 | variant, engine |
| `tab_switch` | 切换底部 Tab | tab |
| `pick_tap` | 点击主按钮 | — |
| `file_select` | 选中合法 PDF | name, size_kb |
| `file_parsed` | 读出头数成功 | pages |
| `file_reject` | 选了非 PDF | ext |
| `file_parse_error` | 加密 / 损坏 / 读取失败 | message |
| `file_clear` | 移除已选文件 | — |
| `format_select` | 切换 JPG / PNG | format |
| `scale_select` | 切换清晰度 | scale |
| `conversion_start` | 点击开始转换 | format, scale, size_kb |
| `conversion_done` | 渲染完成 | pages, ms, out_kb |
| `conversion_error` | 渲染失败 | message |
| `flow_complete` | 成功拿到图片（**漏斗关键节点**） | pages, format |
| `viewer_open` / `viewer_close` | 打开 / 关闭全屏看图 | page |
| `download_single` / `download_all` | 保存单张 / 全部 | page / pages, format |
| `share_images` | 分享图片 | pages, format |
| `feedback_open` / `feedback_close` | 打开 / 关闭反馈 | — |
| `rating_select` / `intent_select` | 打分 / 选意愿 | rating / value |
| `feedback_submit` | 提交反馈 | rating, has_text, intent, **converted** |
| `privacy_open` / `about_open` | 查看隐私说明 / 关于 | — |
| `record_tap` | 点击历史记录 | — |
| `validation_panel_open` | 打开验证看板 | — |
| `new_session` | 开始新会话 | — |
| `data_export` / `summary_copy` | 导出 JSON / 复制摘要 | — |

**卡点事件**（说明产品有问题，走查时要单独看）：`conversion_error`、`file_reject`、`file_parse_error`。
理想值是 0 次。

---

## 7. 判定规则

| 指标 | 及格线 | 说明 |
|---|---|---|
| 完成率 | **≥ 60%** | 「成功拿到图片」的会话 / 总会话 |
| 平均评分 | **≥ 4.0 / 5** | 应用内反馈收集 |
| 明确意愿 | **≥ 3 人** | 选「一定会用」的人数 |
| 样本量 | **≥ 5 人** | 少于这个数不下结论 |
| 卡点事件 | 0 次 | 有卡点先修卡点，别加功能 |

四项同时达标才算通过。看板底部会直接给出「达到及格线，可进入下一轮」或
「样本或完成率不足，先补数据、别急着加功能」。

---

## 8. 后续演进建议（按优先级）

1. **真实数据一旦证明价值，先补「批量多文件」** —— 报销季用户手里往往不止一张发票
2. 接入真实埋点平台（Firebase / 自建），把 `TelemetryStore` 换成上报实现即可，界面代码不用动
3. 若要做订阅：先验证「一次性需求」还是「高频需求」，这直接决定商业模式
4. `AppConfig` 里的假设与及格线建议直接写进 PRD，作为这一版的需求验收标准
5. 上架前补齐：签名配置、隐私政策、R8 混淆规则、多分辨率图标（当前为 AS 默认图标）

---

## 9. 真机验证记录

在 Pixel 系统镜像（Android 16 / API 36，1080×2400，模拟器）上完整走查了一遍真实链路，
截图见 `docs/screenshots/`。

### 已验证通过

| 验证项 | 结果 |
|---|---|
| 编译 | `./gradlew :app:clean :app:assembleDebug` **干净全量重编通过**（39 任务全部实际执行，非缓存），产物 11.5 MiB |
| **权限清单** | 只有 `WRITE_EXTERNAL_STORAGE (maxSdkVersion=28)` + 一条系统内部权限，**确认无 INTERNET** |
| 启动与渲染 | 冷启动正常，四屏全部按原型布局渲染，无崩溃、无异常日志 |
| 选文件 | SAF 选择器只筛选 PDF；选中后自动读出「共 2 页」并自动开始转换 |
| **真实转换** | 2 页 PDF → **1190 × 1684 px**，与原型 pdf.js 计算完全一致，耗时 44 ms |
| **输出图片真实性** | 拉回产物用 `file` 与 JPEG SOF 段解析双重校验：**均为有效 baseline JPEG，1190×1684** |
| **相册写入** | MediaStore 登记成功：`bucket_display_name=BillPic`、`relative_path=Pictures/BillPic/`、`is_pending=0`，相册可直接看到 |
| 验证看板 | 事件流完整记录真实操作：`session_start → pick_tap → file_select → file_parsed → conversion_start → conversion_done → flow_complete → download_all`；A/B 显示「把 PDF 变成图片 · 1 人 · 完成 100%」 |
| 反馈 | 提交后入账：`feedback_submit {rating=4, has_text=true, intent=一定会用, converted=false}` |
| 隐私页 / 记录页 / 所有弹层 | 渲染与交互正常 |

### 构建指纹（交付版本）

| 项 | 值 |
|---|---|
| 产物 | `app/build/outputs/apk/debug/app-debug.apk` |
| 体积 | 11.5 MiB / 12.06 MB（12,063,095 字节） |
| SHA-256 | `b1a33e2c1881c44ddbb667b0e1878f8b705ef1d4a5e46279fa967561092bdc75` |
| 包名 / 版本 | `com.next.billpic` · versionCode 1 · versionName 1.0 |
| SDK | minSdk 24（Android 7.0）· targetSdk 36 · compileSdk 36 |
| 入口 | `com.next.billpic.MainActivity` |
| dex | 13 个（Compose 调试构建正常多 dex） |
| 签名 | Android Debug 证书（`C=US, O=Android, CN=Android Debug`）——**仅适用于内部走查安装，上架需换 release 签名** |

### 真机跑出来的两个 bug（均已修复并复验）

1. **未打分时错误提示只变色、不换文案**
   错误字符串被设置了却从未渲染 —— 现在会正确显示「请先点一下星星打个分」。
2. **键盘弹出后遮住「提交反馈」**
   `enableEdgeToEdge()` 之后 `adjustResize` 不再自动压缩窗口，输入法高度必须自己让出来。
   已在反馈弹层加 `imePadding()`，复验键盘弹出时提交按钮完整可见。

> 这两个问题都是「看代码看不出来、跑起来一眼就看见」的类型 —— 建议团队每次改动后都至少装一次真机过一遍主流程。

### 已知限制

- 测试用的 PDF 是我用 Helvetica 生成的，**中文字符在 PDF 里本身就被替换成了 `?`**，
  所以渲染结果也是 `?` —— 这说明渲染是忠实的，不是我们渲染错了。
  真实电子发票都会内嵌字体，请用**团队自己的真实发票**再走一遍确认中文清晰度。
- 未做：多分辨率启动图标（当前仍是 Android Studio 默认图标）、release 签名配置、R8 混淆规则。

---

**BillPic Android v1** · 仅用于内部走查验证
