# 知牛 × SaiRen（塞壬）同课题对标研究

> 版本：v1.0（2026-09-13）。对象：https://github.com/1250422131/SaiRen（同课题组，公开仓库）。
> 性质：纯调查与优化方案，**未改动任何业务代码**。所有结论均来自其 README、源码结构与预览截图的实际核查。

---

## 1. 对象概况

| 维度 | SaiRen（塞壬） | 知牛（我们） |
|:--|:--|:--|
| 定位 | "简洁股票查看 APP"，移动端优先 | AI 股票应用，桌面终端密度 + H5 主演示 |
| 技术底座 | KMP + Kuikly（**Compose DSL**） | KMP + Kuikly（**Kuikly DSL**，课题主推） |
| 端 | Android / iOS / H5 / **微信小程序** / 鸿蒙（前三者真机测试过） | H5（主演示）+ Android（APK 构建通过）+ iOS/鸿蒙壳就绪 |
| 后端 | Node.js 22 + Hono + Drizzle + SQLite（生产 **Neon Postgres**），**已部署 vercel** | FastAPI + SSE，本机运行（未部署） |
| AI | DeepSeek 单轮分析 + 工具回填 | 多 Agent 研究管线（多空辩论/风控/归纳）+ 类型化 SSE 阶段流 |
| 市场 | A股/B股/北交所/美股/沪深/港股 6 市场 | A 股 + 东财人气榜 |
| 账户体系 | 注册/登录（scrypt + token SHA-256）+ 服务端会话历史 | 无登录，会话本地 |
| 交付 | README 视觉化极佳；视频"等待制作中" | 文档齐全；视频未录（同样未做） |

## 2. 对方做得好的（带证据）

1. **仓库营销力（第一印象）**：README 有品牌横幅（海妖+K 鸟双 logo）、四页手机截屏拼图（浅/深色）、三台真机同屏图、架构树、接口表、"设计亮点"章节。评委 30 秒内即可建立"完成度很高"的印象。
2. **AI 分析结构化五件套 + 点位卡片**：`analysis` 固定含 结论 / 买入区间 / 卖出减仓区间 / 趋势 / 风险(分级) / 3-6 条信号 / 总结 / 免责声明，直接映射成卡片（红绿底色 chip、风险五档色）。"买卖观察区间"是其最强卖点——把 AI 建议变成一眼可读的可视化卡片。
3. **防幻觉协议（后端数据权威）**：客户端只提交证券标识，服务端自行拉行情/F10/日 K 并**直接回填卡片，模型不得填数据**；聊天中的证券引用**必须来自本轮搜索工具**，拒绝模型猜测代码；工具失败回传错误让 AI 说明，禁止回填未查询的数据卡。
4. **工程健壮性**：同股票分析 1h 缓存 + 进行中任务并发去重（202 + 轮询）；聊天 `requestId` 幂等；JSON 空白响应单独重试一次（关 response_format 再试、仍严格校验）；A 股行情东财失败切腾讯备用源（含 GBK 解码、单位统一为元）。
5. **主题与文字体系**：`SRTextView` 继承官方 TextView——未显式设色则自动绑主题色、字重 ≥600 自动切 MiSans-Bold 字族（MiSans Regular/Medium/Bold 三档随端注册）；语义色板含涨跌 chip 底色、五档风险色、错误底色、扫光高亮。
6. **组件化彻底**：SR 前缀组件库（Button/Card/Tabs×3/NavBar/Divider/AutoError/SweepLightImage）+ chat 子库（Message/Composer/Cards/TradeAdviceCard）+ screen 子库（OHLCVInfo/DetailTopInfo/AIProposal/KChart/ExpandableInfoCard）。
7. **MVI 页面架构**：每页 Store（State/Intent/Effect），加载/空/错/成功边界清晰。
8. **多市场广度**：一次搜索同时展示同名港美股（卡片标注市场与币种）。

## 3. 正面对照：知牛的强项（不要被带偏）

1. **AI 研究深度**：我们的多 Agent 辩论管线（行情→技术→财务→资讯→风险→多空→研究经理→交易员→风控→归纳）+ 四阶段实时进度 + 来源可追溯，**比对方单轮 DeepSeek 分析深一个量级**——这是课题"AI 场景设计 25%"的核心差异化，保持。
2. **K 线与行情纵深**：缩放/拖拽/捏合/十字线 OHLCV/MA/MACD/RSI/分时 VWAP/五档（对方仅基础分时与日 K）。
3. **桌面终端密度 + 四断点响应式**：他们纯手机排版，我们是 1280–1920 终端布局（课题演示以 H5 为主时是优势）。
4. **数据源纵深**：新浪+东财+腾讯多源、TTL/stale 语义、SSRF 白名单（对方 README 未提及安全边界）。
5. **A股红涨绿跌本土化**：对方用绿涨红跌（美式），我们坚持 A 股惯例——不要学。
6. **测试**：后端 53 单测 + 前端 42 断言（对方仓库未见测试说明，仅 `pnpm test` 一句话）。

## 4. 优化方案（按优先级；本轮不动代码）

### P0 —— 直接影响评分观感，纯低成本

| # | 优化项 | 具体做法 | 对应评分 |
|:--|:--|:--|:--|
| 1 | **README 视觉化改造** | 补三张图：①品牌横幅（知牛 logo + 一句定位）；②四页 H5 截屏拼图（浅/深）；③Android 真机 + H5 同屏图（APK 已就绪）。补"设计亮点"小节（多 Agent 辩论/Markdown 渲染/官方字体接入/诚实降级）与"接口概览"表 | 交付物第一印象 |
| 2 | **买卖观察区间卡片** | AiBlocks 新增 `TradeAdviceCard`：买入区间 / 卖出减仓区间 / 依据 / 风险，红绿浅底 chip（Palette 增 rise/fallBackground 两个 token）；后端 synthesis 已有 trader entry/stop 字段可直接映射 | AI 场景 25% |
| 3 | **风险分级可视化** | synthesis.risk 从文本升级为 level（低/中低/中/中高/高）+ warning，前端五档色 chip | AI 场景 25% |
| 4 | **部署上线** | 后端（FastAPI）部署到 Render/Railway 免费档 + H5 静态托管（vercel/gh-pages）；前端 gateway 地址支持线上覆盖（已有 `?gateway=` 参数）。评委可点击即玩 | 功能完整性 40% / 体验 |
| 5 | **录制演示视频** | 对方也未完成——我们抢先完成即是净优势（脚本已就绪） | 交付物硬项 |

### P1 —— 工程与协议（择优实施）

| # | 优化项 | 具体做法 | 备注 |
|:--|:--|:--|:--|
| 6 | 防幻觉协议显式化 | 把「证券引用必须来自本轮搜索工具、卡片数据由服务端回填、模型不得填数」三条写进 gateway/agent 的系统提示与校验（我们已有工具回填，缺显式拒绝路径） | 低成本，评委问及 AI 可信度时的标准答案 |
| 7 | 分析缓存 + 并发去重 + requestId 幂等 | 后端 `/agent/research` 增加同 symbol 1h 缓存与进行中任务复用；聊天提交加 requestId 去重 | 省 token、演示不重复等待 |
| 8 | 对话内 K 线卡片 | AI 消息新增 `AiBlock.KLine`（复用 Canvas 组件渲染近 60 根），对应对方 stock_kline | 增量工作，Canvas 已就绪 |
| 9 | ZNTextView 全局文字组件 | 借鉴 SRTextView：子类化 TextView，默认主题色/字体，字重→字族映射；顺带解决"等宽用于纯中文值"一类问题 | 低风险重构 |
| 10 | 涨跌 chip 底色 token | Palette 增加 rise/fallBackground（浅红/浅绿底），列表涨跌幅、AI 指标改 chip 样式 | 低成本高感知 |
| 11 | AutoError 统一错误组件 | 六态中的 Error/Empty/Retry 收敛为单组件（对方 SRAutoError 同思路） | 与现有六态机兼容 |
| 12 | 多市场搜索（港美股） | 东财接口天然支持；先做搜索结果多市场展示 + 详情报价，K 线/资料降级明示 | 演示冲击大，但注意别摊薄 A 股主链路 |

### P2 —— 不建议照搬 / 视余力

- **登录注册体系**：课题不要求；会话本地已可演示；成本高、评审加分存疑。不建议。
- **core 多模块拆分（core:network/chart/common）**：更"教科书"，但会动构建；我们已用包分层 + Repository 接口表达同等意图。若评委追问分层，以现有 C4/ADR 应答。
- **MVI 全量重构**：仅建议对 AI 聊天页试点 Store 模式，其余页保持 observable。
- **微信小程序端**：平台覆盖加分以鸿蒙叙事即可，小程序性价比低。
- **Compose DSL**：课题官方主推 Kuikly DSL（AGENTS 硬约束），不换。
- **绿涨红跌**：不学，坚持 A 股红涨绿跌。

## 5. 一页结论

SaiRen 的强项是「**产品化包装 + 工程规范叙事 + 防幻觉后端协议**」；知牛的强项是「**AI 研究深度 + 行情纵深 + 桌面终端密度**」。两者几乎互补。

建议的吸收顺序：先做零代码成本的 P0-1（README 改造）与低成本高感知的 P0-2/3（点位卡片、风险分级），再完成 P0-4 部署与 P0-5 视频——这五项完成后，"完成度第一印象"与"AI 场景"两个维度即可对齐甚至反超对方；P1 按余力择取 6/7/9/10；P2 全部缓行。

> 资料来源：SaiRen 仓库 README、`static_server/serve/README.md`、`theme/Color.kt`、`component/SRText.kt`、`component/` 与 `ui/` 目录树、`core/network/model/StockAiAnalysis.kt`、`docs/asstes/` 三张预览图、`https://sai-ren.vercel.app` 实测（返回其线上 API JSON）。
