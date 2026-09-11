# 知牛 ZhiNiu · 字体规范（Kuikly 官方字体接入）

> 版本：v1.0（2026-09-11）。原则：**不自绘、不私有、按 KuiklyUI 官方字体机制跨端注册**；
> 中文正文使用各端系统 Sans，数字/价格/指标值统一等宽字体，保证列对齐。

---

## 1. 字体选型

| 用途 | 字体 | 来源与授权 |
|:--|:--|:--|
| 中文正文 / UI | 系统 Sans（iOS/macOS: PingFang SC · Android: 思源黑体系 · Windows: Microsoft YaHei · H5: 系统字体栈） | 各端系统内置，零下载 |
| 数字 / 价格 / 涨跌幅 / 指标值 / 代码块 | **JetBrains Mono Regular**（字体族名 `JetBrainsMono-Regular`） | [JetBrains/JetBrainsMono](https://github.com/JetBrains/JetBrainsMono)，SIL Open Font License 1.1（见 `docs/fonts/OFL-JetBrainsMono.txt`），可随项目再分发 |

等宽数字是金融终端的基础要求：涨跌额/涨跌幅列、五档盘口、K 线坐标在等宽字体下才能逐位对齐。

## 2. 官方接入方式（各端）

公共代码里只有一处定义（`pages/components/Format.kt`）：

```kotlin
const val NUM_FONT = "JetBrainsMono-Regular"
// 使用：fontFamily(NUM_FONT)
```

各端按 KuiklyUI 官方指引注册同名字体；**未注册的端自动优雅降级为系统默认字体**，布局不受影响。

| 端 | 官方机制 | 本项目落地 |
|:--|:--|:--|
| **H5** | [docs/DevGuide/h5-custom-font.md](https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/DevGuide/h5-custom-font.md)：`@font-face` 注册 + 字体加载完成后触发重新测量 | `web-host/index.html` 同名 `@font-face`（资源 `assets/common/fonts/JetBrainsMono-Regular.ttf`）；并在 `document.fonts` 就绪（1.5s 兜底超时）后才注入官方渲染宿主 `h5App.js`，保证 Kuikly 首次文本量测即用正确字体度量 |
| **Android** | [IKRFontAdapter](https://github.com/Tencent-TDS/KuiklyUI/blob/main/androidApp/src/main/java/com/tencent/kuikly/android/demo/adapter/KRFontAdapter.kt)（官方 Demo 同款） | `androidApp/.../adapter/KRFontAdapter.kt`：`fontFamily` → `assets/fonts/<名称>.ttf`，回退 shared 模块 `assets/common/fonts/`；在 `KuiklyRenderActivity.initKuiklyAdapter()` 注册 `krFontAdapter` |
| **iOS** | [KRFontModule / 自定义字体 Handler](https://github.com/Tencent-TDS/KuiklyUI/blob/main/iosApp/iosApp/KuiklyRenderExpand/Handlers/KRFontHanlder.m)：按「文件名 = 字体族名」从共享资源目录注册 | `iosApp/Sources/Handlers/KRFontHandler.m`（官方实现，`+load` 自动注册）；字体文件放在 shared 资源 `common/fonts/JetBrainsMono-Regular`（无扩展名，随 framework 资源打包） |
| **鸿蒙** | core-render-ohos `KRFontAdapterManager`（C++ 层通用字体管理） | 未注册时降级系统字体；后续可在 `ohosApp` rawfile 放置同名字体启用 |

字体文件本体（`JetBrainsMono-Regular.ttf`，270KB）存放三份，与各端加载路径一一对应：

```
kuikly-shell/androidApp/src/main/assets/fonts/JetBrainsMono-Regular.ttf   # Android 壳 assets
kuikly-shell/shared/src/commonMain/assets/common/fonts/                   # 共享资源（iOS / H5 构建同步源）
kuikly-shell/shared/src/commonMain/assets/common/fonts/JetBrainsMono-Regular  # iOS 无扩展名副本
web-host/assets/common/fonts/JetBrainsMono-Regular.ttf                    # H5 静态资源（scripts/build.sh 同步）
```

## 3. 排版 Token

字号/字重 token 收敛在 `pages/components/Theme.kt`（`AppTypography` + `AppColors`），
完整视觉规格见 `kuikly-shell/AGENTS.md` §2.4：

| 角色 | size / weight |
|:--|:--|
| Page Title | 24 / Semibold |
| Large Price | 32 / Semibold + `NUM_FONT` |
| Section | 16 / Semibold |
| Body | 14 / Regular |
| Table 数字 | 14 / Medium + `NUM_FONT` |
| Mini number | 13 / Semibold + `NUM_FONT` |
| Caption | 12 / Regular |
| 代码块（AI Markdown） | 12 / Regular + `NUM_FONT` |

规则：

1. **所有数字语义文本必须接 `NUM_FONT`**（`fontFamily(NUM_FONT)`）。
2. 涨幅「+」/ 跌幅「−」用半角符号，等宽下逐位对齐。
3. 字号只允许从 `AppTypography.fs11..fs32` 取值，禁止魔法数。

## 4. Markdown 渲染中的字体

AI 回复的 Markdown 渲染（`pages/components/ai/AiMarkdown.kt`）：

- 段落/标题/列表：系统 Sans（`RichText` + `Span`）；
- 行内 `code` 与围栏代码块：`NUM_FONT` 等宽；
- 表格数值列：继承所属文本样式，数字同样走 `NUM_FONT`。

## 5. 验证

- H5：`document.fonts.check('16px "JetBrainsMono-Regular"') === true`，行情数字计算样式
  `font-family: JetBrainsMono-Regular`（2026-09-11 Playwright 实测通过）。
- 构建链：`scripts/build.sh` 第 1 步会把共享资源字体同步到 `web-host/assets/common/fonts/`。
