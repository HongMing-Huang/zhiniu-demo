# 知牛 ZhiNiu · 开发问题汇总（待你决策）

> 本文档汇总从「技术方案」走向「真实开发」过程中**需要你拍板 / 信息不全**的所有问题。每一条都给出：现状 → 影响 → 可选方案 → 我的建议。你逐条处理后，我再据此继续开发，**绝不乱写**。
>
> 更新：2026-08-22

---

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

### 暂缓记录（UI DSL 组件，依赖 Kuikly 官方工程壳，触发条件=壳可用）
- **Canvas K 线 CandlestickChart / KuiklyMarkdown 流式 / SummaryCard·SignalPillGroup·RiskBadgeGroup·JumpCard·QuickChip·AppErrorCard / 内置 Tab·Button·Dialog·Carousel 替换 / KuiklyTableView 五档 / Page 路由 openPage / StockDetailPage.openDetail() 空函数实装**：
- **暂缓原因**：这些 UI 组件须按 Kuikly SDK 真实 API 签名书写并经壳工程 + JDK17 编译验证，本机仅 Java 25、无 Kuikly 壳，写未验证 DSL 会堆屎山。
- **下次推进触发条件**：用户提供 Kuikly 官方工程壳 + JDK17（见上方阻塞记录）后，即可按 docs/ui-design.md 实装这些组件并编译验证。

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