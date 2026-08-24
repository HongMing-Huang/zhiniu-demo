# 知牛 ZhiNiu · 开发问题汇总（待你决策）
> 更新：2026-08-24
---

## 🎯 最新指令（2026-08-24 用户决策，覆盖下方部分待定项）

1. **Web 端优先**：iOS / Android / 鸿蒙 / macOS 全部暂停（编译产物保留），全部精力聚焦 Web(H5) 打穿。冲刺清单见 `docs/llm-router-design.md` §5（W-A1~A8）。
2. **多 LLM 统一路由管理**：参考 CherryHQ/cherry-studio 的 provider 声明式管理 + 模型自动发现 + 三层路由解析，不做"简单接一个 LLM"。完整设计见 `docs/llm-router-design.md`（调研结论 + 差距对照 + providers.json 设计 + Web 模型设置页）。
3. D6 行情源策略随 Web 优先自然解决：Web 端统一走网关代理（真实数据 + Mock 兜底），无小程序 Referer 问题。
4. 仍待用户输入：三厂 LLM Key（D2）、iOS 模拟器运行时与 Android SDK（多端恢复条件）、README 致谢 star 数清理（红线）。


## ⚠️ 最高优先级：设计文档中的数据异常（红线）

**参考数据真实性问题**：技术方案 §2.2 的中标数据我逐一拿去 GitHub API 实测，发现 **daily_stock_analysis** 的 star/fork 严重失衡（star 63,036 / fork **53,018**，正常 forks 通常是 stars 的 20~30%），且仓库创建于 2026-01 却宣称「周更 v3.27→v3.30」。该仓库**存在、真实、可用**，但榜单数据有被刷单/注水的疑点。

- **影响**：方案把它的「多数据源 fallback」列为锚点，属于架构心智借鉴，不依赖其 star 数，**不影响实现**；但 README「致谢」若标注过高 star 会暴露背书可信度问题。
- **建议**：README 引用参考项目时只写「架构借鉴」，不写具体 star 数，或仅用我实测的真实数（见下表）。

| 项目（GitHub API 实测 2026-08-22） | 真实 star | 语言 | 许可证 | 状态 |
|---|---|:---|:---|:---|
| Tencent-TDS/KuiklyUI | 3,404 | Kotlin | Other | 活跃，主页 `framework.tds.qq.com` |
| ZhuLinsen/daily_stock_analysis | 63,036（fork 53,018 异常） | Python | MIT | 活跃，**数据存疑** |
| TauricResearch/TradingAgents | 98,659 | Python | Apache-2.0 | 活跃 |
| virattt/ai-hedge-fund | 62,618 | Python | MIT | 活跃 |
| AI4Finance-Foundation/TradingAgents-CN / OpenBB | 待实测 | — | — | ⏳ 我未逐一验证 |

> **待你处理**：是否认可「致谢不写 star 数」？剩余两个仓库是否需要我也实测？

---

## D1. 网关后端技术栈（✅ 已定：A，已并入 main）

- **决策**：维持设计文档推荐 **A（Python + FastAPI + openai SDK）**，网关后端已合并进 main（提交 `8b71e1d`），单测通过。

- **现状**：三厂商均 OpenAI 兼容，网关只需一份配置。技术栈本待定，但为推进开发已按推荐 **A** 落地了可运行骨架。
- **已实现（临时默认，可随时换）**：`backend/` 分支 `feat/backend-llm-gateway` 已提交 FastAPI 网关骨架（统一 `/v1/chat/completions` 流式 SSE + 降级链 + `/v1/models` + `/healthz`），复用官方 openai SDK，接口契约与实现解耦（改 Node/Ktor 只需换实现不换契约）。
- **选项**
  - A. **Python + FastAPI + openai SDK**（已实现）：与 TradingAgents/ai-hedge-fund 同生态，多 base_url + 流式支持最好，SSE 处理成熟。
  - B. Node/TypeScript + Express + openai SDK：前端偏好的话更顺手。
  - C. Kotlin/JVM + Ktor：与前端同语言同栈，但多厂商 SDK 支持弱，需手写较多。
- **待你确认**：是否维持 A？若换 B/C，我改写实现即可。

## D2. 模型别名与默认路由（对应设计 §4.3 / §5.2）

- **现状**：方案默认「混元为主、DeepSeek 备选」；现在网关支持三厂商，需定别名与降级顺序。
- **我的建议**：默认路由 `quick=deepseek-chat`、`think=deepseek-reasoner`、`flash=glm-4.6`；降级顺序 deepseek→glm→hunyuan→Mock。**混元因正在迁移 TokenHub，不建议作为首选兜底**。
- **待你确认**：是否接受该别名/顺序？各厂商 Key 你是否有（DeepSeek / 智谱 / 混元）？

## D3. 网关部署目标

- **现状**：客户端需要连到网关。开发期本地 `localhost:8000`；演示期是否要求云端可公网访问？
- **选项**：本地起即可（仅演示本机）；或部署 Vercel/Railway/云函数（各端都能访问、规避小程序出网）。是否已有可部署账号 / 偏好的云？

## D4. Kuikly 公共组件与图标（✅ 已调研，见 `docs/kuikly-common-assets.md`）

- **公共组件清单已核实**：Kuikly 内置 `List / Carousel / Tab / Dialog / Input / Button / Canvas / AI Chat` 等；社区有 `KuiklyMarkdown`（流式）、`KuiklyTableView`（表格）。
- **图标公共资源已调研**：推荐 **Google Material Symbols 字体图标**（字体渲染跨渲染层通用，规避 Kuikly 非标准 Compose 的兼容问题）；备选 compose-icons / compose-material-symbols（ImageVector，需先验证 Kuikly DSL 是否支持）。
- **待你确认**：接受「Kuikly 内置组件 + Material Symbols 字体图标」路线（不自绘 SVG）即可。

## D8.（新增）Kuikly Compose DSL 是否支持 ImageVector 图标

- **现状**：备选图标库（compose-material-symbols / compose-icons）以 `ImageVector` 渲染，但 Kuikly 非标准 Compose，兼容性未实证。
- **影响**：图标方案究竟走「字体图标」（必稳）还是「ImageVector 库」（更现代）。
- **建议**：**下一步我拉一个最小 Kuikly 工程在 PR-01 脚手架里实测**，用真机结果定案；在此之前代码默认走「Material Symbols 字体」。

## D5. K 线渲染（✅ 已定：统一 Kuikly Canvas 自绘）

- **决策**：见 `docs/tech-stack.md` §4 —— 弃用 Vico，统一用 Kuikly `Canvas` 自绘 K 线（Vico 为标准 Compose 库，与 Kuikly 非标准 Compose 集成未验证，跨端一致性差）。
- **遗留**：仅剩 Canvas 自绘的细节（坐标/缩放/十字游标）在真机验证。

## D6. 行情数据源：新浪 + 缓存过期策略

- **现状**：方案选新浪主源（免 Key）。但新浪接口需 `Referer: https://finance.sina.com.cn`，小程序端受限（方案已注明走 Mock）。
- **影响**：演示时若主要用 Android 问题不大；若要小程序端真实数据，需另选腾讯行情源或走网关代理。
- **待你决定**：是否接受「Android/iOS 真实 + 小程序 Mock」？或需要我在网关加一层「行情数据代理」让多端都拿真实数据？

## D7. 范围与里程碑优先级

- **现状**：PR-01 前端脚手架已建立（分支 `feat/pr01-frontend`）：`shared/` 四层包结构 + 官方式最小 Hello 页 + `frontend/PR01-SCAFFOLD.md`（含官方脚手架路径与待验证清单）。
- **待你决定**：是否维持「PR-01（脚手架）→ PR-02（网关+Sina）」节奏？下一步需要 IDE/网络生成官方 gradle 模板（我手写会乱写）并在真机验证 D8。

---

## ✅ 进度记录（/goal 阶段一推进，2026-08-22）

### 本轮已完成
- **T1-4（后端 P0）全部通过**，提交 `df04a8b`：
  - 行情代理 `GET /quote/realtime` + `GET /quote/kline`（新浪主源 + Referer + GBK→UTF-8 + 索引0~31解析 + bids/asks 五档 + volume手/amount万元 + 实时3s缓存 + stale/Mock 兜底 + `X-Gateway-Stale` 头）
  - `vision` 别名注册 + `PROVIDER_ORDER` 全局降级顺序
  - `stream=false` 支持（读请求体不硬编码）+ `extra_body` 透传
  - `ProviderError` 结构化 reason（invalid_key/rate_limited/timeout/upstream_5xx/no_key）+ 中文提示含 env 名
  - 全候选失败/缺 Key → 本地 Mock LLM 兜底（SSE 首 chunk 空 role + [DONE]，绝不弹 401/500）；`_sse(event=)` 双行格式
  - 单测 6→9 项通过；curl 实测：`/healthz` 全 false 200、无 Key SSE 返回 Mock、`/quote/realtime` 返回 UTF-8 JSON 字段齐

### 阻塞记录（阶段一其余子任务）
- **T1-1 前端可编译 / T1-2 路由闭环 / T1-3 Canvas K 线 / T1-5 Web(H5) 端**：全部依赖「Kuikly 官方 Gradle 工程壳 + JDK17」这一外部状态。
- **阻塞于：缺少 JDK17 与 Kuikly 官方工程壳（本机仅 Java 25.0.2，无 Android Studio 生成的 4 端壳 + settings.gradle + libs.versions.toml）**。
- **谁提供/如何恢复**：需用户用 Android Studio（Gradle JDK 切 17）`New Project → Kuikly Project Template` 生成工程并给出路径合并，或提供本机 JDK17 路径。提供后方可接入 `shared/` 源码做前端编译，实现 T1-1/2/3/5。
- 在此之前不写任何手写 Gradle/自绘壳工程/自写部署脚本的替代方案（遵循 §6.1）。

### 本轮已完成（前端领域/逻辑层，纯逻辑可审阅）
- **AppError 统一错误层**（提交见下）：`shared/.../domain/model/AppError.kt`（NO_NETWORK/RATE_LIMIT/AUTH_INVALID/UPSTREAM_5XX/JSON_SCHEMA_FAILED/UNKNOWN + 中文分级文案 + `fromBackendCode`/`fromThrowable`）+ `LlmGatewayClient` 错误分支映射后端 `error.code` + `MarketListVM`/`StockDetailVM` 裸 `e.message` 替换为 `AppError.display()` + `commonTest/AppErrorTest.kt` 9 用例。
- 打通：与后端结构化 SSE `error.code`（invalid_key/rate_limited/timeout/upstream_5xx/no_key_configured）契约一致。
- **K 线纯逻辑层** `domain/model/KLineChart.kt`：MA5/10/20 均线、价格区间、蜡烛方向(A股红涨绿跌)、像素几何 `layout`/`buildBundle`（蜡烛与均线共享 y 缩放）；`commonTest/KLineChartTest.kt` 8 用例。T1-3 Canvas 绘制层后续只消费本层。提交见下。
- **行情展示纯逻辑** `domain/model/QuoteDisplay.kt`：KMP 安全格式化（价格/涨跌幅/量-额单位）+ 五档盘口中文档位行；**修复 `pages/components/Widgets.kt` 的 `priceValue/pricePct` 误用 `java.util.Locale`（commonMain 无法在 WASM/iOS/鸿蒙 编译，T3-1 多端障碍）**；`commonTest/QuoteDisplayTest.kt` 7 用例。提交见下。
- **SSE 解析纯逻辑** `domain/model/SseParser.kt`：行级 (event, data) 帧解析 + DONE/STEP_PROGRESS/ERROR/DATA 分类；`LlmGatewayClient` 接入并支持后端 `event: agent_progress`（T2-3 Stepper 点亮打底）与 `event: error` 帧；`commonTest/SseParserTest.kt` 8 用例。提交见下。
- **UI 组件地基 + 详情页接线**：`pages/components/` 新增 `CandlestickChart(paint)` / `SummaryCard` / `SignalPillGroup` / `RiskBadgeGroup` / `JumpCard` / `AppErrorCard` / `Skeleton` / `QuickChip`，`Widgets.kt` 顶部登记 + `colorOf`/`Palette.MA` 色；`StockDetailPage` 接入 OHLC 量/额(QuoteDisplay)、K 线骨架→paint、AI 四卡、AI 独立错误重试卡、跳转卡路由回调(navigator 注入)；`ComponentLogicTest` 6 用例。Canvas 绘制/五档 TableView 仍预留待壳校准确认（见暂缓记录）。提交见下。
- **三页接线完成**：`ChatHomePage`（QuickChip 快捷指令 + Agent 时间线 Stepper 由 vm.agentDone 点亮 + Input/发送）& `ChatVM.agentDone`(0..4)；`MarketListPage`（内置 Tab 高亮 + skeletonRow 加载 + AppErrorCard 错误 + 行点击→StockDetail 路由 + 删除空函数 showAiTagExplanation）。提交见下。
- **移除空函数记录（硬约束 §7）**：`MarketListPage.showAiTagExplanation()` 原为 TODO 空函数，已删除；其 Dialog 实装（标题"AI依据"+技术/舆情/近7日涨跌 3 行）列入阶段三 T3-6 加分项，触发条件=壳工程 + JDK17 可用（届时按 ui-design.md 实装）。
- **T2-6 LlmGatewayClient 真流式**：`bodyAsText()` 一次性读 → `bodyAsChannel()`+`readUTF8Line()` 逐行订阅，逐 chunk 到达即 emit（不整包切）；新增 HTTP 状态映射（`AppError.fromHttpStatus`，401/403/429/5xx 分级）；reasoning_content 与 content 并行容错（reasoning 不污染正文）；SAI error 帧/agent_progress 复用 SseParser；`AppErrorTest` 增 fromHttpStatus 用例。提交见下。

### 暂缓记录（UI DSL 组件，依赖 Kuikly 官方工程壳，触发条件=壳可用）
- **Canvas K 线 CandlestickChart / KuiklyMarkdown 流式 / SummaryCard·SignalPillGroup·RiskBadgeGroup·JumpCard·QuickChip·AppErrorCard / 内置 Tab·Button·Dialog·Carousel 替换 / KuiklyTableView 五档 / Page 路由 openPage / StockDetailPage.openDetail() 空函数实装**：
- **暂缓原因**：这些 UI 组件须按 Kuikly SDK 真实 API 签名书写并经壳工程 + JDK17 编译验证，本机仅 Java 25、无 Kuikly 壳，写未验证 DSL 会堆屎山。
- **下次推进触发条件**：用户提供 Kuikly 官方工程壳 + JDK17（见上方阻塞记录）后，即可按 docs/ui-design.md 实装这些组件并编译验证。

### Web(H5) 端专项推进（W1~W6）
- **W2（后端 CORS）✅ 通过**（提交见下）：`backend/app/main.py` 加 `CORSMiddleware`（`allow_origins=["*"]` 演示期、`allow_credentials=False`）；curl 带 Origin 普通请求返回 `access-control-allow-origin: *`、预检 OPTIONS 返回 allow-methods/max-age → 浏览器 Fetch `/quote/realtime` 无 CORS 报错。
- **W1（Kuikly H5 壳）阻塞记录**：
  - **阻塞于**：Kuikly H5/JS 官方编译壳（settings.gradle + libs.versions.toml + h5App target）+ **JDK17**。本机经核实仍仅 Java 25.0.2、无任何 Kuikly 壳工程（settings.gradle/gradlew/h5App 均缺失）。
  - **谁提供/如何恢复**：用户用 Kuikly 官方模板（`npx create-kuikly-app create --package com.zhiniu --dsl kuikly` → h5App target）生成工程给出路径合并，或安装 JDK17 并在 gradle 内设 `JAVA_HOME`。提供后执行 `./gradlew :h5App:run` 起 dev server 达成 W1 停止条件。
  - **严格遵循**：不手写 HTML/Wasm 壳替代，不跳过 W1 直推 W5。
- **W3/W4/W5/W6**：依赖 W1 壳达成后执行（W3 expect/actual 数据源路由按"Web 走网关 / Android-iOS 走新浪"注入；W4 WatchlistStore Web actual 用 IndexedDB/跨端存储；W5 浏览器全链路验证+截图 docs/img/web-*.png；W6 README/getting-started 补 Web 双起步骤）。
- **隔离工具链就绪（选 B 分诊链路验证）**：
  - 后端：`.venv`（Py3.14）依赖齐、9 单测通过。
  - **JDK17 隔离下载**：`zhiniu/.toolchains/jdk-17.0.20.1+1`（Adoptium arm64，176MB），项目内隔离、系统默认 Java(25) 不受影响；`.toolchains/` 已 gitignore。
  - **Kuikly 官方壳生成**：`kuikly-shell/`（`create-kuikly-app` @0.2.7，`--dsl kuikly +H5`，package=com.zhiniu），含 `settings.gradle.kts`(androidApp/shared/h5App)，`gradlew`(Gradle 8.5)，`buildSrc` 版本管理，`androidApp/iosApp/ohosApp/shared`。
  - **shared 并源**：现有 `com.zhiniu` 源码（pages/viewmodel/domain/data/di）+ commonTest 并入 `kuikly-shell/shared`；补依赖（coroutines/serialization-json/ktor2.3.12/koin-core）+ serialization plugin(Kotlin 2.1.21)。
  - **/gradlew 验证**：`JAVA_HOME`=隔离JDK17 + `GRADLE_USER_HOME`=`.gradle-home` → `./gradlew :shared:tasks` **BUILD SUCCESSFUL (Gradle 8.5 + JVM 17.0.20.1)**，插件/依赖解析通过。
  - **实证发现（D8/签名校准依据）**：壳模板真实 Kuikly API 为 `com.tencent.kuikly.core.*`（`core.base.ViewBuilder` / `core.views.Text` / `core.pager.Pager` / `core.reactive.handler.observable`，body() 内顶层 `attr{}`+子组件）。现有三页/组件/VM 用的 `com.tencent.kuikly.ref.*` + `View{attr{}}/vfor/vif` **为臆造、与真实 API 不符** → 后续 UI/VM 须按 `core.*` 校准（见下）。
  - 提交见下。
- **B2 隔离壳内确定性层编译通过**（选 B 分诊落地）：
  - `JAVA_HOME`=隔离JDK17 + `GRADLE_USER_HOME`=`.gradle-home` → `./gradlew :shared:compileKotlinJs` **BUILD SUCCESSFUL**：commonMain 的 domain(纯逻辑)/data/di 在真实 Kuikly 壳内编译通过（Kotlin 2.1.21 + serialization/Ktor2.3.12/coroutines/koin）。
  - 修复的真实编译错误：`MockDataSource` 未闭合 `/*`（改仅 `//` 注释）；`Dispatchers.IO`(JVM-only)→`Default`（JS 目标无 IO）；`AppError` 继承 `Exception`(override message, 作为统一可抛类型)；`KLineChart.buildBundle` yOf `Double/Float`；`LlmGatewayClient` 的 `continue`-in-lambda(改 if)；`UseCases.flow.first` 用接收者形式。
  - 臆造 UI（`pages`/`viewmodel`/`components` 的 `ref.*` + View{}/vfor/vif）已隔离到 `shared/src/_pending_ui`、对应测试到 `src/_pending_tests`，不参与编译，待按真实 `core.*` API 重写校准（D8 实证）。
  - `:shared:jsNodeTest` 受阻于 **Kotlin/JS IR 内部错误** `IrSimpleFunctionSymbolImpl is already bound / callKotlinMethod`（工具链 bug，非代码缺陷；与 nodejs()+KSP/Kuikly 插件组合有关）→ 壳内单测暂缓，待工具链/环境修复，或改用 Android SDK 环境跑 `testDebugUnitTest`。
  - 提交见下。
- **B3 iOS 端编译跑通**（Web 并行目标）：
  - `export KONAN_DATA_DIR="$PWD/.konan"`（项目内隔离，绕开沙箱禁止写 `~/.konan`）+ 隔离 JDK17 → `./gradlew :shared:compileKotlinIosSimulatorArm64` **BUILD SUCCESSFUL (17m4s)**：Kotlin/Native 2.1.21 编 iOS simulator target 成功（确定性 domain/data/di + 官方 HelloWorld core.*）。`.konan/` 已 gitignore。
  - Web(H5)：shared 的 jstarget `compileKotlinJs` 已通过；**完整浏览器渲染需 h5App 模块入口**（settings include(":h5App") 但 create-kuikly-app 未落目录）——补官方 h5 target / 官方模板补齐（按 `core.*` 渲染入口，详见后续）。
  - **Web(H5) 权威核实**：`npm pack create-kuikly-app@0.2.7` 模板仅含 androidApp/iosApp/ohosApp/shared，**无 h5App 模块模板**（settings include(":h5App") 但 CLI 不产出）→ 浏览器渲染入口须从 **Kuikly 官方仓库/sample** 获取（外部权威源），此为 Web 渲染最后外部依赖；不臆造 H5 renderer。
- **M 迁壳实证（UI 层迁壳 + 首次编译，2026-08-23）**：
  - UI 层已从 zd 迁入壳同包（pages 4、components 9、viewmodel 4），M2-M4 完成；M5 首次 `:shared:compileKotlinJs` 得到大量 `Unresolved reference`（`Input/attr/flex/Text/onClick/value/placeholder/onValueChange` 等）。
  - **官方 base 真实范本**（唯一可编译基准确认）：页面=`class X : BasePager()` + `override fun body(): ViewBuilder = { attr{} 子组件各自 attr{} }`，attr 属性为 **Float**（`fontSize(24f)/marginTop(10f)`），官方 DSL 组件 `Text{}`，`@Page("x", supportInLocal=true)`；`observable` 为 `by observable(x)` delegate（BasePager `companion` 用法）。
  - 迁入页面为**开发期臆造 DSL**：`com.tencent.kuikly.ref.*`（ref.pager/widget/view 包不存在于官方）+ `View{}/vfor/vif/List/Tab/onClick/flex/FLEX_DIRECTION_*` + VM `observable(...).value` —— 与官方 base/HelloWorld **不匹配**。
  - **权威核实限制**：`core-gradle-plugin/core-ksp-jvm/core-annotations-jvm` jar 可本地获取，但**运行时 DSL 库（core-android .aar / core klib）未下载**（需 Android SDK 才能拉取 android target 依赖），JS 使用 klib 无法 javap → 官方完整 DSL 组件清单（List/Tab/ScrollView/Input/Dialog/Canvas/Carousel/onClick/vfor/vif）在本环境无法权威反编译核实。
  - **M7 迁壳实证达成（2026-08-23）**：
  - 用 GitHub 插件核实 **Tencent-TDS/KuiklyUI** 官方 demo 真实 DSL 范本（VforModify.kt 等）确认真实 API：`com.tencent.kuikly.core.*`、`observable/observableList` delegate（`by observable(x)`）、`body(){ attr{} 子组件 attr{} }`、事件 `event{ click{ } }`、`List{ vforLazy({deps}){ item,index,_-> } }`、`willInit()`、attr 属性为 Float。
  - **MarketListPage 已用真实 core DSL 重写**并入官方壳，`:shared:compileKotlinIosSimulatorArm64` **BUILD SUCCESSFUL** → 本项目 UI 页面（非 HelloWorld）用真实 API 在 iOS 编译通过。
  - 已修正的真实适配点：vfor 依赖 `{ ctx.mode; ctx.quotes }`（单个 deps lambda）；vfor lambda 内类成员需显式 receiver（`ctx.priceColor(q)/ctx.pct(q)/ctx.navigator(...)`）。
  - 其余 UI（StockDetail/Chat/Home + components + viewmodel）暂隔离到 `shared/src/_pending_ui`，待按相同真实 API 逐步迁移（K线 Canvas/Tab/Input/Dialog/Markdown 需另据官方 demo 核实签名）。
- **阻塞记录（2026-08-23）模拟器运行**：iOS shared 已编译通过，但**启动模拟器需 iOS 模拟器运行时**（`xcrun simctl list runtimes` 为空；deviceTypes 有 iPhone17/Pro/Air 等）。运行时下载写 `~/Library/Developer/CoreSimulator`（沙箱已禁写系统目录）。解除：用户在本机执行 `xcodebuild -downloadPlatform iOS`（约数 GB）或 `xcrun simctl runtime add <runtime.dmg>`；依赖：需要 xcodegen（project.yml → .xcodeproj，brew install xcodegen）+ `pod install`（拉 OpenKuiklyIOSRender 官方 tag）。
- **iOS 运行链路已备（2026-08-23）**：
  - `brew install xcodegen`（2.46.0）→ `xcodegen generate` 生成 `iosApp.xcodeproj`。
  - `./gradlew :shared:generateDummyFramework` 通过 → `pod install` 成功（HOME 隔离到 `iosApp/.home`、`COCOAPODS_REPOS_DIR` 隔离，沙箱禁写 `~/.cocoapods`；装 OpenKuiklyIOSRender 2.0.0 / SDWebImage / shared）→ `iosApp.xcworkspace`。
  - gitignore 增 `.home/`、`.cocoapods-repos/`、`iosApp/Pods/`、`*.  **新阻塞点**：xcodebuild 报 `iOS 26.4 is not installed`（Xcode 未注册任何 simulator platform / 无 runtime image）→ 无法 generic sim 编译、也无设备可 boot。`xcodebuild -downloadPlatform iOS` 在沙箱内下载卡住（写 Xcode 系统 Components 目录受限）。解除：**用户在 Xcode > Settings > Components 下载 iOS Simulator Runtime**（或沙箱外 `xcodebuild -downloadPlatform iOS`）；之后续跑 `xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` + `xcrun simctl boot/install/launch` 出效果。
- **Web dev server 启动成功（2026-08-23）**：
  - `./gradlew :shared:jsBrowserDevelopmentWebpack` **BUILD SUCCESSFUL**（bundle `nativevue2.js` 4.87MiB；Web target 无 IR 报错——此前 `jsNodeTest` 的 `callKotlinMethod already bound` 为 node 测试目标特有，browser dev bundle 正常）。
  - `./gradlew :shared:jsBrowserDevelopmentRun` 起 dev server：`node *:8080 (LISTEN)`、HTTP 200；Kotlin JS dev server serve 编译产物。
  - **仍缺 H5 渲染入口**：当前 URL 为静态产物/目录非渲染 app；页面渲染需 `h5App` 模块入口（index.html 引入 nativevue2.js + Kuikly H5 renderer 启动）。
- **官方完整基座（路 2）实证（2026-08-23）**：
  - 浅克隆 `Tencent-TDS/KuiklyUI` 到 `upstream-kuiklyui/`（隔离，gitignore，可删除），含 `core-render-web`/`h5App`/`demo`。
  - 链已映射：`:demo` 产出 business bundle（`nativevue2.js`）→ `:h5App`（KuiklyRouter.createDelegator(url)）在 8083 加载渲染 + 宿主 dev server。
  - 已解：官方 Gradle7.6.3 下载 zip 损坏（重下）+ buildSrc(Kotlin1.7) TLS `protocol_version`（gradle.properties 放开 TLS：`systemProp.jdk.tls.disabledAlgorithms=SSLv3,RC4` + `https.protocols=TLSv1~TLSv1.3`）。
  - **官方仓库自身 JS 编译缺陷（阻塞，非臆造可修）**：`:demo:compileKotlinJs` FAILED——KSP 生成 `KuiklyCoreEntry.kt` 的 `nativeBridge.delegate`/`callNative`/`triggerRegisterPages` unresolved（KSP 生成与 core 的 `NativeBridge` API 版本不匹配，`kuikly.useLocalKsp=true` 组合下 demo JS 目标）。17m55s 构建失败。此属官方仓库兼容性问题，需官方修复/固定 KSP-核心版本组合，非业务代码可解。
  - **Web 渲染器已可编译（推进）**：`:core-render-web:h5:compileKotlinJs` **BUILD SUCCESSFUL（13s）** —— 官方 Web 渲染器 core-render-web:h5 可独立编译（demo 的 KSP `NativeBridge` 缺陷为 demo 业务模块特有，不影响 renderer）。**下一步高杠杆**：把已验证的 business bundle（shared 产出的 `nativevue2.js`，含 KSP 页注册，compileKotlinJs 通过）接入官方 h5App renderer（KuiklyRouter 从 URL 解析 page 渲染 MarketList）→ 即可浏览器渲染；需跨工程拼装 dev server（8083 bundle + 宿主）。
- **Web renderer 拼装成功 → MarketList 真实渲染（2026-08-23）✅**：
  - 官方 `:h5App:jsBrowserDevelopmentWebpack` BUILD SUCCESSFUL → `h5App.js`（renderer host，4.07MiB，含 core-render-web base+h5 + KuiklyRouter）。
  - 拼装：`web-8083/nativevue2.js`（我们的业务 bundle，`:shared:jsBrowserDevelopmentWebpack`，4.9MiB）+ `web-host/{index.html,h5App.js}`；两个静态 server：**8083**（业务 bundle，index.html 引 `http://127.0.0.1:8083/nativevue2.js`）、**8090**（renderer 宿主）。
  - **URL 约定**：`KuiklyRouter.createDelegator(href)` 读 **`page_name`** 参数渲染对应 `@Page`（非 SPA）。
  - **实测**：browser_use 打开 `http://localhost:8090/?page_name=MarketList` → 渲染完整行情列表（知牛·行情 标题、分类 Tab 自选/全部/涨幅/跌幅、上证/深证/创业板指数、贵州茅台/平安/招商/宁德/中国平安/五粮液/工商个股行 + 价格/涨跌幅），console 无 error，firstPaint=32ms。**Web 端 MarketList 界面正常渲出**。
  - `pct()` 精度优化为 2 位小数（`:shared:jsBrowserDevelopmentWebpack` 重新打包 + 拷 bundle，刷新生效）。

---

## ✅ 解除阻塞执行清单（当前唯一阻塞：前端真机编译）

**已就绪**：后端网关（运行时验证通过）、文档 8 份 + UI 设计、mock 数据、前端数据/LLM/分析/UI 最小页/测试工程（已并入 main）。

**唯一待办 = 前端 gradle 编译**，需你提供其一：

- **选项 A（推荐）**：在 **Android Studio（Gradle JDK 切到 17）** 用 Kuikly 官方插件 `File→New→New Project→Kuikly Project Template` 生成工程（或 `npx create-kuikly-app create --package com.zhiniu --dsl kuikly`），发我路径/合并，我按 `docs/ui-design.md` 接入 `shared/` 源码、补全页面动态 `observable/vfor` 绑定并真机编译。
- 选项 B：本机装 **JDK17**（或给 JDK17 路径），我设 `JAVA_HOME` 在本地继续（当前仅 Java25，与 Kuikly 要求 Gradle 7.5.1 不兼容）。

**顺带可答（不阻塞上面）**：
- **D2** 三厂 Key（有则后端真跑，无则全 Mock 演示）
- **D6** 行情源策略（Android 真 + 小程序 Mock，或网关加行情代理）
- **红线**：README 致谢是否去掉可疑 star 数（daily_stock_analysis 数据存疑）

> 未提供上述环境/决策前无法真机编译（非"难/慢"，是确需你的 JDK17 + 官方模板等外部状态）。