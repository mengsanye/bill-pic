# 贡献指南 · 提交规范

本项目的提交信息遵循 **Conventional Commits v1.0.0**。
目标是：**不点开 diff，只看 `git log --oneline` 就能判断这次改动是什么类型、动了哪块。**

---

## 1. 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

| 部分 | 必填 | 说明 |
|---|---|---|
| `type` | ✅ | 改动类型，决定这一行的直观含义 |
| `scope` | 推荐 | 改动范围（模块名），让人知道动了哪块 |
| `subject` | ✅ | 一句话说明改了什么 |
| `body` | 视情况 | 说明「为什么这么改」，可多行 |
| `footer` | 视情况 | 关联 issue / 破坏性变更声明 |

**一行版（最常用）**：

```
feat(convert): support exporting PNG
```

---

## 2. type —— 改动类型（核心）

| type | 含义 | 什么时候用 |
|---|---|---|
| `feat` | 新功能 | 用户能感知到的新能力 |
| `fix` | 修 bug | 修复了错误的行为 |
| `perf` | 性能优化 | 更快 / 更省内存 / 更省电，**行为不变** |
| `refactor` | 重构 | 代码结构变了，**行为不变** |
| `style` | 格式 | 缩进、空格、换行、分号；**不影响代码含义** |
| `docs` | 文档 | 只改了 README / 注释 / 文档 |
| `test` | 测试 | 新增或修改测试 |
| `build` | 构建 | Gradle 脚本、依赖版本、打包配置 |
| `ci` | 持续集成 | CI 配置、自动化脚本 |
| `chore` | 杂项 | 其他不涉及 src 的琐事（如 .gitignore） |
| `revert` | 回滚 | 撤销之前某次提交 |

### 类型怎么选（判断口诀）

依次问自己：

1. **用户多了个新能力？** → `feat`
2. **修好了一个坏行为？** → `fix`
3. **行为没变，只是变快了？** → `perf`
4. **行为没变，只是结构变了？** → `refactor`
5. **连代码含义都没变（纯排版）？** → `style`
6. **一行 src 都没碰，只动了文档？** → `docs`
7. **只动了构建参数 / 依赖版本？** → `build`

> **如果选不出来，说明这次提交混了多件事 —— 拆成多个提交。**

---

## 3. scope —— 改动范围

用**模块名**，不要用文件名。本项目可用：

| scope | 对应范围 |
|---|---|
| `convert` | 转换主流程（选文件 → 转换 → 结果页） |
| `pdf` | PDF 渲染引擎（`PdfConverter`、尺寸与内存守卫） |
| `save` | 保存到相册 / 分享（`MediaSaver`、FileProvider） |
| `records` | 记录页 |
| `mine` | 我的页 |
| `privacy` | 隐私说明与数据控制（清除记录） |
| `theme` | 设计令牌与主题（Color / Type / Theme） |
| `ui` | 通用 UI 组件 |
| `icon` | 启动图标（自适应前景 / 背景 / 单色层） |
| `deps` | 依赖升级 |
| `config` | 应用配置（`AppConfig`、构建脚本参数） |

`scope` 可以省略（如 `docs: ...`），但**能写就写** —— 它是一行信息量最大的部分。

---

## 4. subject —— 一句话说明

- **英文**，祈使句（用 `add` 不用 `added` / `adds`）
- **首字母小写**，**结尾不加句号**
- 控制在 **50 字符以内**
- 说清「做了什么」，不写「修复了一些问题」这种空话

```
✅ fix(save): write to MediaStore with IS_PENDING on API 29+
❌ fix: 修复了一些问题
❌ fix(save): Fixed the bug that was causing issues.
```

---

## 5. body —— 为什么这么改

`-` 开头列点，每行不超过 72 字符。**写「为什么」，不写「怎么做」**（怎么做看 diff 就行）。

```
fix(pdf): copy SAF uri to cacheDir before rendering

PdfRenderer requires a seekable file descriptor, but some document
providers only expose a one-shot stream. Staging the file into
cacheDir guarantees a seekable fd regardless of provider.

- keeps the privacy promise (no upload, file stays on device)
- adds PdfConversionException.Encrypted for password-protected pdfs
```

---

## 6. 破坏性变更

在 type/scope 后加 `!`，并在 footer 写 `BREAKING CHANGE:`：

```
refactor(config)!: rename OutputScale.ULTRA to OutputScale.MAX

BREAKING CHANGE: `OutputScale.ULTRA` no longer exists. Use
`OutputScale.MAX`. Persisted records referencing the old name are
migrated on first launch.
```

---

## 7. 关联 issue / 事项

放在 footer，每行一条：

```
feat(convert): support batch page export

Closes #12
Refs #8
```

---

## 8. 提交粒度

**一个提交只做一件事。** 满足以下三条才算合格：

- [ ] 只有一个明确的 `type` 能描述它
- [ ] 能编译（不留下编译不过的中间态）
- [ ] 回滚这一个提交不会牵连别的功能

> ⚠️ **首次导入例外**：项目初始导入是一次性动作，拆成多个提交反而会产生大量编译不过的中间态，
> 因此**允许合并为单个 `feat:` 初始提交**，并在 body 里列清包含范围。

---

## 9. 模板已就位

仓库根目录的 `.gitmessage` 已配置为提交模板，执行 `git commit` 时**会自动带出**填写提示与类型清单：

```bash
git commit          # 裸执行 git commit，模板自动出现
```

若模板没出现（比如换了一台机器），手动启用一次：

```bash
git config commit.template .gitmessage
```

---

## 10. 本项目示例

```
feat(convert): add PNG output format
feat(convert): stop auto-converting when the pdf has more than one page
feat(privacy): add a clear-records entry to the mine screen
fix(save): use IS_PENDING so gallery write works on Android 10
fix(ui): show error text instead of only recoloring the label
perf(pdf): reuse a single Bitmap across pages
refactor(pdf): extract the render-size calculation
docs: document why INTERNET permission is absent
build(config): enable R8 for the release variant
chore: ignore .idea directory
```
