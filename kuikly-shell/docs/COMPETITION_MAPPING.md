# 知牛 · COMPETITION_MAPPING

> 赛题逐项映射（✅ 已实现 / ⏳ 真实 API 接入后启用）。

## Task 1 · AI 股票行情原型 Demo

| 验收项 | 状态 | 实现位置 | 截图 |
|---|:---:|---|---|
| 股票名称 | ✅ | `StockRow` 第 1 列 `q.name` | v4f_market_1440_light |
| 股票代码 | ✅ | `StockRow` 第 1 列下行 `fmtSymbol(q.symbol)` | v4f_market_1440_light |
| 最新价 | ✅ | `StockRow` 第 2 列 `fmt2(q.price)` | v4f_market_1440_light |
| 涨跌额 | ✅ | `StockRow` 第 3 列 `fmtChangeSigned(q.change)` | v4f_market_1440_light |
| 涨跌幅 | ✅ | `StockRow` 第 4 列 `fmtPct(q.changePercent)` | v4f_market_1440_light |
| 股票列表可滚动 | ✅ | `List { flex(1f) }` + `vfor` 12 只 | v4f_market_1440_light |
| 点击进入详情 | ✅ | StockRow 整行 `click → openStock → Router.push` | v4f_detail_1440_light |
| 名称 | ✅ | QuoteHeader `q.name` | v4f_detail_1440_light |
| 代码 | ✅ | `fmtSymbol(q.symbol) · marketName` | v4f_detail_1440_light |
| 最新价 | ✅ | 32/Semibold 大数字 | v4f_detail_1440_light |
| 涨跌幅 | ✅ | `fmtPct(q.changePercent)` 红色 | v4f_detail_1440_light |
| 最高价 | ✅ | 8 指标 grid | v4f_detail_1440_light |
| 最低价 | ✅ | 8 指标 grid | v4f_detail_1440_light |
| 成交量 | ✅ | 8 指标 grid | v4f_detail_1440_light |
| 成交额 | ✅ | 8 指标 grid | v4f_detail_1440_light |
| **AI 分析（加分）** | ✅ | 1) Detail 顶 AI分析按钮 → AiInsightPanel 原地替换 Rail；2) 底部 概览/AI解读 Tab | v4f_drawer_1440_light |
| 趋势判断 | ✅ | AiInsightPanel 趋势段 | v4f_drawer_1440_light |
| 风险提醒 | ✅ | AiInsightPanel 风险段（红色色条） | v4f_drawer_1440_light |
| 信号解释 | ✅ | 技术指标段（RSI / MACD） | v4f_drawer_1440_light |
| 指标解读 | ✅ | 趋势/RSI/动量 键值 | v4f_drawer_1440_light |
| 多轮追问 | ✅ | 抽屉内 4 个 FollowUp chip + 输入框 + 圆形发送 | v4f_drawer_1440_light |

## Task 2 · AI 股票问答应用 Demo

| 验收项 | 状态 | 实现位置 | 截图 |
|---|:---:|---|---|
| AI 聊天主页 | ✅ | AiResearchPage 中栏 List | v4f_airesearch_1440_light |
| 输入问题 | ✅ | `AppInput` + onTextChange | v4f_airesearch_1440_light |
| 发送 | ✅ | Composer 圆形 34 IconButton（Enter 也触发 onReturn） | v4f_airesearch_1440_light |
| 展示 AI 回复 | ✅ | 5 类 AiBlock 流式揭示 | v4f_airesearch_card_1440_light |
| 展示会话记录 | ✅ | 左栏 session 列表（4 预设 + 新建 + 时间戳 + 切换） | v4f_airesearch_1440_light |
| **Markdown（加分）** | ✅ | 块结构化（Text / Card / Metrics / Risk / FollowUps） | v4f_airesearch_card_1440_light |
| **股票结构化卡片（加分）** | ✅ | `AiBlock.StockCard` → `AiStockCard`（名称/代码/价格/涨跌/趋势/RSI/查看详情→） | v4f_airesearch_card_1440_light |
| 关键指标 | ✅ | `AiBlock.Metrics`（市盈率/市净率/总市值/量比/RSI14） | v4f_airesearch_card_1440_light |
| 风险块 | ✅ | `AiBlock.Risk`（色条 + 文案） | v4f_airesearch_card_1440_light |
| 简单行情图 | ✅ | 卡内 mini 视觉；右栏 Chart 工具（未启用第三栏但卡内含 sparkline） | v4f_airesearch_card_1440_light |
| 股票详情承接（加分） | ✅ | AiStockCard "查看详情 →" / 卡片整体点击 → `openStock → Router.push` 同一 StockDetailPage | v4f_ai_to_detail_1440_light (= v4f_detail) |
| AI 结构化 | ✅ | 5 类 AiBlock sealed class | v4f_airesearch_card_1440_light |
| 流式 Loading | ✅ | `ActivityIndicatorRow("正在分析…")` + 块逐步追加（180ms 步进） | v4f_airesearch_card_1440_light |

## Engineering

| 验收项 | 状态 |
|---|:---:|
| Kuikly 官方 | ✅ Kuikly 2.25.0-2.1.21（DSL 模式，非 Compose DSL） |
| 公共组件 | ✅ 14 极简组件（AppHeader / AppButton / IconButton / SearchField / MarketPulse / StockTable / StockRow / QuoteHeader / KLineChart / ChartToolbar / AiStockCard / AiInsightPanel / StockSearchOverlay / ThemePopover） |
| Mock Repository | ✅ MarketRepository / AiService 接口 + MockMarketRepository / MockAiService（确定性 Seed） |
| SPA | ✅ H5 `use_spa=1` + KuiklyRouter push/back，同 Tab，浏览器 Back 恢复滚动 |
| Light/Dark | ✅ Light 全部到位；Dark 代码完整（SYSTEM/LIGHT/DARK + animate isDark） |

## 验收截图（v4f）

| 分辨率 | Market | Detail | Drawer | AI Research |
|---|---|---|---|---|
| 1440×900 Light | v4f_market_1440_light | v4f_detail_1440_light | v4f_drawer_1440_light | v4f_airesearch_1440_light |
| 1920×1080 Light | v4f_market_1920_light | — | — | — |
| 1280×800 Light | v4f_market_1280_light | — | — | — |

**AI Research → 查看详情 → StockDetail 流程**：v4f_ai_to_detail_1440_light（与 detail 同 hash，证明跳转成功）。

## 加分项

| 加分项 | 状态 |
|---|:---:|
| K线完整：分时 + 日K + 周K + 月K + MA / MACD / RSI + Crosshair + OHLC Tooltip | ✅ |
| 5 段交互：搜索 / 排序 / 筛选 / 详情 / 抽屉 | ✅ |
| 12 只 A 股（覆盖沪/深/创业/科创板）+ 3 指数 + MarketBreadth | ✅ |
| 跨端同代码（Kotlin Multiplatform commonMain） | ✅ |
| 主题：SYSTEM / LIGHT / DARK（Light 验收全通，Dark 后续） | ✅ / ⏳ |
| H5 SPA：同 Tab + 浏览器 Back + 滚动恢复 | ✅ |
| TDesign 真实图标资源（13 个，MIT） | ✅ |
