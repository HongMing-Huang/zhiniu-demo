# 知牛 · ZhiNiu 行情 AI Demo

> 腾讯 Kuikly × 自选 AI Coding Skill Demo · **知牛 ZhiNiu** · Web H5（Kuikly DSL · 多端可编译）

**核心演示路径**：

```
市场 → 搜索/点击「贵州茅台」→ StockDetail（日K / 周K / RSI）→ AI分析 Drawer
AI研究 → 问「分析贵州茅台」→ 结构化股票卡 + 关键指标 + 追问 → 点击「查看详情」→ StockDetail
```

***

## 1. 项目简介

知牛 ZhiNiu 是一款面向 A 股个人投资者的「金融研究工具」原型 Demo。围绕"克制、高信息密度、金融工具"的设计语言，把行情列表、K线分析、AI 解读三条主线收进同一条 SPA 路径，并用统一的 12 极简组件 + 8 个核心领域模型 + 2 个 Mock 仓储把它串起来。本阶段全部使用稳定 Mock 数据，UI 层零感知 Mock/Real 差异，真实 API 仅替换 `MarketStore.repository` / `MarketStore.aiService` 两个对象。

## 2. 赛题范围

| Task                  | 验收项                                                    | 状态 |
| --------------------- | ------------------------------------------------------ | -- |
| **Task 1 · 行情列表**     | 股票名称/代码/最新价/涨跌额/涨跌幅/可滚动/进入详情                           | ✅  |
| **Task 1 · 个股详情**     | 名称/代码/最新价/涨跌幅/最高/最低/成交量/成交额                            | ✅  |
| **Task 1 加分 · AI 分析** | 趋势判断/风险提醒/信号解释/指标解读/多轮追问                               | ✅  |
| **Task 2 · 聊天主页**     | 输入/发送/会话记录/回复                                          | ✅  |
| **Task 2 加分 · 结构化回复** | Markdown / 股票卡片 / 关键指标 / 风险块                           | ✅  |
| **Task 2 加分 · 详情承接**  | 卡片点击进入同一 StockDetailPage                               | ✅  |
| **工程**                | Kuikly DSL · 公共组件 · Mock Repository · SPA · Light/Dark | ✅  |

## 3. Kuikly 版本与模式

- **Kuikly**：`2.25.0-2.1.21`（Kotlin 2.1.21）

- **DSL 模式**：**Kuikly DSL**（指令式 `attr{}` / `vfor` / `vif`），**非** Compose DSL

- **官方 AI 规则**：已放置 `.cursor/rules/kuiklyDSL.mdc`（来自 `Tencent-TDS/KuiklyUI-AI`）

## 4. 如何启动

### 4.1 环境要求

- JDK 17（项目工具链：`/Users/c14h14n3/Desktop/demo/zhiniu/.toolchains/jdk-17.0.20.1+1/Contents/Home`）

- 系统默认 Java 25 与 Kotlin 内置 Node 22.0.0 不兼容；构建时需 `env -u NODE_OPTIONS`

- H5 入口 Chrome（Playwright 自带 chromium）

### 4.2 一键开发

```bash
./scripts/build.sh          # 编译 + 部署 nativevue2.js → web-8083
(cd web-8083 && python3 -m http.server 8083 &)
(cd web-host && python3 -m http.server 8082 &)
open http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1
```

### 4.3 H5 SPA 路由

- 启用方式：URL `?use_spa=1`（host 端 `h5App.js` 内 `ENABLE_BY_DEFAULT=false`，但支持 `use_spa=1` 激活）

- 导航：`openStockDetail(symbol)` → `push`，`closeCurrentPage()` → 浏览器 Back

- 缓存：返回 Market 恢复滚动位置（KuiklyRouter 自动）

## 5. Mock 数据说明

确定性 Seed，**刷新/录屏结果一致**：

- **股票池**：12 只 A 股覆盖沪市/深市/创业板/科创板（贵州茅台 600519.SH、宁德时代 300750.SZ、平安银行 000001.SZ 等）

- **指数**：上证指数 +0.39% / 深证成指 +0.58% / 创业板指 +0.91%

- **K 线**：每只股票 120 根日 K + 48 点分时 + 52 周 K + 36 月 K；末根 close 对齐报价，high≥max(o,c)/low≤min(o,c)

- **AI**：确定性回答覆盖 RSI / 量能 / 支撑压力 / 综合判断

## 6. 核心演示路径

1. **市场**（[http://...MarketList](http://127.0.0.1:8082/?page_name=MarketList\&use_spa=1)）→ 看见 12 只股票 + Market Pulse（指数+宽度）
2. **搜索**：点击 Header 搜索框 → `StockSearchOverlay` 浮层（最近/热门/搜索结果）→ 输入"茅台"或"600519"→ 点选 → SPA push 进入 **StockDetail**
3. **K线交互**：切换 日K/周K/月K/分时；切换 MA/MACD/RSI；十字光标 + Tooltip OHLC
4. **AI分析**：点 Detail 顶部"AI 分析" → 右侧滑入 `AiInsightDrawer`（趋势/量能/技术信号/风险 + 追问按钮 + 输入框）
5. **AI研究**：Header 切到 AI研究 → 三栏（会话 / 聊天 / 当前研究对象）→ 问"分析贵州茅台" → 流式出现 Text + **AiStockCard** + Metrics + Risk + FollowUps → 点卡片"查看详情"→ 进入同一 StockDetail

## 7. 目录结构

```
zhiniu/
├── kuikly-shell/                       # Kuikly H5 主项目
│   ├── AGENTS.md                       # Agent 协作规范
│   ├── README.md
│   ├── docs/UI_SPEC.md                 # 视觉/组件规范
│   ├── docs/ARCHITECTURE.md            # 架构 & 替换点
│   ├── docs/COMPETITION_CHECKLIST.md   # 赛题逐项映射
│   ├── .cursor/rules/kuiklyDSL.mdc     # Kuikly 官方 AI 规则
│   ├── shared/src/commonMain/kotlin/com/zhiniu/
│   │   ├── pages/                      # 3 个 @Page + AppBasePage
│   │   ├── pages/components/           # 12 公共组件
│   │   ├── domain/model/               # 8 个领域模型
│   │   ├── domain/repository/          # 2 个接口
│   │   ├── data/mock/                  # 2 个 Mock 实现 + MarketStore
│   │   └── ...
│   └── shared/src/commonTest/          # 纯逻辑测试
├── web-host/index.html + h5App.js + assets/common/icons/  # 宿主 H5 + TDesign 图标
├── web-8083/nativevue2.js              # 业务 JS bundle（构建产物）
├── scripts/
│   ├── build.sh           # :shared:jsBrowserProductionWebpack → web-8083
│   ├── icons_build.sh     # TDesign 官方 svg/ → commonMain assets
│   ├── icons_render.js    # SVG → 20px PNG（DPR4）
│   └── dev.sh             # 后端 + 8083 + 8082
└── upstream-kuiklyui/                  # Kuikly 官方源码（参考）
```

## 8. 已知限制

- **仅 H5 端构建/验证通过**（Android / iOS / 鸿蒙编译产物保留；本阶段未跑端到端）。

- **AI / 行情全部为 Mock**：`MarketStore.repository` 与 `MarketStore.aiService` 真实接入时只换对象，UI 层零改动；接入后要保证：① stockQuotes 同步返回；② candles(symbol, Timeframe) 同 timeframe 必须有完整 K 线；③ spark(symbol) 与搜索 name/pinyin/code/suffix。

- **跨端能力限制**：H5 SPA 通过 `use_spa=1` 激活；原生端走 Native Router（同接口），行为与 SPA 一致。

- **后端**：未启 LLM 网关；所有 AI 答复为 Mock 确定性文本，标注"Demo 行情与 AI 演示输出，不构成投资建议"。

