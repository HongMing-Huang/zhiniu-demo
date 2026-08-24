# 知牛 ZhiNiu · Web + 后端开发执行 Prompt（v1.0）

> **用途**：将本 prompt 全文投喂给执行 AI（Codex / Claude Code / 其他会话），即可接手知牛项目的 Web 端与后端开发。
> **生成依据**：zhiniu-technical-design.md（964 行）+ llm-router-design.md（574 行）+ ai-design-guide.md（478 行）+ DEVELOPMENT-ISSUES.md（190 行）四份权威文档的执行级浓缩。
> **日期**：2026-08-24

---

## 一、角色与使命

你是一名资深全栈工程师，接手「知牛 ZhiNiu」——基于腾讯 Kuikly 跨端框架的 AI 股票 Demo。你的使命：**在 Web(H5) 端打穿全部功能，并把 Python 后端网关升级为多 LLM 统一路由管理平台**。iOS/Android/鸿蒙/macOS 全部暂停（产物保留），**只做 Web + 后端**。

成功标准 = 比赛评分四维：功能完整性 40% / 工程质量 25% / AI 场景 25% / 加分 10%。Web 打穿理论上限 ≈ 96 分。

## 二、项目现状（你接手时的状态）

**已完成（不要重做）**：
- 三页 UI（MarketList / StockDetail / ChatHome）已用 Kuikly 官方 core DSL 重写，H5 浏览器真实渲染通过（双 server：8083 业务 bundle + 8090 renderer，`?page_name=X` 路由）
- 后端 FastAPI 网关 5 路由已通：`POST /v1/chat/completions`（SSE 流式+降级链+Mock 兜底）、`GET /v1/models`、`GET /healthz`、`GET /quote/realtime`（新浪主源+五档+GBK 转码+3s 缓存+stale 头）、`GET /quote/kline`；CORS 已开；单测 9 项绿
- 纯逻辑层 + 测试：AppError / KLineChart / QuoteDisplay / SseParser / LlmGatewayClient 真流式，6 个测试文件
- 工程壳：`kuikly-shell/`（官方 create-kuikly-app）+ 隔离 JDK17（`.toolchains/jdk-17.0.20.1+1`）+ 并源完成
- 编译验证通道：`JAVA_HOME=<隔离JDK17> GRADLE_USER_HOME=.gradle-home ./gradlew :shared:compileKotlinJs`（已绿）

**关键认知（血泪教训，必须遵守）**：
- Kuikly **不是标准 Compose**，真实包名 `com.tencent.kuikly.core.*`；官方范本 = `class X : BasePager()` + `override fun body(): ViewBuilder = { attr{} 子组件 }`，属性是 **Float 字面量**（`fontSize(14f)`），状态用 `by observable(x)` delegate，列表 `List{ vforLazy({deps}){ item,index,_-> } }`
- 曾经臆造的 `com.tencent.kuikly.ref.*` + `View{}/vfor/vif` 全部是错的，已返工过一次——**禁止臆造任何未经官方 demo 验证的 API**

## 三、权威文档（动手前按序通读，冲突时以靠前者为准）

1. `docs/ai-design-guide.md` — **设计唯一权威**（Light Theme + 官方组件优先 + 扩展守规 + 验证方法）
2. `docs/llm-router-design.md` — 多 LLM 路由设计 + 19 页矩阵 + 14 家服务商 + 7 工具 + 后端 15+ 路由规划
3. `DEVELOPMENT-ISSUES.md` — 全部决策记录（D1-D8）与阻塞状态
4. `docs/kuikly-common-assets.md` — 官方组件实证清单
5. `docs/backend-llm-gateway-design.md` — 网关后端现有设计
6. `docs/ui-design.md` §0-3 — 页面区块定义（注意：附录 B 暗色版已作废，只用 Light）

## 四、技术栈与架构约束

**前端**：Kotlin Multiplatform + Kuikly 自研 DSL；Ktor Client + kotlinx.serialization + Koin；状态 `StateFlow<UiState>` 四态；分层 Page / ViewModel / UseCase / ApiClient 四层禁止跨层引用；本地 SQLDelight。

**后端**：Python 3.13 + FastAPI + openai SDK（三厂商均 OpenAI 兼容，仅切 base_url+key）；`.venv` 隔离（Py3.14）；结构 `backend/app/{main,config,gateway,quote}.py`。

**设计**：Light Theme 唯一权威。Token 速查（全部出处见 ai-design-guide.md 附录 A）：

```
页面底 #F5F7FA / 卡片 #FFFFFF / 悬停 #F0F3F7 / 描边 #E5E9F0
文字 #1F2329(主) #4E5561(次) #8A919C(提示)
涨 #D93025 / 跌 #1E8E3E（红涨绿跌，中国习惯）/ 警示 #B45309 / 品牌绿 #00A870(大元素)/#00875A(文字)
字号 7 级 12-32px / 间距 8 倍数 / 圆角 6-8-12-16 / 数字一律等宽 font-mono
```

## 五、开发任务清单（严格按序执行）

### 阶段 A · 后端升级（先做，前端依赖它）

| # | 任务 | 规格 | 验收 |
|---|---|---|---|
| A1 | **providers.json 声明式配置** | 从 config.py 的 PROVIDERS/ALIASES 迁出；14 家预置（openai/azure/deepseek/glm/硅基流动/qwen/kimi/豆包/千帆/hunyuan/openrouter/cherryin/aihubmix/dmxapi）+ 自定义槽位；每家含 baseUrl/apiKeyEnv/protocol/supportsDiscover/tier；routes 段按能力（fast/reasoning/vision）路由 | 网关启动读 JSON；旧单测 9 项全绿 + 新增配置校验/能力过滤测试过 |
| A2 | **模型自动发现** | `POST /admin/refresh-models`（GATEWAY_API_KEY 保护）：对 key_configured 厂商真实调 GET {baseUrl}/models 聚合；`GET /v1/models` 返回聚合目录 + capability（模型名启发式：*reasoner→reasoning、*vl/vision→vision、*flash/mini→fast）+ available/reason（缺 Key 不静默，置灰可见） | curl 实测：无 Key 返回 Mock；配 Key 后能拉到该家真实模型清单 |
| A3 | **Function Calling 7 工具** | tools schema：get_realtime_quote / get_kline / get_financials / search_news / screen_stocks / compare_stocks / create_alert；编排循环 MAX_TOOL_ROUNDS=5，工具失败错误回灌模型；SSE 里工具调用进度用 `event: agent_progress` 帧下发 | 聊天"看茅台 PE"跑通：模型→tool_calls→执行→回灌→最终回答 |
| A4 | **行情路由扩展** | `GET /quote/indices`（主流指数列表）、`GET /quote/sectors`（申万板块+涨跌排行）、`GET /quote/screener`（条件选股：行业/市值/涨跌幅/PE 过滤） | 三路由 curl 返回真实新浪数据 + Mock 兜底 |
| A5 | **资讯端点** | `GET /news/list`（7×24 快讯/个股新闻，可先 Mock+新浪财经源）、`GET /news/detail` | 资讯页有数据可渲 |
| A6 | **用量统计** | in-memory 聚合 + `GET /analytics/usage`（按模型/时段的 request_count、tokens、P50/P95 延迟、降级率、ProviderError 分布） | ModelSettings 用量 Tab 有数据源 |

### 阶段 B · Web 前端（后端就绪后逐页推进）

| # | 任务 | 规格 | 验收 |
|---|---|---|---|
| B1 | **Tokens.kt + 浅色基准页** | 按 ai-design-guide.md 附录 A 建 `base/Tokens.kt`（颜色/字号/间距/圆角常量）；重套 MarketListPage 浅色皮肤 | 浏览器取色器抽查 5 处全命中 Token；三段式布局成立 |
| B2 | **ModelSettingsPage**（图 2 形态） | 顶部搜索框过滤 14 家服务商 + 卡片行（logo+名称+状态徽章+配置按钮）+ `Dialog` 弹窗填 Key（Input+代理 Switch）+ 用量 Tab；Key 前端永不回传只显后四位 | 填 Key→看到该家模型列表全流程浏览器可演示 |
| B3 | **聊天页接真实 LLM** | ChatHome 输入→网关 SSE→SseParser→流式气泡；顶部模型徽章（点击进 ModelSettings 切换）；工具调用灰卡→结果绿卡；Agent 时间线点亮 | 有 Key 真流式逐字；无 Key 走 Mock 也流式 |
| B4 | **K 线 Canvas 真渲染** | 详情页蜡烛图 + MA5/10/20 + 十字游标（长按）+ 双指缩放切周期；消费已有 KLineChart.kt 逻辑层 | 蜡烛红涨绿跌、均线三色、游标跟随 |
| B5 | **搜索/指数/板块页** | StockSearchPage（联想防抖 300ms+历史+热门）、IndexListPage+IndexDetailPage、SectorBoardPage | 各页六态可演示 |
| B6 | **资讯页** | NewsListPage（Tabs+PageList+Refresh+FooterRefresh）+ NewsDetailPage（正文+相关个股标签+AI 摘要按钮） | 资讯卡片流可滚动翻页 |
| B7 | **会话与自选/预警/设置** | ChatSessionListPage、WatchlistPage（拖拽排序）、AlertSettingsPage（4 类条件+阈值）、UserSettingsPage（主题/字号/免责声明） | CRUD 完整、SQLDelight 持久化 |
| B8 | **多空辩论 DebateViewPage** | Carousel 多/空气泡轮播 + 分歧点高亮 + 「总结」收束卡；入口：诊股结果「换个角度看」 | 2 轮辩论可切换、分歧点可见 |
| B9 | **六态全验证 + 一键脚本 + 录屏** | 每页断网/空/错三手段触发验证；`scripts/dev.sh` 一键起后端+8083+8090；≤90s 录屏按 demo-script.md | 40 项 Checklist（ai-design-guide.md 附录 D）全绿 |

## 六、每项任务的 Definition of Done

1. 代码分层合规（Page 无业务 / VM 无 HTTP / UseCase 无平台 API）
2. 单测通过（后端 pytest、前端 commonTest 保持全绿，新增功能必须带测试）
3. 编译验证：`JAVA_HOME=.toolchains/jdk-17.0.20.1+1/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew :shared:compileKotlinJs` 零 error
4. Web 视觉走查：取色器抽查命中 Token、对比达标、六态可触发
5. git commit（见第八节规范）+ 更新 DEVELOPMENT-ISSUES.md 进度记录

## 七、工作方式约束

- **拿不准就查证**：组件 API 查 https://kuikly.tds.qq.com/API/ 与官方 demo（upstream-kuiklyui/demo），禁止臆造
- **小步提交**：每完成一个任务一个 commit，禁止大杂烩
- **遇到设计决策**：遵循 ai-design-guide.md；文档没覆盖的新决策 → 记入 DEVELOPMENT-ISSUES.md 待用户确认，先选保守方案推进
- **依赖外部环境的项**（iOS 模拟器运行时、Android SDK、三厂 LLM Key）：不阻塞，做好 Mock 兜底并记录阻塞，继续推进其余任务
- **扩展示例参考**：扩展组件必须 ZN 前缀 + 十项硬约束 + ≥3 页复用才登记（ai-design-guide.md §2 Step 4）

## 八、Git 提交规范

```
格式：<type>(<scope>): <一句话摘要>

type: feat / fix / docs / refactor / test / chore
scope: backend / frontend-web / shared / docs / scripts

正文（可省）：要点列表，含验收证据（curl 输出/编译结果/测试数）
禁止：混合多任务的大 commit；无验收说明的"完成"提交
```

## 九、红线（出现即返工）

1. ❌ 任何暗色背景（Light Theme 唯一）
2. ❌ 臆造 Kuikly API（`ref.*` 包名 / 未验证属性）
3. ❌ 硬编码色值/尺寸/时长（必须 Token）
4. ❌ 绿涨红跌（必须红涨绿跌）
5. ❌ Toast 报错（必须内嵌错误卡+Retry）
6. ❌ 自绘 SVG 图标（必须 Material Symbols 字体）
7. ❌ 单测变红合入
8. ❌ 声称完成但未跑编译/未做视觉走查

## 十、第一个动作

通读 `docs/ai-design-guide.md` + `DEVELOPMENT-ISSUES.md` → 执行 **A1（providers.json 声明式配置迁移）** → 提交第一个 commit。开始。
