# 知牛 · 欧易（OKX）设计参考与落地清单

> 目的：按课题「体验优化」加分项，借鉴欧易 OKX 行情/交易产品的设计气质完善 Web 与移动端界面，同时不违背 `AGENTS.md` §2 视觉硬约束（克制、黑白灰、A 股红涨绿跌、禁渐变/毛玻璃/Emoji）。
> 调研说明：本轮 Exa MCP 工具未在会话中注册，改用内置 WebSearch 完成一轮检索；建议启用 Exa 插件后对「OKX 设计规范 / Dribbble 案例」做一次深度调研补充本文件。

## 1. 检索到的参考源

| 类型 | 来源 | 可借鉴点 |
|---|---|---|
| 官方 | [The New OKX Interface: Faster, Clearer, Unified](https://www.okx.com/en-us/learn/okx-interface-update) | 三模式（Simple / Exchange / Web3）**一键切换的统一界面**：同一页面内切换视图而非跳页 → 对应本项目「自选/全部/人气榜/涨幅榜」Tab 内切换、AI 页「分析/多空对抗/风控」角色视图 |
| 官方 | [How do I adjust the color theme of the trading interface?](https://www.okx.com/en-us/help/how-do-i-adjust-the-color-theme-of-the-trading-interface) | 涨跌色可在图表设置切换（红涨绿跌 / 绿涨红跌）；深色模式为交易场景标准 → 本项目固定 A 股红涨绿跌（课题约束），深浅色 + 跟随系统已实现 |
| 案例 | [OKX Designs on Dribbble](https://dribbble.com/tags/okx) | 高密度列表、单色底 + 极少强调色、等宽数字 |
| 设计系统 | [TradeStackUI（Figma，Light/Dark 变量）](https://www.figma.com/community/file/1443235511542147144/tradestackui-design-system-with-light-and-dark-mode-variables) | 交易类深浅色 token 分层（page / surface / subtle / border）与本项目 Theme 分层一致 |
| 指南 | [Trading App Design: UI, UX & Accessibility](https://lollypop.design/blog/2026/june/trading-app-design/) | 实时数据可视化、骨架屏、可访问性语义 |

## 2. 提炼的 OKX 式设计原则（与本项目对照）

| 原则 | OKX 表现 | 本项目状态 |
|---|---|---|
| 黑白灰底 + 单一强调色 | 品牌黑白，涨跌色是唯一高饱和色 | ✅ Theme 黑白灰；涨红 `#F04F5F` / 跌绿 `#16B364`；AI 区域限定 `#D9FF43` |
| 等宽数字、右对齐 | 价格/涨跌幅列全部 tabular | ✅ `NUM_FONT`（JetBrains Mono）+ `textAlignRight` |
| 高信息密度行情表 | 一行放名称/代码/价/涨跌/量/市值 | ✅ 本轮新增 总市值 / 成交量 列 + 「字段」自定义显示列 |
| 榜单式列表切换 | 自选 / 热门 / 涨幅榜 分段切换，排名在最左 | ✅ 本轮新增 人气榜（东财真实排名）/ 涨幅榜 Tab + `RankTable`（# 列，前三名加重） |
| 移动端双行行项 | 名称+代码 上，量/市值 下；价与涨跌右侧 | ✅ compact（≤760px）行内副行「代码 · 市值 · 量」 |
| 图表优先的详情页 | K 线占主区，盘口/深度侧栏原位切换 | ✅ 详情 72/28 分栏、盘口/数据/AI 原位切换（上一轮） |
| 骨架屏与状态诚实 | 加载骨架、数据来源/时效可见 | ✅ 榜单骨架、来源标签（「人气榜 · 东方财富」/「本地快照」）、`isStale` 提示 |
| 结论卡片化 | 结构化数字卡而非长文 | ✅ 「AI 解读」卡：趋势 / 建议 / 压力位 / 支撑位 / 置信度；「多空对抗」双方观点 |

## 3. 待办（下一轮体验优化）

1. **价格变动闪烁**：行情刷新时价格单元 120ms 背景闪红/绿（OKX 常见），需在 `StockRow` 比较前后价并 `animate`——受 Kuikly vfor diff 机制约束，需评估。
2. **榜单 Tab 记忆**：切换回市场页记住上次 Tab（用 `pageData.params` 或单例）。
3. **详情页顶部固定行情条**：滚动时价格/涨跌幅吸顶（OKX 移动端行为）。
4. **移动端底部导航**：课题参考图为「行情 / 自选 / 我的」底部 Tab；当前 H5 为顶部导航，原生端可在 compact 模式切换为底部 Tab（不新增页面，只是导航位置）。
5. **Exa 深度调研**：启用 Exa MCP 后补充 OKX 官方色值 / 间距规范核对本文件 §2。
