# 知牛 ZhiNiu · LLM 多模型网关后端设计

> 文档版本：v0.2（已实现主链路，持续迭代）
> 日期：2026-08-22
> 状态：**主链路已实现** —— 多模型网关、工具编排、真实行情/K 线/资讯聚合、研究 Agent 均已有可运行代码
> 目标：支撑「知牛」Kuikly 客户端「一键诊股 / 问股对话 / 多空辩论」三个 AI 场景，统一接入 **DeepSeek / GLM(Zhipu) / 腾讯混元** 等多个大模型，真实 API 全栈跑通。

---

## 1. 背景与定位

当前实现位于 `backend/app/`：`gateway.py` 负责模型路由与 SSE，`tools.py` 负责 7 个工具契约，`data_sources.py` 归一化外部资讯，`agent.py` 先生成可审计的四类研究证据，再通过统一网关执行第五阶段结构化归纳。无可用模型或上游失败时，会显式返回 `deterministic_fallback`，行情、K 线与资讯工具仍继续使用真实数据源，规则输出不会冒充模型回答。

技术方案原设计（`zhiniu-technical-design.md` §6.2）把腾讯混元直接接在 Kuikly 客户端。经评估，从 KMP 客户端直连多厂商存在三个硬伤：

1. **密钥安全**：API Key 下沉到移动端存在泄露风险，也无法在服务端统一做额度/限流控制；
2. **多模型切换零散**：每个厂商鉴权、参数、错误码各不相同，客户端胶水代码膨胀；
3. **无法统一降级**：混元挂了切 DeepSeek 要在每个端各自实现。

故采用**轻量 LLM 网关（后端）**，客户端只面对一个 **OpenAI 兼容** 的稳定接口，模型路由与密钥全部收敛在服务端。

```
Kuikly 客户端 (行情列表/详情/聊天页)
        │  POST /v1/chat/completions（OpenAI 兼容，流式 SSE）
        ▼
┌──────────────────────────────┐
│      LLM 网关 (backend)       │  密钥管理 / 模型路由 / 降级链 / 流式透传
│  DeepSeek │ GLM(Zhipu) │ 混元  │
└──────────────────────────────┘
        │ 真实厂商 API（Key 只在服务端）
        ▼
  api.deepseek.com / open.bigmodel.cn / api.hunyuan.cloud.tencent.com
```

---

## 2. 真实 API 端点核实（2026-08-22 实证）

> 以下端点均来自**官方文档**，经 Context7 官方文档库 + 腾讯云官方文档实测核实，非杜撰。这是「知牛」真实接入的硬依据。

### 2.1 DeepSeek（官方，Context7 `/websites/api-docs_deepseek_zh-cn`）

| 项 | 值 |
|---|---|
| Base URL | `https://api.deepseek.com` |
| 对话端点 | `POST /chat/completions` |
| 鉴权 | `Authorization: Bearer <DEEPSEEK_API_KEY>` |
| 兼容 | 完全 OpenAI 兼容（可直接用 openai SDK，仅改 base_url/key） |
| 模型 | `deepseek-chat`（V3 系列）、`deepseek-reasoner`（R1 思维链）、新 `deepseek-v4-pro` |
| 结构化输出 | `response_format={"type":"json_object"}` |
| 工具调用 | `tools` + `tool_choice` |
| 流式 | SSE（`stream:true`） |
| 参考 | https://api-docs.deepseek.com/zh-cn/api/create-chat-completion |

### 2.2 GLM / 智谱（官方，Context7 `/metaglm/zhipuai-sdk-python-v4`）

| 项 | 值 |
|---|---|
| Base URL | `https://open.bigmodel.cn/api/paas/v4` |
| 对话端点 | `POST /chat/completions`（v4 OpenAI 兼容） |
| 鉴权 | `Authorization: Bearer <ZHIPUAI_API_KEY>`（当前 raw key，非老式 JWT） |
| 兼容 | OpenAI 兼容 |
| 模型 | `glm-4` 系列、`glm-4.5`、`glm-4.6`、`glm-5`、`glm-4v`（视觉） |
| 结构化输出 | `response_format` / JSON mode 支持 |
| 工具调用 | `tools` 支持 |
| 参考 | 官方 Python SDK `zhipuai` v4（base_url 见 README 初始化示例） |

### 2.3 腾讯混元（官方，腾讯云文档 product/1729）

> ⚠️ **重要更正**：技术方案 §6.2 引用的 `hunyuan.ai.tencentcloudapi.com` 是**腾讯云 TC3-HMAC 签名版**接口，需要 SDK 做复杂签名，**不是**简单 Bearer Key。OpenAI 兼容接口是另一套域名，见下。

| 项 | 值 |
|---|---|
| OpenAI 兼容 Base URL | `https://api.hunyuan.cloud.tencent.com/v1` |
| 对话端点 | `POST /v1/chat/completions` |
| 鉴权 | `Authorization: Bearer <HUNYUAN_API_KEY>`（控制台创建） |
| 模型 | `hunyuan-turbos-latest`、`hunyuan-2.0-thinking*` / `hunyuan-2.0-instruct-*` |
| 结构化输出 / FC | OpenAI 兼容参数 + 混元自定义参数（如 `enable_enhancement`） |
| 流式 | SSE |
| ⚠️ 迁移 | 官方提示将逐步迁移至 **TokenHub**，原平台不再新增模型能力（不影响已购） |
| 参考 | https://cloud.tencent.com/document/product/1729/111007 |

### 2.4 兼容性结论（网关设计的根）

三厂商**全部原生 OpenAI ChatCompletions 兼容**，字节级差异极小（混元多了自定义参数、GLM/DeepSeek 是纯闭源 OpenAI 子集）。因此网关核心可以只维护一份「厂商 → base_url + api_key + 模型别名」的配置，业务层无差异代码。这是本设计能「轻量」的根本原因。

---

## 3. 后端技术栈（提案，见 DEVELOPMENT-ISSUES 决策项 D1）

因三厂商都提供官方 openai SDK，网关推荐选型，按优先级：

| 方案 | 语言/框架 | 优势 | 风险 | 备注 |
|---|---|:---|:---|:---|
| **A. Python + FastAPI**（推荐） | Python/FastAPI + openai SDK | 与参考项目生态一致（TradingAgents/ai-hedge-fund 均 Python+FastAPI）；openai SDK 原生支持多 base_url + 流式 | 需管理 Python 环境 | 各角色 Prompt 处理也方便 |
| B. Node/TypeScript + Express | Node + openai SDK | 生态熟，SSE 处理直观 | 结构性 JSON Schema 强校验弱于 Python | — |
| C. Kotlin/JVM + Ktor | Kotlin/Ktor | 与前端同语言，团队栈统一 | 多厂商 SDK 支持弱，需手写 | 网关可复用的开源包少 |

> 默认取 **A**。若团队更偏前端/同栈，可切 B/C，接口契约不变，仅换实现。

---

## 4. 网关对外 API 契约（客户端只认这一份）

客户端永远面向**这一份 OpenAI 兼容协议**，与底层厂商无关。

### 4.1 `POST /v1/chat/completions`

请求（透传 OpenAI 标准字段 + 一个扩展字段指定路由）：

```json
{
  "model": "zhiyiu/deepseek-chat",        // 见 §4.3 模型别名
  "messages": [
    { "role": "system", "content": "你是知牛股票助手……" },
    { "role": "user",   "content": "茅台现在能买吗？" }
  ],
  "stream": true,
  "temperature": 0.7,
  "tools": [ /* 诊股工具 Schema，见技术方案 §6.3 */ ],
  "tool_choice": "auto",
  "extra_body": {}                         // 可选：透传单个厂商特有参数
}
```

响应：

- `stream=false`：标准 `chat.completion` JSON
- `stream=true`：SSE `data:` 增量块，`tool_calls` 事件一并透传，与 OpenAI 一致，客户端用 Kuikly 流的 `kuiklyMarkdown` 逐字渲染

### 4.2 `GET /v1/models`

返回网关已配置可用模型列表（含别名 → 厂商 → 是否启用），供客户端设置页 / 快捷指令展示。

### 4.3 `GET /healthz`

网关存活 + 各厂商连通性状态，用于演示前自检。

### 4.3 模型别名约定

为避免客户端耦合厂商名，网关定义统一别名，客户端只引用别名：

```
zhiniu/quick    → deepseek-chat          （兜底/快速模型）
zhiniu/flash    → glm-4.6 或 deepseek-chat
zhiniu/think    → deepseek-reasoner / hunyuan thinking（强推理）
zhiniu/vision   → glm-4v                 （预留）
```

路由规则：请求里的 `model` 别名 → 网关查配置 → 替换为真实厂商模型名 + 对应 base_url + key。

---

## 5. 网关核心能力

### 5.1 多厂商注册表（配置驱动）

`providers.yaml`：

```yaml
providers:
  deepseek:
    base_url: https://api.deepseek.com
    api_key_env: DEEPSEEK_API_KEY          # Key 只在服务端，用环境变量注入
    models: [deepseek-chat, deepseek-reasoner, deepseek-v4-pro]
  glm:
    base_url: https://open.bigmodel.cn/api/paas/v4
    api_key_env: ZHIPUAI_API_KEY
    models: [glm-4, glm-4.5, glm-4.6, glm-5]
  hunyuan:
    base_url: https://api.hunyuan.cloud.tencent.com/v1
    api_key_env: HUNYUAN_API_KEY
    models: [hunyuan-turbos-latest, hunyuan-2.0-thinking]
aliases:
  zhiniu/quick: deepseek/deepseek-chat
  zhiniu/think: deepseek/deepseek-reasoner
defaults:
  provider_order: [deepseek, glm, hunyuan]   # 降级顺序
  fallback: mock_all                          # 全挂时走本地 Mock LLM
```

### 5.2 降级链（对齐技术方案「演示绝不弹 401」）

```
请求 → 首选厂商 → [限流/4xx/5xx/超时] → 下一厂商 → … → 本地 Mock LLM（返回预置 JSON）
```

- 每个厂商调用包一层 `try/catch`，`401`=Key 无效（不降级，直接报配置错误提示）；`429/5xx/超时`=触发降级。
- **JSON 模式**：请求带 `response_format=json_object` 时，各厂商解析失败则统一「重试 1 次 → 降级纯 Markdown 文本」，保证 AI 卡片永不白屏。

### 5.3 密钥安全

- 各厂商 Key 只存在于网关服务端环境变量 / 密钥管理（`.env`，不提交 git）。
- 网关自身对客户端用一份**网关 Key**（`GATEWAY_API_KEY`）做 Bearer 鉴权，防止未授权调用，也隔离厂商 Key。
- `.env.example` 提供占位。

### 5.4 功能调用（Function Calling）统一

技术方案 §6.3 的 4 个工具（`get_realtime_quote` / `get_kline` / `get_financials` / `search_news`）定义在网关服务端，按需注册进 System Prompt；LLM 返回 `tool_calls` → 网关**内部**完成取数（复用行情数据网关）→ 结果回灌 → 模型归纳最终 JSON。客户端只需流式接收最终结果，不感知工具链。（取数层的行情 API 沿用技术方案 §6.1 新浪/新浪K线/降级链。）

### 5.5 多 Agent 进度时间线

`POST /agent/research/stream` 已提供类型化 SSE。事件固定为 `run_started → stage_completed × 5 → result → run_finished`；前四阶段是行情、技术面、资讯、风险证据，第五阶段是模型归纳或显式规则降级。失败必须以 `run_error` 结束。每帧含同一个 `runId`，终止帧含 `final=true`，反向代理禁用缓冲与缓存。事件只表达工具阶段、数据源和结果，不暴露模型私有推理文本。

当前 Kuikly 客户端以 `POST /agent/research` JSON 结果作为跨端稳定基线，并用相同五阶段标签展示短时进度；SSE 契约供后续支持流式响应的平台接入、断线重连和精确阶段状态。两条接口复用同一 `_build_report`，避免流式与非流式结果漂移。

---

## 6. 网络拓扑与多端

```
Android / iOS / 鸿蒙 / Web
      │ (https)
      ▼
LLM 网关（单实例，可部署在任意云/本地）
      │
      ├── DeepSeek  ── https://api.deepseek.com
      ├── GLM       ── https://open.bigmodel.cn/api/paas/v4
      └── 混元       ── https://api.hunyuan.cloud.tencent.com/v1
```

> 本地开发：网关本地 `uvicorn` 起在 `http://localhost:8000`，客户端 `BASE_URL` 指向它；演示可部署 Vercel/Railway/阿里云函数等，规避客户端出网限制（如小程序端）。

---

## 7. 待确认 / 决策项

见根目录 [`DEVELOPMENT-ISSUES.md`](../DEVELOPMENT-ISSUES.md)，与本设计逐条映射的编号为 **D1（技术栈）、D2（模型别名/默认路由）、D3（部署目标）**。

---

## 8. 参考来源

- DeepSeek API 官方文档 — https://api-docs.deepseek.com/zh-cn/api/create-chat-completion
- 智谱 GLM 官方 Python SDK（base_url=open.bigmodel.cn）— https://github.com/metaglm/zhipuai-sdk-python-v4
- 腾讯云混元 OpenAI 兼容接口 — https://cloud.tencent.com/document/product/1729/111007
- 腾讯云混元原 TC3 签名版说明 — https://tengxunhunyuan.apifox.cn/6148062m0
- 技术方案 — `docs/zhiniu-technical-design.md` §6.2 / §6.3
