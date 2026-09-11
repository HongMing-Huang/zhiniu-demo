# 知牛 · ZhiNiu · AGENTS.md

> 这是 AI 编码工具的入口规范：所有 Agent 在修改 `kuikly-shell/` 前**必须**读完本文件 + `.cursor/rules/kuiklyDSL.mdc`。
> 官方 Kuikly 资料：<https://github.com/Tencent-TDS/KuiklyUI> · <https://github.com/Tencent-TDS/KuiklyUI-AI>

---

## 0. 工作日志（每次改动必记，防止记忆混乱）

- **唯一记录文件**：`docs/WORKLOG.md`（仓库根 `zhiniu/docs/WORKLOG.md`）。所有 Agent 每次实质改动后**必须**在顶部追加一行。
- 行格式：`YYYY-MM-DD HH:mm | 类别 | 改动描述 | 验证结果`；类别 ∈ {ui / build / data / agent / docs / design / git / chore}。
- 禁止重复写研究/设计文档（已有 `docs/ui-redesign.md` / `AGENTS.md` / 各设计文档），只做增量记录。
- 每轮结束时用 `git status` 核对，确保 WORKLOG 与本轮提交一致。

## 1. 项目性质

- **赛题**：腾讯 Kuikly × 自选 AI Coding Skill Demo（Web H5 优先，H5 启动：<http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1>）
- **GitHub**：`HongMing-Huang/zhiniu-demo`（private，origin=main，2026-09-04 建立）；代码管理一律走该仓库
- **技术栈**：Kuikly DSL（**非** Compose DSL）、Kotlin 2.1.21、**Kuikly 2.25.0-2.1.21**、Multiplatform commonMain + jsMain、H5 SPA via `use_spa=1`
- **当前阶段**：真实行情/K 线/个股资讯/板块/选股/人气榜/市值已通过 FastAPI 网关接入（新浪 + 东财 + 腾讯双来源，离线快照兜底）；研究 Agent 为 TradingAgents 结构的多空/风控辩论管线（`backend/app/ta_agents.py`），前端接类型化 SSE 研究流；**真实模型仍待有效 LLM Key**（当前无 Key → 显式 rule-engine 降级）

## 2. 赛题硬约束（不可违反）

### 2.1 页面范围（仅 3 + 3 浮层）
```
页面：MarketPage / StockDetailPage / AiResearchPage
浮层：StockSearchOverlay / AiInsightDrawer / ThemePopover
```
禁止：排行、新闻、资金、财务、设置、策略中心、投资组合、复杂选股器等独立页。
允许（课题 Task01 明确要求，均为 MarketPage 页内状态，不是新页面/新浮层）：「人气榜 / 涨幅榜」作为行情 Tab 的列表模式切换（`RankTable`）；「字段」内联芯片行做自定义显示列（`StockColumns`）。

### 2.2 视觉气质
- 关键词：克制 / 清晰 / 冷静 / 高信息密度 / 金融 / 工具 / 准确
- 禁：传统 Admin Dashboard 巨型 Card、ChatGPT 首页、蓝紫渐变、毛玻璃、霓虹发光、过量 Shadow、Emoji
- 图表颜色：仅 A 股红涨 #F04F5F / 绿跌 #16B364；UI 自身保持黑白灰；AI 强调仅在 AI 区域用 #D9FF43（前景深色 #0B0D0F），MA/RSI 等指标线用橙/蓝/紫区分（不与 AI 强调同色系）

### 2.3 主题
- Light：Page #F6F7F9 · Surface #FFFFFF · SurfaceSubtle #F2F4F6 · Hover #EEF1F4 · Border #E4E7EB · BorderStrong #D5D9DE · TextPrimary #15181C · TextSecondary #68717C · TextMuted #969FA9
- Dark：Page #0E1013 · Surface #15181C · SurfaceSubtle #1A1E23 · Hover #20252B · Border #292E35 · BorderStrong #343A42 · TextPrimary #F2F3F4 · TextSecondary #9CA4AE · TextMuted #68717B
- 主题：SYSTEM（跟随系统） / LIGHT / DARK；切换 180ms 不刷新页面

### 2.4 排版（系统 Sans + 官方接入的等宽数字字体）
- 中文正文：各端系统 Sans；**数字/价格/指标值/代码块一律 `fontFamily(NUM_FONT)`**（`JetBrainsMono-Regular`，按 KuiklyUI 官方机制三端注册，见 `docs/typography.md`；未注册端优雅降级）。
| 角色 | size / weight |
|---|---|
| Page Title | 24 / Semibold |
| Stock Name | 20 / Semibold |
| Large Price | 32 / Semibold + NUM_FONT |
| Section | 16 / Semibold |
| Body | 14 / Regular |
| Table | 14 / Medium + NUM_FONT |
| Caption | 12 / Regular |
| Mini number | 13 / Semibold + NUM_FONT |
| Stat | 18 / Semibold + NUM_FONT |

### 2.5 间距 / 尺寸
- Header 64 · 内容最大 1360 · 水平留白 32（compact 16）· 区块间距 28 · 面板间距 16 · Radius 8
- Button 高 34 / 38 · Input 高 38 · Table 行 60
- 1920 居中 · 1440 完整 · 1280 不溢出

## 3. Kuikly API 约束（硬性）

**任何 Kuikly API 必须能在这五处之一找到**：① 当前 SDK · ② KuiklyUI 官方源码 · ③ 官方 Documentation · ④ 官方 Demo · ⑤ KuiklyUI-AI Rule / Skill。
**禁止创造**：`Modifier.xxx` / `attr.xxx` / `event.xxx` / 不存在的 Icon / 不存在的 Router API / 自造 Compose DSL 调用。

### 3.1 DSL 关键规则（来自 `kuiklyDSL.mdc`）
- `attr {}` 设置样式 · `event {}` 设置事件 · `ref {}` 设置组件引用
- 响应式：`observable` / `observableList<T>`（绑定到当前 Pager，跨页不同步）
- 指令：`vif` / `velseif` / `velse` / `vfor` / `vforIndex` / `vforLazy` / `vbind`
- **`vfor/vforLazy` 闭包必须且仅有一个根 View**（复杂结构用 View 包裹）
- 叶子组件（Text/Input）不支持 padding，用 margin 或外层 View padding
- 扩展函数必须 `import` 后用**非限定名**调用（全限定 `com.tencent.kuikly.core.views.X` 调扩展会 Unresolved）

### 3.2 页面基类
- 继承 `com.zhiniu.pages.AppBasePage`（已封装 Header、StockSearchOverlay、ThemePopover、contentWidth/isCompact/isMedium/isNarrow/overlayLeft）
- 在 `body()` 末尾用 `renderCommonOverlays(this, "市场" | "AI研究")` 渲染公共浮层
- 跨页跳转用 `openMarketPage()` / `openAiResearchPage()` / `openStockDetail(symbol)` / `closeCurrentPage()`（H5 SPA 下被劫持为 push/back，浏览器 Back 自动恢复滚动）

### 3.3 K线
- **仅**用 Kuikly Canvas（`com.tencent.kuikly.core.views.Canvas` + `CanvasContext`）
- 禁：TradingView Lightweight Charts / ECharts / Highcharts / 任何 JS Chart 库
- 坐标换算统一在 `yOf(v)` / `xOf(i)`；`drawKLineChart(context, w, h, bars, colors, indicator, crossX, crossY, intraday)` 全部可视化

## 4. 数据层（Repository 模式）
- 接口：`MarketRepository` / `AiService`（见 `domain/repository/`）
- Mock：`data/mock/MockMarketRepository` / `MockAiService`（确定性 Seed，录屏稳定）
- 入口：`data/mock/MarketStore.repository` / `.aiService`（**唯一替换点**）
- 真实 API：`data/remote/GatewayMarketClient` 统一调用本地 FastAPI；页面只在真实结果成功时覆盖快照。不得从 H5 直接调用需要 Referer/GBK/JSONP 的上游。网关地址默认 `http://127.0.0.1:8000`，可用 URL/路由参数 `gateway=http://<电脑IP>:8000` 覆盖（`AppBasePage.created()` 读取，真机联调用）。
- 行情补充字段：`/quote/realtime` 已带 `marketCap / turnoverRate / volumeRatio`（东财 ulist → 腾讯 gtimg 双来源，缺失为 null → UI 显示「—」）；`/quote/popularity`（东财人气榜）、`/quote/sectors`（东财行业板块）、`/quote/screener`（东财成交额榜池）均为真实数据 + `isStale` 显式降级。详情页估值用 `factsOf(quote, fundamentals)`，禁止再用哈希伪造演示值。
- AI 研究：首选 `GatewayMarketClient.researchStream`（`/agent/research/stream` 类型化 SSE，阶段帧驱动进度），流不可用回退 `research()`；页面必须展示来源、时效和风险，不得向 UI 暴露私有思维链。研究结果 `synthesis` 含 `rating（五档）/trend/pressure/support/bullPoints/bearPoints/riskNotes/trader`，UI 以卡片呈现（课题：趋势/压力位/支撑位/投资建议）。
- Agent 管线：`backend/app/ta_agents.py` 为 TradingAgents（TauricResearch，MIT）多 Agent 结构的移植适配（多头/空头研究员 → 研究经理五档评级 → 交易员绝对价位 → 激进/中性/保守风控），经 `gateway.chat_completions` 降级链调用，不引入 LangGraph；压力/支撑由 `agent._levels_from_kline` 确定性计算，模型不得编造价位；无模型/超时显式 `mode=deterministic_fallback, provider=rule-engine`。
- 后端数据源：`backend/app/data_sources.py` 负责外部资讯归一化、来源字段、TTL/stale；Agent 工具必须复用同一行情/资讯函数，禁止再造一套假数据。出站请求必须经 `quote._guard_external_url` 白名单（HTTPS + 已知行情域名），新增上游先加白名单。

## 5. 命名规范
- 只用业务名：`selectedStock` / `selectedTimeframe` / `searchQuery` / `marketQuotes` / `klineData` / `isSearchVisible` / `isAiDrawerVisible` / `chatSessions` / `currentSession`
- 禁：`data2` / `tmp` / `flag1` / `view1` / `box` / `btn1` / `newData` / `aaa` / `flag` / `btn` / `page1`
- Class 与文件同名：MarketPage / StockDetailPage / AiResearchPage / StockRow / KLineChart / MockMarketRepository
- 模型：`StockQuote` / `MarketIndex` / `MarketBreadth` / `Candle` / `TechnicalIndicator` / `AiInsight` / `ChatMessage` / `ChatSession`

## 6. 注释规范
只解释非显然逻辑：K线坐标换算、timeframe 聚合、MA/MACD/RSI 计算、H5 SPA 特殊处理、Kuikly 跨端 API 限制、Mock→Real 替换点、AI Streaming 状态机、非显然性能优化。禁止 `// 创建View` `// 设置背景` 这类废话。

## 7. 图标（Tencent TDesign · 本地 PNG · 中性灰预着色）

- **资源源**：`Tencent/tdesign-icons`（develop 分支 `svg/` 目录，文件名经 GitHub Contents API 实测核验；svgs 共 1000 个）
- **本地路径**：`shared/src/commonMain/assets/common/icons/*.png`（native bundle）+ `web-host/assets/common/icons/*.png`（H5 `assets://common/...` → `assets/common/...`）
- **加载**：Kuikly `Image` + `tintColor`（H5 用 SVG filter 实现 / 原生用 ImageView tint）
- **禁**：Emoji、Unicode 符号、自绘 Canvas 图标、第三方 Icon 字体、运行时 TDesign 组件
- **落地 20 个**（真实文件名映射，详见 `docs/REFERENCE.md`）：含 search/back/star/star-filled/theme/close/send/ai/chart/more/filter、chevron 三向、calendar/check/filter-sort/error/chat/data
- **新增图标**：编辑 `scripts/icons_build.sh` 加 `TDesign 官方文件名` → 跑 `scripts/icons_build.sh && node icons_render.js`

## 8. 动画（只允许）
- Button Press 100–120ms 背景 · StockRow Hover 120ms · Tab 160ms · SearchOverlay 160ms opacity+translateY · AIDrawer 200ms translateX+opacity · Theme 180ms backgroundColor · K线周期切换 100ms crossfade
- 禁：Spring · Bounce · 3D · Glow · 无限装饰动画

## 9. 验证流程（每次大改后必跑）
```bash
# 1) 编译
cd kuikly-shell && env -u NODE_OPTIONS JAVA_HOME=/Users/c14h14n3/Desktop/demo/zhiniu/.toolchains/jdk-17.0.20.1+1/Contents/Home \
  ./gradlew :shared:compileKotlinJs

# 2) 完整构建 + 部署
cd .. && ./scripts/build.sh

# 3) 启动服务（H5 单端口主链路：bundle 同源 web-host/，8083 仅兼容旧链）
(cd web-host && python3 -m http.server 8082 &)

# 4) 浏览器验收：打开
#    http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1
#    检查 MarketPage / StockDetailPage / AiResearchPage 三屏 + 搜索浮层 + 设置面板 + 行跳转
#    注意：index.html 必须保持「nativevue2.js → kuiklyBundlesReady → h5App.js」同源顺序，
#    缺 ready 置位或跨端口引用会导致白屏/乱码（官方 h5App 宿主依赖该标志启动）。
#    修订部署链路必须重跑 scripts/build.sh（会把 bundle 拷贝进 web-host/）。
```
四分辨率验收：1440×900 Light/Dark · 1920×1080 Light · 1280×800 Light（浏览器 DevTools 设备模拟）。

## 9.1 Git 提交规范
- 提交前先 `git status` + 更新 `docs/WORKLOG.md`（§0）。
- Commit message：单行标题 ≤ 72 字符，`type(scope): 描述`（type ∈ feat/fix/docs/design/build/chore/refactor）。
- 每轮只提交有验证结果的改动；禁止把"未编译/未跑"的代码入库。
- 推送：`git push origin main`（仓库 `HongMing-Huang/zhiniu-demo`）。

## 10. 禁止事项
- 一次性重写整个项目（按 Phase 0→1→2→3→4→5 推进，每阶段运行 + 截图检查）
- 用 `rm -rf` / `del /S /Q` 处理 Desktop / Downloads / 任何用户数据
- 引入未在 Kuikly 官方文档出现的第三方 UI 库
- 重复创建同名组件（已有：AppButton/IconButton/SearchField/StockTable/StockRow/QuoteHeader/MetricRow/KLineChart/ChartToolbar/AiStockCard/AiInsightDrawer）
- 创建中间件、复杂依赖注入、DI 容器（页面级 direct access 即可）
- TODO / FIXME 注释、未使用变量、空 catch

## 11. 文件结构
```
kuikly-shell/
├── .cursor/rules/kuiklyDSL.mdc          # Kuikly 官方 AI 规则（必备）
├── shared/src/
│   ├── commonMain/kotlin/com/zhiniu/
│   │   ├── pages/
│   │   │   ├── AppBasePage.kt          # 公共基类（Header + 浮层 + 响应式几何）
│   │   │   ├── MarketPage.kt
│   │   │   ├── StockDetailPage.kt
│   │   │   └── AiResearchPage.kt
│   │   ├── pages/components/
│   │   │   ├── Theme.kt / Format.kt（含 NUM_FONT 单一来源）/ Icons.kt / StockFacts.kt
│   │   │   ├── AiInsightDrawer.kt
│   │   │   ├── common/                 # AppCard / Divider / Buttons / Inputs / Tabs / Header / SearchOverlay / ThemePopover / Base
│   │   │   ├── market/                # MarketPulse / StockRow
│   │   │   ├── chart/                 # KLine / ChartToolbar
│   │   │   └── ai/                    # AiBlocks（AiStockCard / Metrics / Risk / FollowUps）· AiMarkdown（Markdown 解析+渲染，commonTest 覆盖）
│   │   ├── domain/
│   │   │   ├── model/                 # StockQuote / Candle / MarketIndex / MarketBreadth / TechnicalIndicator / AiInsight / ChatMessage / ChatSession / Timeframe
│   │   │   └── repository/            # MarketRepository / AiService
│   │   ├── data/
│   │   │   ├── mock/                  # MockMarketRepository / MockAiService / MarketStore
│   │   │   ├── remote/                # GatewayMarketClient（quote/kline/news）
│   │   │   └── local/                 # Watchlist
│   │   ├── platform/                  # SystemTheme (expect/actual) / HttpClientPlatform
│   │   └── base/                      # BasePager / PageNavigator / IPagerIdKtx / Utils / BridgeModule
│   └── commonTest/kotlin/com/zhiniu/  # KLineChartTest / SseParserTest / AppErrorTest
├── docs/                               # UI_SPEC / REFERENCE / COMPETITION_MAPPING
├── README.md
└── AGENTS.md
```

## 12. 重要技术细节
- **NODE_OPTIONS 绕过**：本机 NODE_OPTIONS 含 `--use-system-ca` 触发 WorkBuddy shim，Kotlin 内置 Node 22.0.0 不支持，webpack 会 `KotlinNothingValueException` 假错误。**始终** `env -u NODE_OPTIONS` 跑 gradle。
- **JDK**：必须用 `/Users/c14h14n3/Desktop/demo/zhiniu/.toolchains/jdk-17.0.20.1+1/Contents/Home`（系统默认 25 太新）。
- **KSP 页注册**：`<Page("name", supportInLocal = true)>` 标注类，KSP 编译时生成页表；运行时通过 `page_name=xxx` URL 参数寻址。
- **响应式状态作用域**：observable 字段仅驱动当前 Pager；页面间状态需通过 `pageData.params` 或单例传递。
- **安全区**：使用官方 `pageData.safeAreaInsets`；Header/浮层叠加 `top`，滚动页尾与 AI Composer 叠加 `bottom`，禁止写死刘海屏/手势区高度。
- **窄屏**：`<=760px` 必须切换 compact 结构；禁止仅缩小桌面两栏。行情表隐藏次要列，详情 K 线/盘口上下排列，AI 会话侧栏收起但保留新建入口。
- **可访问性**：所有按钮、导航、Tab、可点击行情行使用 SDK 官方 `accessibility`、`accessibilityRole`、`accessibilityInfo`；图标按钮必须提供中文语义标签，静态状态行不得伪装成可点击按钮。
- **已知 mock 与真实 API 行为差异**：当前 Mock 12 只 A 股 + 3 指数 + 120 根日 K；真实接入后 `MarketRepository.stockQuotes()/indices()/candles()/spark()/search()` 五个方法的语义不变即可。
