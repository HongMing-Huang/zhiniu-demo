# 知牛 · 变更日志（Work Log）

> 单一维护变更记录，防止记忆混乱。每条记录：时间 · 改了什么 · 影响/验证。
> 约定：开发中每次有实质改动即在顶部追加一行（不写无关文档）。格式：
> `YYYY-MM-DD HH:mm | 类别 | 改动描述 | 验证结果`

***

## 2026-09-11

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 18:05 | docs/feat | 对照课题验收标准全面自查与补齐：①新增 `AiMarkdown.kt` commonMain Markdown 解析器+渲染器（标题/列表/表格/代码块/引用/行内样式，官方 RichText/Span），接入 AI 研究主回复「研究归纳」、分析视角与 Mock 回复，解析器 10 项 commonTest 单测；②字体改走 KuiklyUI 官方接入：数字等宽 JetBrains Mono（OFL）单字体名 `NUM_FONT`，Android KRFontAdapter 实现、iOS 官方 Handler+共享资源副本、H5 @font-face+字体就绪门控启动（1.5s 兜底），build.sh 同步字体；③交付物完整性：web-host（H5 主入口）首次入库，README/快速开始/技术栈/架构/演示脚本按实态重写，新增 docs/typography.md 与 docs/deliverables.md（评分 40/25/25/10 自查映射）；④工程清理：移除未用 koin 依赖、幽灵 `:h5App` 模块、HelloWorldPage 与 `_pending_ui` 死代码，修复 README 占位符/断链/虚假声明 | `:shared:compileKotlinJs` 与 `scripts/build.sh` 通过；后端 53/53 单测通过；Playwright 实测：字体 loaded 且数字计算样式生效、AI Markdown 全块型渲染正确、来源去重、控制台 0 error、行情/详情/AI/深浅色截图入库 docs/img/ |
| 17:50 | build/android/ios | 三端静态检查与补齐（只读审查，未编译原生端、未启动模拟器）：新增 `shared/src/androidMain`、`iosMain` 的 4 组 `actual`（`createPlatformHttpClient` OkHttp/Darwin 引擎、`systemPrefersDark/watchSystemTheme/applyHostTheme`）；`shared/build.gradle.kts` androidMain/iosMain 补 `ktor-client-okhttp/-darwin:2.3.12`；iOS Podfile `OpenKuiklyIOSRender` 2.16.0→2.25.0 对齐 core；鸿蒙阻塞（Ktor 无 ohos 引擎、`libshared.so`/`libshared_api.h` 缺失、`@kuikly-open/render` 2.16.0）与三端就绪结论记录于 `docs/platform-readiness.md` | `:shared:compileKotlinJs` 仍通过；原生端编译按要求等待确认后进行 |
| 17:20 | ui/data | 首页课题字段补齐：`StockQuote` 增 `marketCap/turnoverRate/volumeRatio`；行情表新增总市值/成交量列 + 「字段」内联芯片自定义显示列（`tableEpoch` 奇偶重建表格）；新增「人气榜」（东财真实排名）/「涨幅榜」Tab 与 `RankTable`；`MarketTab`/`RoleTab` 的 active 改为 lambda 在 attr 内读取保证响应式；详情页 `factsOf(quote, fundamentals)` 改真实基本面+行情快照双源并补 `liveFundamentals` 拉取（修复上轮遗留 4 处 `factsOf(q)` 编译错误）；`?gateway=` URL 参数可覆盖网关地址（真机联调） | `compileKotlinJs` + `scripts/build.sh` 通过；浏览器（8084/8001）：总市值 1.59 万亿、人气榜 风华高科/N燧原 真实、「字段」取消总市值后列消失、详情换手率 0.28% 真实 |
| 17:00 | agent/ui | AI 研究页接入 `/agent/research/stream` 类型化 SSE（Ktor `preparePost`+`readUTF8Line`，流不可用回退 `research()`），阶段帧实时驱动进度条（替换定时假进度）；新增结构化「AI 解读」卡（短期趋势/投资建议/压力位/支撑位/置信度/归纳模式）、「多空对抗」多头/空头观点、风控视角（三方风控 + 交易员观察方案）；角色视图切换按 `lastResearch` 复述真实辩论内容；`AiMetricsBlock` 标签固定宽、长值多行 | 浏览器：发送「分析宁德时代」→ 下行/减持/压力 414.04/支撑 326.00/置信度 50%，多空 3 条，资讯 12 条 eastmoney-search，市值 1.53 万亿 |
| 16:30 | agent/backend | 移植 TradingAgents（TauricResearch，MIT）多 Agent 结构为 `backend/app/ta_agents.py`：多头/空头研究员对抗 → 研究经理五档评级（买入/增持/持有/减持/卖出）→ 交易员绝对价位观察方案 → 激进/中性/保守风控辩论；经网关 `zhiniu/quick` 降级链直接调用（不引入 LangGraph）；`agent.py` 接入并新增 `_levels_from_kline` 确定性压力/支撑（近 60 根高低点）；`stream_research` 用队列实时转发辩论阶段帧；无模型/超时/非法评级显式 `rule-engine` 同构回退 | pytest 新增 6 项（规则回退形状、LLM 全流程假响应、非法评级降级、进度转发）；真实调用：无有效 Key → `deterministic_fallback`，11.7s 完成 |
| 16:00 | quote/backend | 板块/选股/人气榜接真实数据：`/quote/sectors` 东财行业板块（按 f3 排序、领涨股）、`/quote/screener` 东财成交额榜前 100 池 + 行业/涨幅过滤、新增 `/quote/popularity` 东财人气榜排名 + 新浪实时增强；`/quote/realtime` 批量补充总市值/换手/量比（东财 ulist → 腾讯 gtimg 双真实来源）；新增出站白名单 `_guard_external_url`（仅 HTTPS + 行情域名，防 SSRF）；均 TTL 缓存 + stale/mock 显式降级 | pytest 42→53 通过（真实抓包样本解析 + 强制离线降级）；实测人气榜/市值为真实值；沙箱内 push2 限流时腾讯兜底生效 |
| 11:31 | quote/ui | 修复五档盘口真实数据在客户端模型层被丢弃的问题：`StockQuote` 增加买卖档位，网关 JSON 解析 `[价格,股数]`，盘口优先展示新浪真实五档并换算为手，仅缺档时确定性降级；窄屏侧栏 AI 标签强制单行 | 后端解析单测覆盖五档字段；Kuikly 编译、全量构建及浏览器切换验收通过 |
| 11:08 | ui/agent | 按 OKX 图表优先布局重构详情工作区：1024px 内图表与侧栏上下排列，侧栏改“盘口/数据/AI”原位切换；设置面板改紧凑主题分段与 /healthz 真实服务状态；研究 Agent 新增真实 LLM JSON 归纳、来源标记和无 Key 明示规则降级，详情追问改走后端 | Kuikly JS 编译与完整生产构建通过；后端 39/39 测试通过；浏览器复验详情与设置；真实新浪行情、K线和东方财富资讯返回成功，模型上游不可用时明确标记规则降级 |
| 10:21 | ui/design | 依据 OKX 工作区与当前窄屏截图重做 compact 布局：行情脉冲分两行、表格只保留股票/价格/涨幅/走势、工具条分层；详情 K 线/盘口上下排列；AI 收起会话侧栏；设置入口显式文字化并补齐 Kuikly 官方无障碍语义 | Kuikly JS 编译与 `scripts/build.sh` 通过；Codex 内置浏览器回归行情/设置/深色/详情/AI；后端 39/39 测试通过，新浪实时行情返回成功 |
| 01:46 | agent/backend | 研究管线新增 `/agent/research/stream` 类型化 SSE：统一 runId、四阶段完成事件、结果帧及显式 `run_finished/run_error` 终止帧；JSON/SSE 复用同一结果构建器 | 后端 39/39 单测通过；真实贵州茅台请求返回新浪行情/K线、东方财富资讯，约 295ms 完成 |
| 01:46 | ui/data | AI 研究页接入 `GatewayMarketClient.research`，展示真实价格、RSI14、MA20、资讯数量、来源、风险与时效；并在消息内容变化时自动跟随到最新结果 | 浏览器发送“分析贵州茅台”后显示 12 条资讯与完整证据卡，Composer 清空且最新消息可见 |
| 01:46 | ui/build | 按 Kuikly 官方 `pageData.safeAreaInsets` 为 Header/浮层增加顶部避让，为列表页尾和 AI Composer 增加底部避让；同步 API/UI/AGENTS/QA 文档 | `:shared:compileKotlinJs` 与 `scripts/build.sh` 通过；未启动模拟器 |

## 2026-09-10

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 23:13 | docs/design | 用 Browser 实看 OKX 工作区，并以 Exa/GitHub 官方资料复核 TradingView、ChartIQ、Quantower、TradingAgents、OpenBB、KuiklyUI、TDesign Icons、AKShare；同步架构/API/UI/AGENTS 文档 | 研究来源与实现边界已记录；未启动模拟器 |
| 23:13 | data/agent | 新增东方财富资讯 provider、120 秒缓存/stale/离线快照链；7 个 Agent 工具改为复用真实行情/K线/资讯；新增 `/agent/research` 四阶段证据管线；个股新闻 Tab 接网关 | 后端 37 项单测通过；真实新闻与研究接口返回成功 |
| 23:13 | ui/build | 设置面板扩为 288px 分组面板并使用 TDesign 图标；新增 compact/medium 断点；修复 1920 宽度下搜索/设置浮层定位越界；Header/AI 会话栏响应式适配 | `:shared:compileKotlinJs` 与完整生产构建通过；浏览器控制台 0 warn/error |
| 12:02 | qa/build | 修复详情五标签“状态已更新但界面不刷新”的根因：一次性 `when` 改为 Kuikly 响应式 `vif`，激活态改为响应式读取；移除会拦截框架事件的 H5 `mousedown` 补丁；构建脚本启用 `pipefail`，避免 Gradle 失败被 `tail` 掩盖 | 概览/资金/财务/新闻/AI解读逐项点击与内容断言 5/5 通过；生产构建通过 |
| 11:58 | qa | 以 1280×800、1440×900 浅/深色、1920×1080 验收市场/详情/AI；回归市场筛选、主题、AI 会话/新建/角色/发送、K 线按钮与鼠标滚轮缩放 | 浏览器控制台 0 error；K 线 80→68→80 且滚轮可改变可见根数；AI 核心交互通过 |
| 10:48 | data/backend | 前端 Market/StockDetail 接入 FastAPI `/quote/realtime` 与 `/quote/kline`；实时请求成功时覆盖快照，失败保留本地数据；修复新浪成交额单位（统一为元）和 K 线缓存键未包含周期/长度的问题 | 实测网关返回 2026-09-09 贵州茅台 1290.88 与 30 根日 K；后端 7 项单测通过 |
| 10:40 | ui/chart | 按 Product Design 审计与 Ponytail 最小根因修复：所有公共按钮/导航/Tab 强制单行；AI 会话改为可恢复的历史内容并居中；详情 AI 面板内容独立滚动；K 线默认最新 80 根，新增放大/缩小/复位、拖拽、捏合与 H5 滚轮缩放桥；移除可见 Demo/演示文案并更新设置状态 | 编译、浏览器交互与多视口验收通过 |

## 2026-09-09

| 时间  | 类别   | 改动 | 验证 |
| ----- | ------ | ---- | ---- |
| 22:57 | design | 完成当前前端 UI 终验：Header 层级修复；按钮行布局、激活态与收藏填充态统一；19 枚 TDesign 官方图标改为透明底预着色 PNG，移除 H5 tint 方块伪影；1280 档隐藏次要行情列避免横向溢出 | Codex 浏览器实测 1280×800、1440×900 浅/深色、1920×1080；市场/详情/K线/五档/AI 路由与交互通过，控制台无 error/warn |
| 22:53 | ui     | 主题调色板改为稳定的 AdaptiveColors 动态语义代理，修复浅/深色切换后背景、文字、边框和图表混用旧色；行情数字单元改为动态取色 | 市场页与个股 K 线页深色模式视觉复验通过 |
| 22:45 | ui     | AppInput 接入 Kuikly 官方 ViewRef/InputView.setText，Enter 与发送按钮提交后显式清空 AI Composer；修复仅更新状态但 H5 原生输入值残留 | AI 研究页提交“分析贵州茅台”后结构化回复出现且输入框清空 |
| 22:39 | build  | 重新构建并部署 H5 bundle；图标生成脚本使用透明背景与中性灰预着色，避免 Kuikly H5 对透明 SVG/PNG 的边界盒 tint 问题 | `:shared:compileKotlinJs`、`scripts/build.sh` 通过；`:shared:jsTest` 被 Kotlin/JS IR `callKotlinMethod` 重复绑定的上游编译器错误阻断（clean 后复现） |

## 2026-09-04

| 时间    | 类别     | 改动                                                                                                                                                                                                     | 验证                                                    |
| ----- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------- |
| 18:28 | build  | 修复 H5 启动链路（根因：index.html 缺 `kuiklyBundlesReady` 置位 + 跨端口 8083 直引 + 旧 dev 宿主混用 → 白屏/乱码/路由失效）：bundle 同源部署 web-host/，index.html 改「nativevue2.js → ready 置位 → h5App.js」同源顺序；build.sh/dev.sh/AGENTS 同步单端口链路 | 编译通过 + 构建部署 + 8082 全资源 200                            |
| 18:22 | design | 设置面板补全：ThemePopover 升级为「外观/数据源/模型/免责声明」四分组（主题三档切换 + 数据源/模型演示态「接入中」标识）                                                                                                                                  | 编译通过 + 构建部署                                           |
| 18:13 | chart  | 分时图补均价线：累计 VWAP 橙线（原仅价格线+面积），对齐股票软件分时双线标配                                                                                                                                                              | 编译通过 + 重新部署 v9                                        |
| 18:12 | ui     | 新增五档盘口 Level2Panel（详情页 Rail 顶部：卖五\~买五 + 中间最新价/涨跌 + 相对量条，卖绿买红确定性模拟量）                                                                                                                                    | 编译通过 + 完整构建 + 部署 v9                                   |
| 18:08 | ui     | 详情页底部 Tab 扩为 5 个（概览/资金/财务/新闻/AI解读）：资金=主力/超大/大/中/小单净流入（随涨跌方向正负）、财务=估值+营收/净利/毛利率/负债率、新闻=个股资讯列表                                                                                                           | 编译通过 + 完整构建 + 部署 v8                                   |
| 18:05 | ui     | 自选交互闭环：详情页改用 FavoriteButton（点击切换「加自选⇄已自选」星形填充+文案），首页新增「自选」Tab 真实过滤（Watchlist 驱动）                                                                                                                       | 编译通过 + 部署 v7                                          |
| 18:00 | design | 按钮图标插槽：PrimaryButton/SecondaryButton/GhostButton 支持前置 icon；接入详情「加自选⭐/AI 分析🤖/查看完整分析🤖」、AiBlocks「查看详情📈」                                                                                                | 编译通过 + 部署 v6                                          |
| 17:55 | agent  | TradingAgents 研究落地：AiResearchPage 新增「角色视图」Tab（分析/多空对抗/风控，2px AI 色指示器 + 切换引导消息）；ui-redesign.md 记录研究结论与迁移点                                                                                               | 编译通过 + 部署 v5                                          |
| 17:50 | design | 图标系统扩充：TDesign 官方 1000 图标复核，新增 6 个 PNG（calendar/check/filter-sort/error-triangle/chat-message/data-display），IconKind 扩至 19；接入排序按钮与会话行                                                                  | 编译通过；6 图标 HTTP 200                                    |
| 17:45 | chart  | K 线规范对齐股票软件：时间轴首/中/末 3 标签、蜡烛/量柱/MACD 宽度改 slot\*0.7 自适应（原固定 6-9）                                                                                                                                        | 编译通过                                                  |
| 17:10 | ui     | UI 精修 4 处：①Header 品牌占位（22px 描边方块 Z + 知牛）；②AI 消息头改用 aiAccent #D9FF43；③ThemePopover 扩展免责声明分组（设置占位）；④详情页 AI 分析按钮改 aiAccent 描边强调                                                                           | 编译通过 + 浏览器验证（Z logo ✓、AI 按钮 rgb(217,255,67) ✓、免责声明 ✓） |
| 17:05 | ui     | 三屏浏览器验收：MarketPage（涨红/跌绿 token 生效 #F04F5F/#16B364）、StockDetail（K线 tab+五档+Canvas 899×520）、AiResearch（AI 问候+推荐问题）、主题切换（Light/Dark）                                                                       | 全部通过；index.html js 引用加 ?v=2 刷新缓存                      |
| 14:40 | git    | 推送全部提交至 `HongMing-Huang/zhiniu-demo` 远程仓库（已建仓）                                                                                                                                                         | gh 推送成功，origin/main 最新                                |
| 14:35 | docs   | 新增 `docs/WORKLOG.md`（变更日志规范）；完善 `AGENTS.md`：§0 工作日志、GitHub 仓库信息、Kuikly 2.25.0、验证流程修正（移除不存在的 v4\_final.js）、§9.1 Git 提交规范                                                                                | 文档已同步                                                 |
| 14:10 | git    | 初始化 GitHub 独立仓库 `HongMing-Huang/zhiniu-demo`（private），`origin` 关联并推送 main                                                                                                                              | gh repo create 成功，推送 HEAD→main                        |
| 13:55 | design | `Theme.kt` 统一规范：涨 `#F04F5F`、跌 `#16B364`、AI 强调 `#D9FF43`（默认值）；MA/RSI 指标紫保留；同步 `AGENTS.md` 色彩条款                                                                                                          | 编译通过                                                  |
| 13:50 | docs   | 新增 `docs/ui-redesign.md`：现状诊断 / OKX 参考 / 规范 v2 / 素材占位清单 / 新浪直连与 Agent 端到端接入研究                                                                                                                          | 已提交 commit 4e09d82                                    |
| —     | design | 基础提交：目标结构 + Kuikly 2.25.0 + 市场排序/筛选（commit 0f9dcce）                                                                                                                                                    | 编译+浏览器验证通过                                            |

## 2026-09-03

| 时间    | 类别    | 改动                                                                                             | 验证                                       |
| ----- | ----- | ---------------------------------------------------------------------------------------------- | ---------------------------------------- |
| 20:21 | ui    | 补齐全真排序（默认/涨幅/跌幅）与筛选（仅上涨）交互                                                                     | 浏览器端到端验证：排序升降序、筛选开关、行跳转详情全部通过            |
| 20:15 | build | 升级 Kuikly 2.16.0 → 2.25.0（buildSrc/KotlinBuildVar + ohos 依赖 + 文档同步）                            | productionWebpack 全量编译成功，MarketPage 正常渲染 |
| —     | ui    | 目标结构落盘：MarketPage/StockDetailPage/AiResearchPage + 公共组件体系 + TDesign 图标 + Light/Dark 主题（历史重构基线） | 浏览器验证                                    |

***

## 待办 / 已知

- [ ] UI 精修三屏（Header 品牌位 / 全数字等宽接入 / AI 强调落地 / 设置面板占位）

- [x] 实时行情与 K 线接入（FastAPI 8000 网关；失败时保留本地快照）

- [ ] Agent 端到端联调（后端 gateway 已具雏形：SSE + tools 编排）

- [ ] 素材替换（用户后续提供：品牌 logo / 个股 logo / AI 配图）
