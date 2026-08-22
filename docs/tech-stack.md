# 知牛 ZhiNiu · 技术栈定稿

> 文档版本：v1.0（定稿，2026-08-22）
> 目的：回答「技术栈到底怎么用、选什么」。逐层定稿，标注真实来源/版本与已落地/待验证。
> 总原则：**前端 Kuikly + 多用公内置组件，后端轻量网关，全真实 API，图标用公共资源不自绘 SVG**。

---

## 1. 总览

```
┌──────────────────────────────────────────────────────────────┐
│ 前端 shared/commonMain（Kotlin 2.2 · K2）· Kuikly 自研 DSL      │
│  Pager/@Page · 内置组件 List/Text/View/Carousel/Tab/Dialog      │
│  StateFlow 四态 · Koin DI · Ktor3 Client · kotlinx.serialization│
│  SQLDelight(自选/会话) · Canvas 自绘 K线 · Material Symbols 字体  │
└──────────────┬───────────────────────────────────────────────┘
               │ POST /v1/chat/completions（OpenAI 兼容，SSE）
┌──────────────▼───────────────────────────────────────────────┐
│ 后端 backend/（Python · FastAPI · openai SDK）· LLM 网关          │
│  DeepSeek(api.deepseek.com) · GLM(open.bigmodel.cn) ·           │
│  混元(api.hunyuan.cloud.tencent.com/v1) · 降级链               │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. 分层定稿表

| 层 | 选型 | 怎么用（真实依据） | 版本/来源 | 状态 |
|---|---|:---|:---|:---|
| **前端框架** | **Kuikly**（KMP 跨端，原生渲染） | 一套 Kotlin 六端；minic 90% 代码在 commonMain；优先内置组件 | GitHub `Tencent-TDS/KuiklyUI`（3.4k★）；官方 kuiklyDSL.mdc | ✅ 已选 |
| **UI DSL** | **Kuikly 自研 DSL**（主） | `@Page`/`Pager`/`body():ViewBuilder`/`attr{}`；Compose DSL 仅作对齐备用 | kuikly.tds.qq.com/ComposeDSL/allApi.html | ✅ 已选 |
| **组件** | **Kuikly 内置组件**优先 | `List/Carousel/Tab/Dialog/Input/Button/Canvas/AI Chat`；社区 `KuiklyMarkdown`(流式)/`KuiklyTableView`(表格) | `docs/kuikly-common-assets.md` | ✅ 已选 |
| **HTTP（客户端）** | **Ktor Client** | commonMain 用 Ktor 3（OkHttp/Darwin engine），ContentNegotiation + kotlinx-json；`ignoreUnknownKeys` | Ktor 3.4.0（JetBrains 2026-01） | ✅ 已选 |
| **JSON** | **kotlinx.serialization** | `@Serializable` DTO 反序列化 | Kotlin 2.x 内置 | ✅ 已选 |
| **状态管理** | **Kuikly 原生 `observable`/`observableList`** | 页面 `attr{}` 内用 `vfor/vif/vbind`；聊天流式 `Flow<StreamChunk>` | kuiklyDSL.mdc | ✅ 已选（2026-08-22 由 StateFlow 调整为框架原生） |
| **DI** | **Koin（for KMP）** | 单入口 `App.kt` 注入；network/repository/viewModel 分模块 | Koin 官方 KMP 支持 | ✅ 已选 |
| **本地存储** | **SQLDelight** | `.sq` 定义表 → 生成类型安全接口；AndroidSqliteDriver/NativeSqliteDriver | SQLDelight；ADR-005 | ✅ 已选 |
| **K 线** | **Kuikly `Canvas` 自绘**（统一，不用 Vico） | 封装 `CandlestickChart` 组合组件；**Vico 为标准 Compose 库，与 Kuikly 非标准 Compose 集成未验证，弃用** | `docs/kuikly-common-assets.md` §2 | ✅ 定（替代原 Vico） |
| **图标** | **Google Material Symbols 字体**（主） | 字体渲染跨渲染层通用；不自绘 SVG | fonts.google.com/icons；`docs/kuikly-common-assets.md` §3 | ✅ 定 |
| **后端网关** | **Python · FastAPI · openai SDK** | 对前端暴露 OpenAI 兼容 `/v1/chat/completions`；复用官方 SDK 换 base_url/key；多厂商降级 | D1=A；`backend/` 已合并 main | ✅ 已落地 |
| **LLM 多模型** | **DeepSeek / GLM / 腾讯混元** | 三厂商均 OpenAI 兼容；统一别名 `zhiniu/quick·think·flash`；密钥只在服务端 | 官方文档核实，见 `docs/backend-llm-gateway-design.md` §2 | ✅ 已落地 |
| **行情数据** | **新浪 hq.sinajs.cn（P0）+ 新浪 K 线 + 降级** | 免 Key、覆盖广；`Referer: finance.sina.com.cn`；降级 Tushare→AKShare→Mock | 技术方案 §6.1 | ✅ 已选 |
| **性能/异步** | **Kotlin 协程 + Flow** | 全网异步，流式响应，不阻塞渲染 | Kotlin 2.x | ✅ 已选 |

---

## 3. 关键「怎么用」说明（避免误用）

1. **Kuikly ≠ 标准 Compose Multiplatform**：不能直接塞 JetBrains Compose 生态组件。这就是：
   - K 线弃用 Vico → 用 Kuikly `Canvas` 自绘；
   - 图标走**字体**而非 ImageVector 库（除非 D8 实测支持）；
   - 尽量用 `@Page`/内置组件，而非 external 组件。
2. **前端连后端只认一份 OpenAI 兼容协议**：`model` 用统一别名，客户端不感知厂商。
3. **后端 openai SDK**：三厂商全部通过 `base_url` + `api_key` 切换，无手写 HTTP，遵循「用公共组件」。
4. **行情降级链**：`Sina → Tushare(Token 可选) → AKShare → Mock`，演示绝不弹 401。

---

## 4. 已异议/替换的初代选型

| 原技术方案选型 | 定稿 | 原因 |
|---|---|:---|
| 腾讯混元为主、DeepSeek 备选（直连客户端） | **网关统一接入三模型，DeepSeek 作默认 quick** | 密钥安全 + 多模型切换 + 混元在迁 TokenHub |
| Android/iOS 用 Vico K 线、鸿蒙 Canvas | **统一 Kuikly Canvas 自绘** | Vico 与 Kuikly 非标准 Compose 集成未经证实，跨端一致性差 |
| 客户端直连混元 | **经 LLM 网关** | 密钥收敛服务端、统一降级 |
| 自绘 SVG 图标 | **Google Material Symbols 字体** | 公共资源，跨渲染层可用，零自绘 |

---

## 5. 待运行验证（非选型分歧，实测定案）

- **D8**：Kuikly DSL 是否直接支持 `ImageVector`（若支持可加 CMP 图标库；默认仍走字体）
- **K 线自绘**细节（Canvas 坐标/缩放/十字游标）
- 各端壳工程编译跑通（用官方模板生成 gradle）

---

## 6. 参考（真实来源）
- Kuikly 官方文档/规范 — kuikly.tds.qq.com · github.com/Tencent-TDS/KuiklyUI-AI/blob/main/rules/kuiklyDSL.mdc
- Ktor 3.4.0 — jetbrains.com/kotlin (2026-01)
- KMP 生态（Koin/SQLDelight/kotlinx.serialization）— calmops.com · youngju.dev 综述
- LLM 端点 — docs/backend-llm-gateway-design.md §2（官方核实）
- 组件/图标 — docs/kuikly-common-assets.md