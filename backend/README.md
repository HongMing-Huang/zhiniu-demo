# 知牛 · LLM 多模型网关（后端）

基于 **官方真实端点、官方 openai SDK** 实现的轻量 LLM 网关，统一接入 **DeepSeek / GLM(智谱) / 腾讯混元**。

## 为什么需要网关

Kuikly 客户端直连多厂商会暴露 API Key、胶水代码膨胀、无法统一降级。网关把「密钥管理 / 模型路由 / 降级链」收敛到服务端，客户端只面对一个 OpenAI 兼容接口。

## 真实端点（官方核实 2026-08-22）

| 厂商 | Base URL | 鉴权 |
|---|---|:---|
| DeepSeek | `https://api.deepseek.com` | Bearer |
| GLM/智谱 | `https://open.bigmodel.cn/api/paas/v4` | Bearer |
| 腾讯混元 | `https://api.hunyuan.cloud.tencent.com/v1` | Bearer |

## 快速开始

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # 填入 DEEPSEEK_API_KEY 等
uvicorn app.main:app --port 8000
```

## 接口（客户端只认这一份）

- `POST /v1/chat/completions` —— 统一别名 + 流式 SSE + tool_calls 透传 + 多厂商降级
- `GET /v1/models` —— 可用模型/别名
- `GET /healthz` —— 存活 + 各厂商 Key 配置状态
- `GET /quote/realtime` / `/quote/kline` —— 新浪行情代理 + TTL/stale/离线快照
- `GET /news/list` / `/news/detail` —— 个股资讯聚合 + 来源与陈旧标记
- `POST /agent/research` —— 行情/技术面/资讯/风险四阶段研究证据
- `POST /agent/research/stream` —— 类型化 SSE 研究进度 + 结果 + 显式终止帧

外部资讯在 `app/data_sources.py` 归一化；Agent 工具、JSON 与 SSE 接口复用同一数据函数和结果构建器。网络失败会显式返回 `isStale=true` 或 `provider=offline-snapshot`，不会把快照伪装为实时结果。SSE 正常以 `run_finished`、失败以 `run_error` 明确结束，不输出模型私有推理过程。

统一别名示例：

| 别名 | 对应 | 用途 |
|---|---|:---|
| `zhiniu/quick` | deepseek-chat | 兜底/快速 |
| `zhiniu/think` | deepseek-reasoner | 强推理 |
| `zhiniu/flash` | glm-4.6 | 均衡 |

> ⚠️ 技术栈按设计文档推荐 **D1=A（Python+FastAPI）** 落地，属**临时默认**，可在你确认后切换实现而不改接口契约。完整设计见 [`docs/backend-llm-gateway-design.md`](../docs/backend-llm-gateway-design.md)。

## 运行验证（2026-08-22 实测）

```bash
# 环境：python3 venv + requirements.txt；无任何 API Key
uvicorn app.main:app --host 127.0.0.1 --port 8010
```
- `GET /healthz` → `{"status":"ok",...3 providers, key_configured:false}` ✅
- `GET /v1/models` → 返回 `zhiniu/quick·think·flash` 别名降级链 + 各厂商模型 ✅
- `POST /v1/chat/completions`（无 Key）→ SSE 优雅返回「未配置任何厂商 API Key…」+ `[DONE]`，**非 401/异常** ✅（符合"演示绝不弹 401"）

无 Key 时 LLM 文本走优雅降级，但行情/资讯/研究证据仍可真实运行。全量单测：`.venv/bin/python -m unittest discover -s tests -v`。
