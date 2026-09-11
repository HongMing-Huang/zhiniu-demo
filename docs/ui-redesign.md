# 知牛 · UI 重设计与数据接入研究（2026-09-03）

> 阶段目标：先研究 → 设计 → 开发。本文档 = 现状诊断 + 参考研究 + 统一设计规范 + 素材清单 + 实施顺序。
> 关联工程：`kuikly-shell`（Kuikly DSL，2.25.0-2.1.21）；视觉基线沿用既有 AppBasePage / AppTheme / 组件体系，避免推翻重写。

***

## 1. 现状诊断（代码层，2026-09-03 快照）

### 1.1 已就位（不推倒）

- 页面壳：MarketPage / StockDetailPage / AiResearchPage，AppBasePage 统一 Header + SearchOverlay + ThemePopover

- 组件：AppButton / IconButton / StockTable / StockRow / MarketPulse / KLine(Canvas) / ChartToolbar / AiInsightPanel / AiInsightDrawer / SearchOverlay / ThemePopover

- 主题：Light / Dark / System，180ms 过渡，token 语义化（`AppColors` 单点改色）

- 图标：TDesign 20 个线性 PNG，使用本地中性灰预着色资源，native 与 web-host 双端落地

- 交互：行 hover(CSS 120ms)、按钮按压 `highlightBackgroundColor`、Tab 160ms、浮层 160–200ms

- 2.25.0 已生效：`mouseEnter/mouseExit` 事件可用（此前 2.16 缺失）

### 1.2 需要修正的规范漂移（对照最初产品要求）

| 维度        | 最初要求（用户早期）         | 当前实现                     | 结论                                                                    |
| --------- | ------------------ | ------------------------ | --------------------------------------------------------------------- |
| Dark 页面底  | `#0A0A0A`          | `#0B0D0F`                | 二者均近纯黑；统一用 `#0B0D0F` 保留层级                                             |
| Dark Card | `#141414`          | surface `#111417`        | 统一 surface `#111417` + surfaceSecondary `#171B20` 层级更深，视觉更清晰          |
| 上涨红       | `#F04F5F`          | `#E5484D`                | **采用** **`#F04F5F`（更明亮的正红，暗底上可读性更好）**                                 |
| 下跌绿       | `#16B364`          | `#13A66A`                | **采用** **`#16B364`**                                                  |
| AI 强调     | `#D9FF43`（灰绿荧光，少量） | 紫 `#A56BEA`              | **采用** **`#D9FF43`**，仅用于 AI 触发面（按钮/入口/观点关键字），避免大面积                    |
| 禁         | 蓝紫渐变 / 毛玻璃 / 发光    | aiAccent 紫在 MA10 等技术指标使用 | 指标线仍可用区分色（MA5 橙 / MA10 蓝 / MA20 紫）——**与 AI 强调分离**：AI 紫改为 `#D9FF43` 体系 |
| 内容宽       | 1240–1320          | 1360                     | 保持 1360（1920 下两侧留白更多，观感更克制）；narrow<1280 折叠                            |

> 落地方式：只在 `Theme.kt` 改 `up/down/aiAccent` 三值 + `Light/Dark` 内 MA/指标线保留，全局自动生效（token 单点）。

### 1.3 结构性短板（用户反馈「界面没做好」的根因候选）

1. **品牌/终端感弱**：Header 采用文字「知牛」；正式 Logo 未提供前不绘制伪造图形资产，避免把临时字母块误当品牌标识。
2. **首屏密度与对齐**：标题「市场」区 72px 高但后续 Pulse/Table 与 OKX 的紧凑对齐还有差距；数字未全量等宽字体对齐（`NUM_FONT` 已定义未全量接入）。
3. **详情页行情区**：报价头数据横排后，二级数据(今开/最高/最低…) 行高与 Tab 层级需对齐成熟券商；K 线图 560 高 OK，但十字线与周期切换提示未完善。
4. **设置只有主题**：无入口面板承载「模型/免责/数据源」等 → 计划 ThemePopover 扩为通用 SettingPopover 或独立浮层（遵守 3+3 页面约束，不建独立页）。
5. 空态 / 骨架：有 Skeleton 与 EmptyState，但股票无 Logo 时的行内占位处理不统一。

## 2. 参考研究（外部）

### 2.1 OKX Markets / Prices（2026-09-03 抓取）

表结构列序：名称｜最新价｜24h涨跌幅｜过去24h(走势 mini)｜24h区间｜市值｜操作。要点：

- 单行两行信息：主行名称（大写缩写+全称），价格右对齐等宽

- 涨跌列以同色 **箭头/前缀**，区间并列高低两值

- 行 hover 浅层背景；行无独立卡片；分割线极细

- 顶部 Tab 用「文字 + 选中下划线」；右上搜索 + 图标按钮组

- 我们首页表列：股票｜最新价｜涨跌额｜涨跌幅｜今日走势｜高/低｜成交额 —— 与 OKX 一一对应，方向正确，仅需微调列宽/间距/等宽数字。

### 2.2 TradingView Screener / Webull·富途 借鉴点（既定结论沿用）

- 股票名与代码两行；涨跌用「数值+百分比」列，负值同色

- 详情页报价区：大价格 + 涨跌额/幅同排，下一行 6–8 项指标无卡片横排；指标标签灰小字、值右对齐

- 底部 Tabs：概览/资金/财务/新闻（AI 解读为知牛特色 Tab）文字 + 2px 指示器

- 图表区：价格轴左、时间轴下，十字线随 hover；周期 Tabs 悬浮于图顶

## 3. 统一设计规范（v2，作为下一轮实现基线）

- 配色：见 §1.2 修订表（核心改 3 个 token）。AI 按钮/关键字用 `#D9FF43`，其前景文字用深色 `#0B0D0F`。

- 字体：正文系统 sans（SF/PingFang）；**全部数字/价格/涨跌幅/成交额/指标值接入** **`NUM_FONT`** **等宽**（统一列对齐）。

- 间距节奏：区块 28 / 面板 16 / 行 64 / 表头 42；Radius 8 主卡片、6 控件。

- 细节规则：

  - 分隔线 1px；hover 过渡 120ms；数值变化短闪 300ms（保留现有实现，非阻塞）

  - 涨幅数字前「+」、跌幅「−」用半角，等宽下对齐

  - 除主 CTA 与 AI 入口外，按钮一律「surface 底 + 1px border」，无投影

- Logo/素材缺失时：保持纯文字或省略图片位，不用字母方块、手绘图形或 Emoji 冒充正式资产。

## 4. 素材清单（占位 → 待用户提供）

| 位           | 占位方案                            | 待提供素材                  |
| ----------- | ------------------------------- | ---------------------- |
| Header 品牌标记 | 纯文字「知牛」，不伪造 Logo | 品牌 logo 图（可透明 PNG/SVG） |
| 个股无 Logo    | 行内股票名首字灰底块（W / N 等）             | 个股 logo 集或放弃 logo      |
| AI 研究页配图    | 中性几何（ChartPainter 折线块），不用图      | AI 配图/横幅               |
| 搜索空态        | EmptyState 文本 + 图标              | 插画素材（可选）               |
| 设置面板        | 主题 + 模型 + 免责（文字态）               | 无                      |

> 用户已确认：素材后续全部提供；实现先以占位符。

## 5. 真实数据接入研究（前端「直连新浪」）

- 目标：搜索下拉真实联想、行情/K 线真实、断网回退 Mock（既有 `MarketRepository` 语义不变）。

- 障碍（H5 实测前提）：`hq.sinajs.cn` 需合法 Referer 且默认不允许跨域；`suggest3.sinajs.cn` 仅 JSONP。纯浏览器 fetch 会 CORS 失败。

- 结论/推荐（研究输出）：

  1. **短期（不依赖 Python）**：Kuikly H5 由宿主页注入小段 JS 完成 JSONP 搜索联想 + 将结果桥接回业务（`BridgeModule` 已有通道）——规避 CORS，兑现「前端直连新浪」；
  2. 行情实时/K 线同步走 JSONP 桥接或同源静态代理（`/web-host` 同源加轻量转发已具备条件，因 web-host 与 8082 同源）；
  3. 原生端（Android/iOS）直连无 CORS，天然可行，同一 Repository 实现按平台分派。
  4. Mock 兜底策略不变；加载态用现有 `marketLoading` 骨架。

- 开发落点：`data/remote/`（现为空）新建 `SinaQuoteSource`(expect/actual) + `SearchSource`，`MarketStore.repository` 仍是唯一替换点。

## 6. Agent（知牛智能体）端到端设计要点（研究输出）

- 后端（主链路已实现）：`/v1/chat/completions` 支持 SSE + function-calling；工具复用真实行情/K 线/资讯数据源；`POST /agent/research` 返回四类可审计证据与第五阶段模型归纳，模型不可用时明确标记规则降级；`/agent/research/stream` 提供类型化阶段与显式终止事件。

- 端到端缺口：

  1. AI 研究页已通过 `GatewayMarketClient.research` 接入真实研究网关，并渲染阶段、来源、RSI/MA20、资讯数量、风险与时效；网关失败才退回 `MockAiService` 快照。
  2. 消息状态机（Streaming/工具卡片/失败重试）在 `AiInsightPanel`/`AiResearchPage` 收敛。
  3. 工具结果可视化：quote→报价块、kline→迷你图、news→列表块（复用 AiBlocks）。

- 依赖：需启动 Python 后端；`.env` 配置模型 Key 后启用真实模型归纳。无可用模型或上游失败时，研究结果保留真实数据证据并显示“规则降级 · 未伪装模型”。

- 会话历史持久化：`data/local` 现有 Watchlist 模式扩展 ChatSession。

## 7. 实施顺序（开发阶段待执行）

1. Token 三值修订（up/down/aiAccent）+ 全数字等宽接入（低风险、可立即）
2. 数据接入：Remote 层（搜索 JSONP 桥 + 行情/Agent RemoteAiService）→ 替换点生效 → 断网回退验证
3. UI 精修三屏：Header 品牌位/首屏对齐/详情报价区/设置面板占位
4. Agent 端到端联调（启动 8000 + Key/Mock）
5. GitHub：用户建空仓或提供 PAT → 关联 origin 推送（当前代码已本地提交）
6. 素材替换（用户提供后逐位替换占位）

## 8. TradingAgents 研究结论（2026-09-04，借鉴到知牛智能体）

> 来源：<https://github.com/TauricResearch/TradingAgents>（Structured-Graph/Backtesting v0.3.x，LangGraph）。

- **多角色分工**：Analyst Team（Fundamentals / Sentiment / News / Technical）→ Researcher Team（多空对抗辩论）→ Trader（决策）→ Risk Mgmt（风控评估）→ Portfolio Manager（最终批准）。

- **对知牛的迁移点**（不照搬全量，赛题内做成 3 屏内的增强）：

  1. `AiResearchPage` 聊天气泡可加「视图切换」：分析（默认）/ 多空对抗 / 风控结论 —— 结构化 5 类 AiBlock 已具备承载。
  2. `AiInsightDrawer`（个股侧）输出 4 个维度块：基本面 / 技术面(MA·RSI·MACD) / 情绪(新闻) / 风险 —— 对应 Analyst 分队；追加「多空摘要」与「交易计划」占位。
  3. 后端 `gateway.chat_completions_orchestrated` 已有 tools（quote/kline/financials/news/screen/compare/alert），可让任一角色调用真实数据，构成 Agent 工具闭环。

- **实现边界更新**：视图切换 Tab、研究进度、真实证据卡与 JSON 端到端链路已落地；后端类型化 SSE 契约已就绪，下一步是多端流解析与真实模型归纳回答联调。

## 9. 2026-09-10 交易终端与数据架构复核

- Browser 实看 OKX Spot 桌面交易页：品牌固定左上；左侧市场列表、中央标的/K 线、右侧盘口形成稳定三栏；周期、指标、显示、设置、全屏等控制贴近图表，不散落为大卡片。
- OKX 官方帮助进一步确认工作区可折叠、可全屏、布局可定制，多图同步符号/周期/十字线/日期。TradingView Advanced Charts、ChartIQ、Quantower 官方资料共同支持滚轮缩放、拖拽平移、捏合、十字线 OHLCV、键盘替代与“回到最新”控件。
- GitHub 官方仓库复核：`Tencent-TDS/KuiklyUI` 提供 `activityWidth/activityHeight`、`safeAreaInsets`、pinch 等跨端依据；`Tencent/tdesign-icons` 作为唯一图标源；`TauricResearch/TradingAgents` 与 `OpenBB-finance/OpenBB` 支持“角色编排 + provider/router/fetcher”架构。
- 数据策略：当前新浪行情/K 线与东方财富资讯都经 FastAPI provider boundary，前端不直接处理 Referer/GBK/JSONP；所有资讯带来源、链接、发布时间、provider 与 stale 标记。
- 合规边界：AKShare 明示数据仅供研究且上游接口可能移除；`TradingAgents-CN` 的 `app/`/`frontend/` 标注专有授权，故仅借鉴公开架构，不复制相关代码。
- 多端检查：断点统一为 compact ≤720、medium ≤1024、narrow ≤1280、desktop；弹层定位修正为相对最大内容区右缘。模拟器不在本轮启动。

## 10. 图标与 K 线规范核对（2026-09-04 落地）

- **图标**：TDesign 官方 1000 图标复核（`Tencent/tdesign-icons` develop）。新增 6 个（PNG 80×80 白描边透明底，与现有规格一致）：
  `calendar` / `check` / `filter-sort` / `error-triangle` / `chat-message` / `data-display`；`IconKind` 扩至 19 个。
  已接入：排序按钮（默认排序→filter-sort，涨幅/跌幅→chevron-down）、会话列表行（chat-message，选中高亮）。

- **K 线规范核对**（对照 OKX / 富途 / Webull）：

  | 项         | 股票软件标准           | 当前                      | 结论            |
  | --------- | ---------------- | ----------------------- | ------------- |
  | 涨跌色       | A股 红涨绿跌          | up=#F04F5F down=#16B364 | ✅             |
  | 蜡烛+影线     | 实体+上下影线          | ✅ 已有                    | ✅             |
  | 时间轴       | ≥3 个标签（首/中/末）    | 曾仅首/末                   | ✅ 已补中值        |
  | 蜡烛宽度      | 随 slot 自适应 \~0.7 | 曾固定 6-9                 | ✅ 改 slot\*0.7 |
  | 量柱/MACD柱  | 与蜡烛同宽比           | 同步 0.7                  | ✅             |
  | MA5/10/20 | 主图叠加             | ✅ 已有                    | ✅             |
  | 成交量副图     | 红涨绿跌半透明          | ✅ 已有                    | ✅             |
  | MACD/RSI  | 副图可选             | ✅ 已有                    | ✅             |
  | 十字线+OHLC  | hover 十字+左上信息块   | ✅ 已有                    | ✅             |
  | 分时        | 折线+昨收基准          | ✅ 已有                    | ✅             |

- 字体：数字统一 `NUM_FONT` 等宽栈（Table/QuoteMetric/MarketPulse/ChartToolbar 已接入）。

- **五档盘口**（2026-09-04 新增 `Level2Panel`，对齐委托盘口）：详情页 Rail 顶部「卖五~~卖一 ｜ 最新价+涨跌 ｜ 买一~~买五」，卖绿买红、量右对齐 + 半透明相对量条，挂单量按 symbol 种子确定性模拟（`seededVol`），真实行情接入后由 Repository 替换。

## 11. 待用户确认的 2 项

- A. 涨跌色/AI 色按 §1.2 修订表统一？（推荐：是）

- B. 「真实数据」H5 采用 §5 的 JSONP 桥接方案（推荐）还是先启用 Python 后端 /quote/\*？

## 12. 2026-09-11 二次审计：图表优先与可验证 Agent

- OKX 官方把图表、订单/持仓面板显隐和工作区保存作为一套布局能力；交易设置集中管理主题、涨跌颜色、操作按钮与页面布局。对应实现不再把盘口、关键数据和 AI 长文同时堆进 28% 右栏，而改成“盘口 / 数据 / AI”原位切换；在 ≤1024px 时图表与侧栏上下排列，优先保住完整 K 线。
- 设置面板把三条稀疏主题菜单改为一个分段控件；行情与 Agent 状态来自 /healthz，静态说明不再伪装成可点击配置。
- GitHub 参考边界：TradingAgents 用多角色图编排表达研究流程；OpenBB 用 provider/registry 分离数据能力；FinRobot 强调确定性计算、模型叙述、结果溯源。知牛只借鉴公开架构，不复制界面或业务代码。
- 研究管线增加第五阶段“归纳 Agent”：行情/K 线/资讯仍由代码计算；配置模型时做 JSON 归纳，否则明确标为 deterministic_fallback。

资料：OKX Chart Trading Layout、OKX Trading Settings、OKX Candlestick Settings、TauricResearch/TradingAgents、OpenBB-finance/OpenBB、AI4Finance-Foundation/FinRobot。
