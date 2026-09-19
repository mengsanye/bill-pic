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

### 源集划分（上架包与走查包的分界线）

```
app/src/
├── main/      两个变体共用：界面、转换引擎、用户数据、合规文本
├── debug/     ★ 只在走查包里存在：TelemetryStore、ValidationCalculator、验证看板
└── release/   ★ 只在上架包里存在：TelemetryProvider 指向空实现
```

`main` 只调用 `TelemetryProvider.create()` 与 `ValidationPanelHost()` 这两个名字，
具体实现由构建变体提供。于是**上架包里根本没有采集代码**——
「不采集」是编译产物上的结构性事实，不是一个可能被改错的布尔开关。

> 代价：`main` 源集单独看会有「未解析符号」提示，这是变体注入的正常现象，按变体编译不受影响。

### 目录

```
app/src/main/java/com/next/billpic/
├── MainActivity.kt                 入口（edge-to-edge）
├── core/
│   ├── model/
│   │   ├── BillPicModels.kt        领域模型（格式、档位、结果、反馈、会话、用户数据）
│   │   ├── AppConfig.kt            ★ 全部可调参数：主体信息、备案号、假设、及格线、A/B
│   │   ├── PageRange.kt            页码范围解析（容错中文标点/全角/写反的区间）
│   │   └── LegalText.kt            隐私政策与用户协议正文
│   ├── pdf/PdfConverter.kt         PdfRenderer 逐页渲染 + 页码选择 + 尺寸/显存保护
│   ├── io/PickedFile.kt            SAF Uri → 缓存文件（保证可随机读取）
│   ├── media/MediaSaver.kt         写相册 / FileProvider 分享 / 导出文本
│   ├── data/
│   │   ├── Telemetry.kt            采集接口（实现按变体注入）
│   │   └── UserDataStore.kt        用户自己的转换记录与偏好（上架包也需要）
│   └── util/Images.kt              采样解码 + 格式化
└── ui/
    ├── MainViewModel.kt            全部交互（唯一数据源）
    ├── BillPicApp.kt               Scaffold + 底部 Tab + 弹层与确认弹窗编排
    ├── components/                 AppComponents / AppDialogs / 矢量图标
    ├── screens/                    转换 / 结果 / 记录 / 我的 / 隐私 / 政策 / 协议 / 常见问题 / 关于
    ├── sheets/                     反馈 Bottom Sheet、全屏看图
    └── theme/                      设计令牌（取自原型的语义色板）

app/src/debug/java/com/next/billpic/
├── core/data/TelemetryStore.kt + DebugTelemetry.kt   真采集、真落盘
├── core/model/ValidationMetrics.kt                   漏斗与指标计算（纯函数）
└── ui/screens/ValidationPanelHost.kt                 验证看板整屏
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

# 走查包（含验证看板与埋点）
./gradlew :app:assembleDebug

# 上架包（R8 压缩 + 结构性剥离埋点）
./gradlew :app:assembleRelease

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

| 变体 | 产物 | 体积 | 用途 |
|---|---|---|---|
| debug | `app-debug.apk` | ~11.5 MiB | 内部走查（含验证看板） |
| release | `app-release.apk` | ~1.24 MiB | 对外分发 / 上架候选 |

**环境要求**：JDK 17+、Android SDK Platform 36、Gradle 由 wrapper 自动管理（9.4.1）。

### 上架签名

`keystore.properties` 存在时 release 使用其中的密钥；**不存在时自动回退到 debug 签名**，
保证任何一台机器都能编译出包（仅供内部走查，不可分发）。

```bash
cp keystore.properties.example keystore.properties
keytool -genkeypair -v -keystore billpic-release.jks \
  -alias billpic -keyalg RSA -keysize 2048 -validity 10000
# 然后编辑 keystore.properties 填入四项
```

`keystore.properties` 与 `*.jks` 都已在 `.gitignore` 中，密钥不会入库。

### 两个变体的差异（为什么不能只靠开关）

| | debug | release |
|---|---|---|
| 验证看板整屏实现 | 参与编译 | **不存在** |
| `TelemetryStore` / `ValidationCalculator` | 参与编译 | **不存在** |
| `TelemetryProvider.create()` | 返回 `DebugTelemetry` | 返回 `NoOpTelemetry` |
| 埋点事件名（如 `conversion_start`） | dex 中存在 | **dex 中不存在** |

验收方式（已执行，结果见第 10 节）：

```bash
unzip -p app/build/outputs/apk/release/app-release.apk 'classes*.dex' | grep -c "conversion_start"
# 期望 0
```

`AppConfig.VALIDATION_PANEL_ENABLED` 只决定界面是否显示入口；
真正保证「上架包不采集」的是 `src/release` 源集。

---

## 6. 埋点事件字典

> **仅 debug 变体有效。** release 包中不存在采集实现，这些事件名也不会出现在 dex 里。

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

### 已完成（本轮优化）

上线阻塞项与核心链路体验已全部落地，详见第 11 节。重点：

- 走查工具与埋点从源码层面移出上架包（`src/release` 源集）
- 「我的」页重构为「隐私与数据 / 帮助与反馈 / 关于」三组，补齐隐私政策、用户协议、
  常见问题、关于页、清除转换记录
- 输出档位从「清晰度」改为**体积取向**（省空间 / 标准 / 高清），命中报销平台上传限制
- 新增页码范围选择、单页分享、结果页「换一份」、记录长按删除、权限被拒引导

### 待做

1. **批量多文件**（R-18，未做）—— 需要把 `state.source` 改为文件列表、结果按文件分组，
   属架构级改动，建议单独一轮
2. **分享回流**（R-20，未做）—— 需先定传播口径，且不得设计诱导分享
3. 接入真实数据后台：当前 `Telemetry` 接口已经把采集与业务隔开，
  换实现即可，界面代码不用动。**但要注意这会打破「零联网」这个卖点**，
   建议做成用户主动选择加入，而不是默认开启
4. 若要做订阅：先验证「一次性需求」还是「高频需求」，这直接决定商业模式
5. 上架前仍需外部输入：**App 备案编号**、开发者主体、反馈邮箱、法务定稿的法律文本
   （这四项在应用内已用警示色标出，填 `AppConfig.kt` 即可）

---

## 9. 应用图标

已替换掉 Android Studio 默认图标，完整设计规范见 **[`design/icon/ICON-SPEC.md`](design/icon/ICON-SPEC.md)**。

**概念**：「一页发票，就是一张图」—— 白色纸张 + 右上折角 = PDF 文档；页内两条文字条 = 原有文字内容；
远山 / 近山 / 太阳 = 转换出来的图片。三笔说清产品，且不放文字、不放箭头。

**结构**：标准自适应图标三层（背景渐变 / 前景图形 / 单色剪影），加 legacy 位图与 Play 上架图。

| 资产 | 位置 |
|---|---|
| 矢量三层 | `res/drawable/ic_launcher_{background,foreground,monochrome}.xml` |
| 自适应入口 | `res/mipmap-anydpi-v26/ic_launcher{,_round}.xml` |
| Legacy 方/圆 | `res/mipmap-{mdpi..xxxhdpi}/ic_launcher{,_round}.webp`（48/72/96/144/192） |
| Play 上架图 | `design/icon/export/ic_launcher_play_512.png`（512×512 满幅直角） |
| 可复现脚本 | `design/icon/generate_icons.py`（含几何自检 + 超采样渲染） |

**关键取舍**：纸张取 Material Keyline 竖向矩形 37×52 dp（贴近 A4 比例），前景最远点 30.58 dp < 33 dp 安全界；
legacy 位图**裁中央 72dp** 生成，保证新旧设备图标观感一致，不会在旧机上突然变小。

**已实测**：系统渲染满铺蒙版无白边；圆形蒙版下纸张四角完整；与系统图标并排视觉权重一致。
桌面上的浅色光环已查明是 Android 16 启动器按图标主色派生的底板（品红对照实验证实），非本图标缺陷。

---

## 10. 真机验证记录

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

### 构建指纹（交付版本 · 2026-09-19 优化后）

`./gradlew :app:clean :app:assembleDebug :app:assembleRelease` → **BUILD SUCCESSFUL**，
88 个任务 / 86 个实际执行（非缓存）。

| 项 | debug（走查包） | release（上架候选） |
|---|---|---|
| 产物 | `app/build/outputs/apk/debug/app-debug.apk` | `app/build/outputs/apk/release/app-release.apk` |
| 体积 | 12,108,617 字节（~11.5 MiB） | **1,301,994 字节（~1.24 MiB）** |
| SHA-256 | `97a6583334d5602487ca2ed510f06e8afde4b17ba753fcbb5a66ea68b647fe1b` | `b1bd495b4a687e240b4c11c964637fd7a2f910247d78fa4a4ca6dd6cfd8a5090` |
| 包名 / 版本 | `com.next.billpic` · versionCode 1 · versionName **1.0.0** | 同左 |
| SDK | minSdk 24（Android 7.0）· targetSdk 36 · compileSdk 36 | 同左 |
| 入口 | `com.next.billpic.MainActivity` | 同左 |
| 权限 | 仅 `WRITE_EXTERNAL_STORAGE (maxSdkVersion=28)`，**无 INTERNET** | 同左（已复核） |
| 签名 | Android Debug 证书 | 回退到 Debug 证书（`keystore.properties` 未配置） |

release 体积比 debug 小 **89%**，主要来自 R8 的 tree-shaking 与资源压缩；
上架前替换正式签名即可。

### 源集隔离验收（本轮新增，判据可复现）

```bash
REL=app/build/outputs/apk/release/app-release.apk
for s in billpic_telemetry validation_panel_open conversion_start \
         flow_complete session_start state_v2 ValidationCalculator; do
  echo "$s: $(unzip -p $REL 'classes*.dex' | grep -ac "$s")"
done
```

| 特征串 | debug | release |
|---|---|---|
| `billpic_telemetry`（持久化名称） | 1 | **0** |
| `validation_panel_open` | 有 | **0** |
| `conversion_start` / `flow_complete` / `session_start` | 有 | **0** |
| `state_v2` | 有 | **0** |
| `ValidationCalculator` | 有 | **0** |

release 包中**搜不到任何埋点痕迹**——这是「上架包不采集」的可复现证据。

### 真机跑出来的 bug（均已修复并复验）

1. **未打分时错误提示只变色、不换文案**
   错误字符串被设置了却从未渲染 —— 现在会正确显示「请先点一下星星打个分」。
2. **键盘弹出后遮住「提交反馈」**
   `enableEdgeToEdge()` 之后 `adjustResize` 不再自动压缩窗口，输入法高度必须自己让出来。
   已在反馈弹层加 `imePadding()`，复验键盘弹出时提交按钮完整可见。

> 这两个问题都是「看代码看不出来、跑起来一眼就看见」的类型 —— 建议团队每次改动后都至少装一次真机过一遍主流程。

### 已知限制

- 测试用的 PDF 是脚本生成的矢量发票（含中文、二维码占位）。真实电子发票的字体内嵌
  差异更大，**上线前请用团队自己的真实发票再走一遍**确认中文清晰度。
- 未做：批量多文件（R-18）、分享回流（R-20），理由见第 8 节。
- 备案编号、开发者主体、反馈邮箱、法律文本均为占位，**未补齐不得上架**。

---

## 11. 本轮优化实施记录（2026-09-19）

来源：《我的页上线就绪度评估与产品优化建议》。共 21 条需求，本轮完成 **19 条**，
2 条明确未做并给出理由。

### P0 · 上线阻塞（9/9）

| 编号 | 需求 | 落点 |
|---|---|---|
| R-01 | 「我的」页身份文案清理 | `MineScreen.kt` 整体重写，4 处自曝文案全部移除 |
| R-02 | 埋点结构性剥离 | 新增 `src/debug`、`src/release` 源集；`build.gradle.kts` 变体配置 |
| R-03 | 正式隐私政策 | `LegalText.kt` + `LegalScreen.kt`（文本版本 `v1.0（产品初稿）`） |
| R-04 | 用户协议 | 同上 |
| R-05 | 备案编号展示位 | `MineScreen.kt` 页脚 + `AboutScreen.kt`，含备案系统链接 |
| R-06 | 清除记录 + 留存披露 | `UserDataStore.clearHistory()`；`PrivacyScreen.kt` 补充文件名留存披露 |
| R-07 | 关于页 | `AboutScreen.kt`；版本号读 `BuildConfig.VERSION_NAME` |
| R-08 | release 签名与混淆 | `signingConfigs` + `keystore.properties.example` + `proguard-rules.pro` |
| R-09 | 真实反馈通道 | `MainViewModel.openFeedbackEmail()`（mailto 自动带设备与版本信息） |

### P1 · 核心链路（7/7）

| 编号 | 需求 | 落点 |
|---|---|---|
| R-10 | 记录卡点击有真实去向 | 最近一次回结果页，否则打开系统相册（`recordTap()`） |
| R-11 | 结果页「换一份发票」 | `BackTitleBar` 新增 `trailing` 动作位 |
| R-12 | 首页上次结果入口措辞 | 改为「上次转换：N 张图片 → 查看结果」 |
| R-13 | 单页分享 | `PageCard` 新增「分享这张」；`shareOne()` |
| R-14 | 页码范围选择 | `PageRange.kt` + `TextInputRow`，容错中文标点 / 全角 / 写反区间 |
| R-15 | 存储权限被拒引导 | `AppConfirmDialog` + 跳转系统应用详情页 |
| R-16 | emoji → 矢量图标 | 新增 10 个矢量 drawable，全部界面 emoji 替换完毕 |

### P2 · 能力与增长（3/5）

| 编号 | 需求 | 状态 |
|---|---|---|
| R-17 | 输出档位改为体积取向 | ✅ `OutputScale` 重构，档位同时控制渲染倍数与 JPEG 质量 |
| R-19 | 记录长按删除 | ✅ `combinedClickable` + 二次确认 |
| R-21 | 诊断信息导出 | ✅ `exportDiagnostics()`，用户主动发起，不违背零联网承诺 |
| R-18 | 批量多文件 | ⏸ **未做**——架构级改动（状态模型 + 结果分组），建议单独一轮 |
| R-20 | 分享回流 | ⏸ **未做**——需先定传播口径，且不得设计诱导分享 |

### 本轮新增功能的走查证据（Android 16 / API 36 模拟器）

| 验证项 | 结果 |
|---|---|
| 冷启动 | 1162 ms |
| 转换（标准档，2 页） | 1190×1682 px · 125 KB · 0.17 s |
| 页码范围 `2` | 「将转换 1 页：2」→ 实际只出第 2 页（1190×1682 · 42 KB · 0.06 s） |
| 档位切「省空间」 | 714×1009 px · **16 KB**（同页体积降到 38%，命中报销平台上传限制） |
| 保存到相册 | `/sdcard/Pictures/BillPic/发票2609-p1.jpg`、`-p2.jpg` 均落盘 |
| 清除转换记录 | 弹层含「其中包含发票文件名」披露 → 确认后记录清空，**相册图片仍在**（边界正确） |
| 长按记录卡 | 「删除这条记录？」二次确认正常 |
| 「我的」页 | 四组结构渲染正常，4 处自曝文案已消失 |
| 关于页 | 「上架前需补齐」警示卡正确列出 3 项未填项；备案编号 + 备案系统链接就位 |
| 键盘避让 | 输入页码时「用当前设置重新转换」位于键盘上方且可点（y=1002 < 键盘顶边） |

### 本轮实现中新发现并修复的两个问题

1. **设置改了却没有出口**：选好文件自动开转（继承原型的「少一步」原则），
   但用户随后改页码范围或输出档位时，没有任何地方能把新设置跑一遍——是个死胡同。
   修复：`MainUiState.settingsDirty` + 「用当前设置重新转换」按钮，仅在设置变化后出现。

2. **键盘遮挡底部按钮**：`enableEdgeToEdge()` 下 `adjustResize` 不再压缩窗口，
   输入页码时键盘盖住了「用当前设置重新转换」。
   修复：内容区加 `imePadding()`。
   与上一轮反馈弹层是同一类问题——**edge-to-edge 下每一处新增的底部交互元素，
   都要重新检查一次键盘避让。**

> 另有一处只在实现时才暴露的坑：`AppConfig.VALIDATION_PANEL` 这类编译期常量，
> 必须判断在**事件入口**（`MainViewModel.track()` 第一行）才能让 R8 折叠整条链路；
> 只把它判断在实现类里，事件名字符串会作为 no-op 调用的参数留在 dex 中。

---

**BillPic Android v1.0.0** · 上架阻塞项已清零，待补齐主体信息与备案编号
