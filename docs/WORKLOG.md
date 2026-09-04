# 知牛 · 变更日志（Work Log）

> 单一维护变更记录，防止记忆混乱。每条记录：时间 · 改了什么 · 影响/验证。
> 约定：开发中每次有实质改动即在顶部追加一行（不写无关文档）。格式：
> `YYYY-MM-DD HH:mm | 类别 | 改动描述 | 验证结果`

---

## 2026-09-04

| 时间 | 类别 | 改动 | 验证 |
|---|---|---|---|
| 17:05 | ui | 三屏浏览器验收：MarketPage（涨红/跌绿 token 生效 #F04F5F/#16B364）、StockDetail（K线 tab+五档+Canvas 899×520）、AiResearch（AI 问候+推荐问题）、主题切换（Light/Dark） | 全部通过；index.html js 引用加 ?v=2 刷新缓存 |
| 14:40 | git | 推送全部提交至 `HongMing-Huang/zhiniu-demo` 远程仓库（已建仓） | gh 推送成功，origin/main 最新 |
| 14:35 | docs | 新增 `docs/WORKLOG.md`（变更日志规范）；完善 `AGENTS.md`：§0 工作日志、GitHub 仓库信息、Kuikly 2.25.0、验证流程修正（移除不存在的 v4_final.js）、§9.1 Git 提交规范 | 文档已同步 |
| 14:10 | git | 初始化 GitHub 独立仓库 `HongMing-Huang/zhiniu-demo`（private），`origin` 关联并推送 main | gh repo create 成功，推送 HEAD→main |
| 13:55 | design | `Theme.kt` 统一规范：涨 `#F04F5F`、跌 `#16B364`、AI 强调 `#D9FF43`（默认值）；MA/RSI 指标紫保留；同步 `AGENTS.md` 色彩条款 | 编译通过 |
| 13:50 | docs | 新增 `docs/ui-redesign.md`：现状诊断 / OKX 参考 / 规范 v2 / 素材占位清单 / 新浪直连与 Agent 端到端接入研究 | 已提交 commit 4e09d82 |
| — | design | 基础提交：目标结构 + Kuikly 2.25.0 + 市场排序/筛选（commit 0f9dcce） | 编译+浏览器验证通过 |

## 2026-09-03

| 时间 | 类别 | 改动 | 验证 |
|---|---|---|---|
| 20:21 | ui | 补齐全真排序（默认/涨幅/跌幅）与筛选（仅上涨）交互 | 浏览器端到端验证：排序升降序、筛选开关、行跳转详情全部通过 |
| 20:15 | build | 升级 Kuikly 2.16.0 → 2.25.0（buildSrc/KotlinBuildVar + ohos 依赖 + 文档同步） | productionWebpack 全量编译成功，MarketPage 正常渲染 |
| — | ui | 目标结构落盘：MarketPage/StockDetailPage/AiResearchPage + 公共组件体系 + TDesign 图标 + Light/Dark 主题（历史重构基线） | 浏览器验证 |

---

## 待办 / 已知
- [ ] UI 精修三屏（Header 品牌位 / 全数字等宽接入 / AI 强调落地 / 设置面板占位）
- [ ] 真实数据接入（搜索 JSONP 桥 + RemoteAiService 走后端 8000）
- [ ] Agent 端到端联调（后端 gateway 已具雏形：SSE + tools 编排）
- [ ] 素材替换（用户后续提供：品牌 logo / 个股 logo / AI 配图）