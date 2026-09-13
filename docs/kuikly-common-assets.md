# 知牛 ZhiNiu · Kuikly 公共组件与图标公共资源选型

> 文档版本：v0.1（真实调研，2026-08-22）
> 状态：**基于官方文档与官方规范（kuiklyDSL.mdc）实证**，用于指导前端「多用公共组件、不自绘 SVG」
> 关键背景：Kuikly **不是标准 Compose Multiplatform**，其 Compose DSL 是「对齐 Compose 写法」的自研 DSL。因此「能否直接用 JetBrains Compose 生态组件」需逐一验证，不能想当然。

---

## 1. Kuikly 官方内置组件清单（实证）

来源：Kuikly 官方文档 `kuikly.tds.qq.com/ComposeDSL/allApi.html`（已支持组件列表）+ 官方 AI 规范 `Tencent-TDS/KuiklyUI-AI/rules/kuiklyDSL.mdc` + 腾讯云开发者社区 Kuikly 组件全景。

### 1.1 自研 DSL 内置基础组件

| 组件 | 用途 | 知牛对应场景 |
|---|---|:---|
| `View` | 容器/布局（backgroundImage、glassEffect 等） | 卡片容器 |
| `Text` | 文本 | 股票名/代码/价格/涨跌幅 |
| `RichText` | 富文本 | 财务摘要 |
| `Image` | 图片加载显示 | 头像/banner |
| `Input` / `TextArea` | 单/多行输入框 | 聊天输入框 |
| `Button` | 按钮 | 「AI 看看」「发送」 |
| `Canvas` | 自绘画布 | **K 线降级方案**（若不用 Vico） |
| `Scroller` / `List` / `WaterfallFlow` / `PageList` | 滚动/列表/瀑布流/分页 | 行情长列表、聊天消息流 |
| `Carousel` | 轮播 | 多空辩论气泡轮播 |
| `Video` / `Image` | 媒体 | — |
| `Dialog` / `Picker` / `Switch` / `Slider` / `Tab` / `BottomSheet` | 交互组件 | 选股 Tab、App 内弹窗 |
| **`AI Chat` 内置组件** | 接入大模型对话 | 「问股对话」可直接复用 |

> 体验结论：**行情列表用 `List`（分页/预加载/首屏优化）、聊天用内置 `AI Chat` 或自组 `List`、诊股卡片用 `View+Text` 组合**——全部在 Kuikly 内置能力内，无需自绘 SVG，符合你「多用公共组件」的要求。

### 1.2 Compose DSL 已支持（对齐 Compose 标准 API ~90%）

`allApi.html` 已列：`Text` / `Image` / `BasicTextField` / `TextField` / 布局系统 / 动画 / 手势等。

### 1.3 官方/社区扩展组件（真实）

| 库 | 来源 | 用途 |
|---|---|:---|
| `KuiklyMarkdown` | Kuikly-contrib/KuiklyMarkdown | AI 回答 Markdown 流式渲染 |
| `KuiklyTableView` | lfan-ke/KuiklyTableView（社区） | **表格**（Table/HTable/排序）→ 财务表 |
| `KuiklyBase` | Tencent-TDS | 网络/资源/序列化等基建 |
| KMP 生态库 | Ktor / kotlinx.serialization / SQLDelight | HTTP / JSON / 本地存储 |

---

## 2. K 线渲染：Vico vs Canvas 自绘（确定性决策）

- 技术方案 §4.3 原定「Android/iOS 用 Vico、鸿蒙 Canvas 自绘」。
- **实测风险**：`Vico` 是标准 **Jetpack/Compose Multiplatform** 专用库（`io.github.kamalyes: ...` 或 JetBrains Compose 生态），而 Kuikly 渲染层非标准 Compose。**Vico 与 Kuikly 直接集成的兼容性未经验证**，技术方案风险表也自认存在「渲染层冲突」。
- **建议（推荐，待你 D5 拍板）**：**统一用 Kuikly `Canvas` 组件自绘 K 线**（跨端一致、无集成风险），仅需封装一个 `CandlestickChart` 组合组件。折中方案：先封装 `Canvas` 自绘，末尾若 Vico 可通再替换。

> 这一点直接决定 K 线页的「公共组件 vs 自绘」取舍，是前端第一优先要定的。

---

## 3. 图标公共资源选型（遵循「不自绘 SVG」）

目标：涨跌箭头、搜索、设置、用户、发送、返回、关闭、趋势等图标全部用**现成公共资源**，不手工绘制 SVG。

### 3.1 首选方案：Google Material Symbols（字体图标）

- **原理**：Google Material Symbols 官方干字库/字体（.ttf）打包进工程，用富文本字体渲染，不引入任何 SVG 绘图。
- **优势**：字体渲染与渲染引擎无关（Kuikly `Text/RichText` 都能用），**天然规避 Kuikly 非标准 Compose 的兼容问题**；样式/填充/粗细可变。
- **公共来源**：Google Fonts（fonts.google.com/icons）—— 开源 Apache 2.0 授权，可直接下载 TTF 。

### 3.2 备选方案：Compose Multiplatform 图标库（若 Kuikly DSL 接受 ImageVector）

以下均为真实存在的 CMP 图标库（MIT/Apache）：

| 库 | 坐标 | 说明 |
|---|---|:---|
| compose-icons | `com.composables:icons-material-icons-cmp:2.2.1` 等（含 Material/Lucide/Bootstrap/Tabler…） | 以 ImageVector 直接 `Image(v)` 渲染 |
| compose-material-symbols | `dev.vicart:compose-material-symbols:1.1.5` | Material Symbols 字体封装 |

- **前置验证**：需确认 Kuikly Compose DSL 能否直接消费 `ImageVector` 图标组合。**若不能，则回退 3.1 字体方案**。

### 3.3 决策汇总

| 交付物 | 结论 | 依据 |
|---|---|:---|
| 涨跌箭头/趋势 icon | **Google Material Symbols 字体**（首选，稳健） | 字体渲染跨渲染层通用 |
| 其余 UI 图标（搜索/设置/发送/返回/关闭） | 同上，同一字体 | 无需 SVG |
| logo | AI 生成（GenerateImage）或公共图标准 | 不自绘 |

---

## 4. 待验证/决策（已并入 WORKLOG 跟踪）

1. **D4 已推进**：Kuikly 内置组件清单 + 图表标库备选已核实（本文档）；待你点头用「字体图标 + Kuikly 内置组件」路线。
2. **D5（K 线）**：建议统一 Canvas 自绘，待你拍板。
3. **D8（新增待验证）**：Kuikly Compose DSL 是否直接支持 `ImageVector`——这决定是否可用方案 3.2；**下一步我拉一个最小 Kuikly 工程实测确认**（也可在 PR-01 脚手架阶段实证）。

---

## 5. 参考

- Kuikly 官方已支持组件列表 — https://kuikly.tds.qq.com/ComposeDSL/allApi.html
- Kuikly 跨平台实践（内置组件/组件市场）— https://cloud.tencent.com/developer/article/2725548
- Kuikly 官方开发规范 kuiklyDSL.mdc — https://github.com/Tencent-TDS/KuiklyUI-AI/blob/main/rules/kuiklyDSL.mdc
- KuiklyMarkdown — https://github.com/Kuikly-contrib/KuiklyMarkdown
- KuiklyTableView（社区表格）— https://github.com/lfan-ke/KuiklyTableView
- compose-icons（CMP 图标库索引）— https://composables.com/icons
- compose-material-symbols — https://github.com/ClementVicart/compose-material-symbols
- Google Material Symbols — https://fonts.google.com/icons