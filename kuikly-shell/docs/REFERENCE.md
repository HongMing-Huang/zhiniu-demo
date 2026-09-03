# 知牛 · REFERENCE

> 视觉与工程参考（真实参考内容 + 边界）。

---

## OKX Markets / Prices
**参考**：顶部 Navigation 密度、Macro（指数）信息区、Category Tabs、Filter、Table 布局、黑白灰 Surface 系统。
**不参考**：Crypto 业务字段、品牌、交易入口、Logo。

## TradingView Stock Screener
**参考**：Filter + Sort 工具栏、Column hierarchy、Sortable header、Filter → Asset Detail 流程。
**不参考**：Pine Script、复杂多条件筛选器、Drawer-based 高级筛选。

## moomoo / 富途 Detailed Quotes
**参考**：Stock Header（名称+代码+市场标签 + 大价格+涨跌 + 8 指标 grid）、Chart 为主视觉、Timeframe + Main/Sub indicator 切换、Context Rail（Key Data + AI Quick）。
**不参考**：Trade 入口、Drawer-based advanced 工具、报价档口。

## Webull Charts
**参考**：Chart workspace 布局、Candlestick + Volume、Crosshair + Tooltip、Zoom/Pan 工作流。
**不参考**：Options/Level 2/Trade 工具栏。

## TDesign（Tencent Design System）
**参考**：Color（中性黑白灰 Surface 体系）、Typography（系统 Sans + 数值字面）、Motion（100-200ms easeOut）、Dark Mode（分层 Surface Raised）、Divider 层级。
**不参考**：React/Vue TDesign 运行时（仅取视觉规范，Kuikly 自实现）。
**未引入**：TDesign 组件库 / Tailwind / shadcn / Material UI / 任何 React 生态。

## Kuikly 官方资源（工程依据）
- **规则**：`Tencent-TDS/KuiklyUI-AI` → `kuiklyDSL.mdc`（已放置 `.cursor/rules/`）
- **源码**：`Tencent-TDS/KuiklyUI`（本仓库 `upstream-kuiklyui/`） — 所有 API 必须可在其中找到
- **文档**：`docs/DevGuide/`（flexbox / animation / h5-spa / router / ...）
- **Demo**：`upstream-kuiklyui/h5App/`（H5 Router / KuiklyRenderer 集成参考）
- **图标**：`Tencent/tdesign-icons`（develop 分支 `svg/` 目录，真实文件名经 API 核验后落地为 PNG）

## 图标（Reference Section）

**Icon Source**: Tencent TDesign Icons  
**License**: MIT  
**Repository**: https://github.com/Tencent/tdesign-icons (branch: `develop`, path: `svg/`)  
**本地路径**: `shared/src/commonMain/assets/common/icons/*.png`  
**Web 路径**: `web-host/assets/common/icons/*.png`（H5 ImageProcessor: `assets://common/...` → `assets/common/...`）  
**加载方式**: Kuikly `Image` + 本地 PNG，tintColor 按主题染色（`textPrimary/secondary/tertiary`）

**已落地的图标（官方 svg 目录真实文件名）**：
| 用途 | 官方文件 | 落地产物 |
|---|---|---|
| search | `ai-search.svg` | `icons/search.png` |
| back | `arrow-left.svg` | `icons/back.png` |
| 自选 | `collection.svg` / `collection-filled.svg` | `icons/star.png` / `icons/star_filled.png`（官方无 `star.svg`，取官方"收藏"语义） |
| theme | `brightness.svg` | `icons/theme.png`（官方无 `mode-light/dark`，取亮度图标） |
| close | `close.svg` | `icons/close.png` |
| send | `arrow-up.svg` | `icons/send.png`（composer 圆形 34 发送） |
| ai | `ai-1.svg` | `icons/ai.png` |
| chart | `chart-line.svg` | `icons/chart.png` |
| more | `ellipsis.svg` | `icons/more.png` |
| filter | `filter.svg` | `icons/filter.png` |
| chevron | `chevron-down.svg` / `chevron-up.svg` | `icons/chevron_down.png` / `icons/chevron_up.png` |

> 所有图标名经 `https://api.github.com/repos/Tencent/tdesign-icons/contents/svg` 实测确认存在于官方 develop 分支（svg/ 目录共 1000 个 SVG）。**不猜名**。
