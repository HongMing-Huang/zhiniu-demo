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

## D1. 网关后端技术栈（对应 `docs/backend-llm-gateway-design.md` §3）

- **现状**：三厂商均 OpenAI 兼容，网关只需一份配置。技术栈未定。
- **选项**
  - A. **Python + FastAPI + openai SDK**（推荐）：与 TradingAgents/ai-hedge-fund 同生态，多 base_url + 流式支持最好，SSE 处理成熟。
  - B. Node/TypeScript + Express + openai SDK：前端偏好的话更顺手。
  - C. Kotlin/JVM + Ktor：与前端同语言同栈，但多厂商 SDK 支持弱，需手写较多。
- **待你选择** A / B / C。

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

## D5. Vico K 线 / 鸿蒙降级（技术方案 §4.3 已定的历史决策）

- **现状**：方案定「Android/iOS 用 Vico、鸿蒙用 Canvas 自绘」。
- **问题**：Vico 是否为 **Compose Multiplatform** 且对 **Kuikly（非原生 Compose）** 兼容，存在「渲染层冲突」风险（方案风险表也自认'低概率'）。是否允许我用 **Kuikly Canvas/Path 统一自绘 K 线**（省去 Vico 集成与多端不一致），换取更稳？还是严格按方案走 Vico+降级？
- **待你决定**：Vico 优先 / 统一 Canvas 自绘。

## D6. 行情数据源：新浪 + 缓存过期策略

- **现状**：方案选新浪主源（免 Key）。但新浪接口需 `Referer: https://finance.sina.com.cn`，小程序端受限（方案已注明走 Mock）。
- **影响**：演示时若主要用 Android 问题不大；若要小程序端真实数据，需另选腾讯行情源或走网关代理。
- **待你决定**：是否接受「Android/iOS 真实 + 小程序 Mock」？或需要我在网关加一层「行情数据代理」让多端都拿真实数据？

## D7. 范围与里程碑优先级

- **现状**：方案有 3 周 12 个 PR。当前阶段我已完成调研 + 仓库初始化（`demo/zhiniu/`，已 git init，含 README + 本文档 + 网关设计）。
- **待你决定**：上面问题处理完后，下一里程碑从哪个 PR 开始？（建议 **PR-01 工程脚手架** 或 **PR-02 网关骨架 + Sina API**）

---

## 处理建议顺序（一次给我答复即可推进）

1. **D2**：给三厂商 Key（或告知暂无，那就最后通通路演 Mock）
2. **D1**：选网关技术栈（推荐 A Python/FastAPI）
3. **D4**：同意我现在去库 Kuikly 组件清单 → 定公共组件
4. **D6 / D7**：行情源策略 + 下一 PR
5. **红线项**：README 致谢是否去掉 star 数

> 未答复前我不会乱写代码；收到决策继续。