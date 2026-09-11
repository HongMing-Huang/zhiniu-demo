# 知牛 ZhiNiu · 外部 API 接入矩阵

> 版本：v0.1（2026-08-22）。所有端点/字段均来自官方文档或一线实测（标注来源），未杜撰。
> 目的：一表看清「每个 API 怎么调、字段是什么、成本、降级链」，并作为 `data/remote/` 各 Client 的实现依据。

---

## 1. 行情数据源

### 1.1 新浪实时行情（P0 主源，免费）

- **端点**：`https://hq.sinajs.cn/list=<code1>,<code2>,...`
  - 股票代码带前缀：沪 `sh600519`、深 `sz000001`；指数 `s_sh000001`（上证）、`s_sz399001`（深成）
- **请求头**：`Referer: https://finance.sina.com.cn`（必需，否则拒绝）
- **编码**：**GBK**（解析需按 GBK 解码，否则中文乱码）
- **返回**：JS 变量赋值 `var hq_str_<code>="..."`，逗号分隔字段（实测顺序，索引从 0）：

| 索引 | 含义 | 索引 | 含义 |
|:--|:--|:--|:--|
| 0 | 股票名称 | 8 | 成交量（股） |
| 1 | 今开 | 9 | 成交额（元） |
| 2 | 昨收 | 10–19 | 买一~买五（量,价 交替，量单位股） |
| 3 | 当前价 | 20–29 | 卖一~卖五（量,价 交替） |
| 4 | 最高 | 30 | 日期 YYYY-MM-DD |
| 5 | 最低 | 31 | 时间 HH:MM:SS |
| 6 | 买一价 | | |
| 7 | 卖一价 | | |

- 参考：tech-stack §行情；新浪接口多篇解析（2026-02 起 CSDN/HarmonyOS 社区）

### 1.2 新浪 K 线（P0，免费）

- **端点**：`https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sh600519&scale=240&ma=no&datalen=180`
  - `scale`：5/15/30/60 分钟、120/240（日）等
- **返回**：JSON 数组，元素 `{day, open, high, low, close, volume}`（已被 mock 对齐此结构）
- 参考：技术方案 §6.1

### 1.3 Tushare Pro（P1，可选 Token）

- **接入**：`https://tushare.pro` Token 注入，可提供财务/估值/股东等历史数据
- **成本**：免费 Token（积分制），缺失自动降级
- 参考：technical-design §6.1 / ADR-002（行情主源选新浪）

### 1.4 AKShare（P1，Python 微服务或预 dump）

- **接入**：龙虎榜/资金流/北向等更丰富字段，需 Python 中间层
- **成本**：免费

### 1.5 降级链

```
新浪实时 →（失败/无网）→ 缓存(5min) →（再失败）→ 本地 Mock（data/mock）
新浪K线 → Tushare(Token可选) → Mock
```

---

## 2. LLM / 多模型（经自建网关）

统一入口：**POST /v1/chat/completions**（OpenAI 兼容，SSE 流式）。底层三厂商真实端点与成本见下；Key 只在服务端（.env），客户端不持有。

| 厂商 | Base URL | 鉴权 | 默认模型 | 成本 | 降级 |
|---|---|:--|:--|:--|:--|
| **DeepSeek** | `https://api.deepseek.com` | Bearer | `deepseek-chat` / `deepseek-reasoner` | 按 Token 计费 | 🥇 默认 quick |
| **GLM(智谱)** | `https://open.bigmodel.cn/api/paas/v4` | Bearer | `glm-4.6` | 按 Token 计费 | flash 备选 |
| **混元** | `https://api.hunyuan.cloud.tencent.com/v1` | Bearer | `hunyuan-turbos-latest` | 按 Token 计费 | ⚠️ 迁移 TokenHub |

- 结构化 JSON：`response_format={"type":"json_object"}`；工具调用：`tools`/`tool_choice`
- 参考：`docs/backend-llm-gateway-design.md` §2（含官方链接）

---

## 3. 自建服务（本项目提供）

| 接口 | 说明 |
|---|---|
| `POST /v1/chat/completions` | 统一多模型对话（流式 SSE / JSON / 工具；缺 Key 时落地本地 Mock LLM 兜底，绝不弹 401） |
| `GET /v1/models` | 可用模型/别名 |
| `GET /healthz` | 存活 + 各厂商 Key 配置自检 |
| `GET /quote/realtime` | 实时行情代理（新浪主源，Referer+GBK+CORS 在代理层处理） |
| `GET /quote/kline` | K 线代理（新浪 getKLineData，scale/datalen） |
| `GET /news/list` | 个股/关键词资讯聚合（TTL、stale、离线快照降级） |
| `GET /news/detail` | 按已归一化 `news_id` 读取详情，不接受任意 URL |
| `POST /agent/research` | 行情、技术面、资讯、风险四阶段证据 + 第五阶段模型归纳；模型不可用时显式规则降级 |
| `POST /agent/research/stream` | 类型化研究 SSE：开始、五阶段完成、结果、显式成功/失败终止帧 |

### 3.1 网关内部接口契约（行情代理）

- **GET /quote/realtime**
  - 入参：`codes`（逗号分隔的带前缀代码，如 `sh600519,sz000001`）
  - 出参：`{ code: {symbol,name,open,prevClose,price,high,low,buy1,sell1,volume(手),amount(元),bids[[价,量]×5],asks[[价,量]×5],date,time} }`
  - 缓存：实时 TTL=3s；新浪失败 → stale 近况 → Mock JSON 兜底；命中 stale 时响应头 `X-Gateway-Stale: true`
  - 字段来源：新浪 `hq_str_<code>` 索引 0~31，GBK 解码转 UTF-8
- **GET /quote/kline**
  - 入参：`symbol`、`scale`(默认240日线)、`datalen`(默认120，≤1023)
  - 出参：`{ symbol, name, scale, data:[{day,open,high,low,close,volume}] }`
  - 缓存：日线(scale≥240)隔夜过期，分钟线 scale 秒级 TTL；失败 → stale → Mock 兜底

### 3.2 资讯与研究 Agent

- 资讯适配参考 AKShare `stock_news_em` 的公开实现，不引入 pandas/AKShare 运行时；标准库解析固定上游 JSONP，并保留 `provider/source/url/publishedAt/isStale`。
- 上游结果缓存 120 秒；失败优先返回 stale 缓存，再返回明确标注的 `offline-snapshot`。数据源必须可替换。
- `POST /agent/research` 并行聚合行情、120 根 K 线与资讯，先输出四类可审计证据，再由统一模型网关完成第五阶段结构化归纳；上游模型不可用时返回 `deterministic_fallback`，绝不把规则文案伪装成模型回答。
- `/agent/research/stream` 复用同一结果构建器；事件含 `runId/type/final`，正常以 `run_finished(final=true)`、异常以 `run_error(final=true)` 收束，客户端无需猜测流是否结束。

---

## 4. 接入成本与鉴权总表

| API | 成本 | 鉴权/约束 | 备注 |
|---|:--|:--|:--|
| 新浪实时 | 免费 | Referer + GBK | P0 主源 |
| 新浪 K 线 | 免费 | 无 | P0 |
| Tushare | 免费 Token | Token | 可选增强 |
| AKShare | 免费 | 无 | 需 Python 层 |
| DeepSeek/GLM/混元 | 按量付费 | 服务端 Key | 通过网关 |

演示期零成本策略：行情走新浪 + Mock 兜底；AI 走任一免费额度模型，缺失则 Mock LLM。**绝不弹 401**。

---

## 参考
- 新浪字段/接口解析 — tech-stack §行情；CSDN 新浪 API 全解析（2026-02/06）
- LLM 端点官方文档 — docs/backend-llm-gateway-design.md §2
- 技术方案 — docs/zhiniu-technical-design.md §6
