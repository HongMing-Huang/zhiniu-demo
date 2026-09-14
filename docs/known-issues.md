# 知牛 · 已知问题与风险清单

> 单一事实源：运行/构建/测试中实际暴露的问题，按「已修复 / 已知非阻塞 / 待办」三档维护。
> 约定：每条含 现象 → 根因 → 影响 → 状态；修复后在对应条目标注日期与提交。

最后核对：2026-09-14（三端构建 + 后端 106 测试全绿当日）

---

## 一、已修复

### 1. 后端离线兜底依赖已废弃的根 `shared/` 目录（本轮结构整理时暴露）

- **现象**：删除根 `shared/` 旧脚手架后，`test_search_offline_falls_back_to_local_snapshot` / `test_failed_fetch_mock_not_cached` 2 项测试转红；断网时行情不再回退离线快照。
- **根因**：`backend/app/quote.py` 的 `_MOCK_DIR` 用相对路径指向仓库根 `shared/src/commonMain/.../data/mock`（8 月废弃的 PR01 脚手架），而真实 mock JSON 在 `kuikly-shell/shared/src/commonMain/.../data/mock`。属于隐藏跨目录耦合。
- **影响**：断网演示时行情/K 线/搜索不再有离线兜底，直接空数据。
- **状态**：✅ 已修复（2026-09-14）——`_MOCK_DIR` 改指 `kuikly-shell/shared/...`，106/106 全绿。

### 2. ⟦TOOL⟧ 指令展示名为裸代码

- **现象**：模型直接输出代码（如 sh600519）时，`name` 字段回退为代码本身，对比页/反馈卡显示「打开对比 sh600519 × sz300750」。
- **根因**：`_resolve_symbol_arg` 对合法代码直接放行、不回查名称。
- **状态**：✅ 已修复（2026-09-14）——新增 `_display_name()` 兜底，解析后从真实行情补名称；实测「打开对比 贵州茅台 × 宁德时代」。

### 3. `push2.eastmoney.com` 拒连导致 5 个能力静默降级 mock

- **现象**：行业板块领涨股为编造值（「半导体·龙头」）、条件选股/资金流/诊股快照字段缺失。
- **根因**：本网络环境 `push2` 域名 RemoteDisconnected；失败被 `except: pass` 静默吞掉。
- **状态**：✅ 已修复（2026-09-14，`de3957c`）——全部切换 `push2delay.eastmoney.com`（东财网页端同款公开端点），板块/选股/资金流/快照字段恢复真实。

### 4. K 线缓存污染：离线快照写入 6h 长缓存

- **现象**：一次瞬时网络失败后，日 K 连续数小时返回 2026-07/08 的假数据（且 datalen 不同键表现不一致）；过期 stale 兜底也退化为快照。
- **根因**：`quote_kline` 把失败回退的 mock 与真实结果写同一缓存、同一 TTL（日线 6h）。
- **状态**：✅ 已修复（2026-09-14，`de3957c`）——只有「真实源+非空+非 stale」入缓存；失败后下轮必重试；stale 不续期。4 项回归测试锁死。

### 5. realtime 缺 `changePct` 字段

- **现象**：AI 快照注入涨跌幅 None（模型被迫答「无法判断涨跌」）；诊股证据/双股对比规则基准拿不到涨跌。
- **状态**：✅ 已修复（2026-09-14，`de3957c`）——`_parse_sina` 现算 + `_chat_messages` 兜底；实测模型引用「涨跌幅 -0.78%」。

### 6. 多轮对话历史中 AI 侧内容恒为空

- **现象**：`sendGeneralQuestion` 历史投影用 `it.text`，AI 消息内容都在 blocks，后端只收到用户侧问题（多轮失忆）。
- **状态**：✅ 已修复（2026-09-14，`4cf3cb6`）——`ChatArchive.aiTextOf` 块文本投影。

### 7. 外观偏好不持久化 / 聊天会话不持久化

- **状态**：✅ 已修复（2026-09-14，`4cf3cb6`）——`persistHook` 收口 AppBasePage；ChatArchive 归档（会话+消息）。

### 8. 前端行情数据硬编码在 Kotlin 源码里

- **现象**：12 只股票池 / 3 个指数 / 市场宽度 / 默认自选 / 默认对比对 / 详情页行业文案全部以字面量散落在 `MockMarketRepository`、`Watchlist`、`ComparePage`、`ProfilePage`、`StockDetailSections`；与 `mock_quotes.json` 双源维护。
- **状态**：✅ 已修复（2026-09-14）——数据抽到 `shared/src/commonMain/data/mock_market.json` 单一事实源，Gradle 任务 `generateMockMarketData` 编译期生成 Kotlin（Android/iOS/ohos/JS 四端构建脚本均已接线），业务代码只读解析结果；详情页行业改用真实 `fundamentals.industry`。离线兜底能力不变（无网时股票池/指数/宽度照常渲染）。

### 9. 数据源单点：realtime 只依赖新浪、日K 只依赖新浪

- **现象**：新浪不可达时 realtime/日K 直接落离线快照（假数据兜底）。
- **状态**：✅ 已修复（2026-09-14）——realtime 新浪失败自动降级腾讯 gtimg（`_parse_tencent_quote` 同构解析）；日K 新浪失败自动降级东财 push2delay kline。三源链 = 主源 → 备源 → 离线快照（标注 stale），备用源为真实数据可正常入缓存。

---

## 二、已知非阻塞（不阻塞演示与交付，按影响排序）

### N1. LLM 中转站抖动：单请求耗时波动大

- **现象**：gemai 免费池高峰偶发挂起；`/agent/chat` 最坏约 90s（45s 超时+1 次重试），`/agent/research` 为 7 次串行调用、实测 139s 正常 / 抖动窗口更长。
- **缓解**：网关有超时+重试；全部链路失败时诚实降级 rule-engine（显式标注，实测生效）；前端有「停止生成」与降级文案。
- **实测（2026-09-14 12:06）**：gemai 直连探测 `APIConnectionError`（0.3s 即失败，非慢而是断）——中转站整体宕机时所有 LLM 链路诚实降级，功能不白屏、数据不带假标注。
- **建议**：**换 DeepSeek 官方直连**（用户已确认计划）：`backend/.env` 加 `DEEPSEEK_API_KEY=sk-…`、注释 `GEMAI_API_KEY`、重启即生效（零代码，回归测试已锁定该路径）；演示/录屏前预热一次请求确认通道质量。

### N2. H5 页内整页跳转丢失 `?gateway=` 覆盖参数

- **现象**：带 `?gateway=http://ip:port` 进入首页后，页内路由（location.assign 整页跳转）到详情/AI 页时参数丢失，回落默认 `127.0.0.1:8000`。
- **影响**：仅影响「后端不在 8000 端口」的联调场景；生产/演示按默认 8000 部署则无感。
- **建议**：联调非默认端口时直接在目标页 URL 带参进入；或后续把 gateway 覆盖写入 localStorage。

### N3. Android 构建期 D8 metadata 警告

- **现象**：`D8: An error occurred when parsing kotlin metadata ...`（Kotlin 2.1.21 源码 vs AGP 7.4 内置 R8 版本差）。
- **影响**：仅 WARNING，APK 正常产出（7.6MB）且功能不受影响。
- **建议**：升级 AGP 可消除，非必要不动（升级有回归风险）。

### N4. 鸿蒙 HAP 未配置签名

- **现象**：`hvigor assembleHap` 成功但 WARN「Will skip sign 'hos_hap'」。
- **影响**：模拟器/设备安装需在 DevEco 的 `build-profile.json5` 配 signingConfigs（自动签名即可）；构建产物本身完整。
- **建议**：真机演示前在 DevEco Studio 一次性配置自动签名。

### N5. `jsNodeTest` clean 态编译报 `IrSimpleFunctionSymbolImpl is already bound`

- **现象**：`clean`/`rerun` 后跑 js node 测试报 KSP 事件桥重复绑定。
- **定性**：worktree 二分证实为历史提交即复现的上游 Kuikly KSP 已知限制（见 WORKLOG 2026-09-14 01:10），非业务代码引入。
- **影响**：主源集四端编译、Android/iOS/ohos 构建与运行均不受影响；增量编译态测试可跑。

### N6. 生成协程绑定页面作用域

- **现象**：AI 页的流式请求挂在页面 `lifecycleScope`，退出 AI 页生成即中断。
- **对照**：KuiklyStock 用常驻根页面桥（AIJobCenter）实现「退出页面后台继续生成」。
- **建议**：候选改进（中等工作量：请求改挂常驻根页桥）；当前「停止生成+原位重试」已覆盖主要体验缺口。

### N7. realtime 接口无 `floatMarketCap`

- **现象**：`/quote/realtime` 的 extras 只带总市值/换手/量比；流通市值仅在 `/quote/fundamentals` 返回。
- **定性**：设计取舍（ulist 字段集未含 f21），前端 StockQuote 模型也未定义该字段，无消费方。
- **建议**：如详情页需要流通市值，从 fundamentals 取（已可用）。

---

## 三、待办（P0）

### T1. 演示视频未录制（交付物三件套唯一缺口）

- 题面正式交付要求 ① 公开仓库 ✅ ② 说明文档 ✅ ③ **原型演示视频 ❌**。
- 分镜脚本已就绪：`docs/demo-script.md`；推荐主线：行情列表（实时）→ 个股详情（K线/五档）→ AI 诊股抽屉 → AI 研究问答（流式+结论徽章+KCHART）→ ⟦TOOL⟧「加自选/设预警」→ 双股对比 → 自选页预警触发。
- 注意：避开 LLM 中转站高峰（见 N1），录制前先预热一遍链路。

### T2. （候选增强）证据可核对

- 点 AI 结论定位 K 线区间并展开计算公式（对标 Study0915 EvidenceResolver）。差异化最强、工作量最大，未排期。
