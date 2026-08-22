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
| `POST /v1/chat/completions` | 统一多模型对话（流式 SSE / JSON / 工具） |
| `GET /v1/models` | 可用模型/别名 |
| `GET /healthz` | 存活 + 各厂商 Key 配置自检 |

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