# 知牛前端 UI · Design QA

## Source truth

- 方向参考：`/Users/c14h14n3/.codex/attachments/793ca392-22c6-4c0e-a97a-043bf5c424d5/image-1.png` 至 `image-4.png`
- 改造前基线：同目录 `image-5.png` 至 `image-7.png`
- 本轮缺陷证据：`/Users/c14h14n3/.codex/attachments/97442749-5fe8-402f-9855-cd557334b119/image-1.png` 至 `image-3.png`（按钮折行、AI 空白、侧栏裁切）
- 项目设计规范：`docs/ui-redesign.md` 与 `kuikly-shell/AGENTS.md`
- 图标来源：Tencent TDesign Icons 官方仓库，MIT License

## Implementation evidence

- 本地入口：`http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1`
- Codex 内置浏览器实测并截图：1280×800、1440×900、1920×1080，以及约 730px 窄窗口；2026-09-11 再验真实 Agent 证据卡与最新消息自动跟随。
- 覆盖状态：市场列表/沪市筛选、股票详情五标签、日 K 缩放/复位/滚轮、五档盘口、AI 分析面板、AI 会话/角色/发送、主题切换。
- 当前轮截图保存在 `docs/audit-2026-09-11/`，含修复前后的行情、设置、详情与 AI 页面，便于复查窄屏问题。

## Comparison audit

| 检查项 | 改造前问题 | 当前结果 |
| --- | --- | --- |
| 信息层级 | 大块空白、行情信息离散 | 市场脉冲、行情表、详情指标与 K 线形成清晰主次层级 |
| 按钮与图标 | 缺少按钮语义，图标显示为方块，文字折行 | 统一按钮体系并强制单行；20 枚 TDesign 官方图标透明底显示正常，含 active/filled 状态与右向箭头 |
| 路由 | 页面入口关系不清 | MarketList → StockDetail → AI 分析 / AiResearch 可操作闭环 |
| 股票基础信息 | 指标与行情字段不足 | OHLC、成交量/额、换手率、振幅、PE/PB/市值/量比、资金/财务/新闻 Tab 完 |
| K 线 | 图表孤立、缺少缩放和辅助决策信息 | 默认最新 80 根；按钮、鼠标滚轮、拖拽与捏合视口交互；蜡烛/量柱/MA/MACD/RSI、五档盘口与 AI 解读同屏 |
| AI 研究 | 大块空白、发送入口弱、侧栏内容裁切 | 历史会话恢复真实内容；发送识别到标的的问题会调用研究网关，展示四阶段进度、真实证据、来源、风险与时效；失败才回退本地快照 |
| 数据 | 页面主要依赖固定快照 | 实时个股报价、日/分时 K 线与个股资讯接入 FastAPI；资讯显示媒体、时间与实时/缓存标记 |
| 字体与数字 | 字号、字重、数字对齐不统一 | 中文系统字体 + 数字等宽字体，语义字号/字重/颜色 token 统一 |
| 响应式 | 约 730px 下指数数值重叠、表格列被裁、K 线右栏挤压图表、AI 会话侧栏占据过多空间 | 760px compact：指数与市场宽度分行；表格只留四个核心字段；K 线与盘口上下排列；AI 会话栏收起并保留“新建”入口 |
| 设置 | 面板窄、状态语义含混 | 288px 外观/数据源/Agent/免责声明分组；选中态与分组图标使用 TDesign 官方资源 |
| Agent | 工具执行与页面数据各自维护 | 工具统一复用行情/K线/资讯 provider；JSON 研究接口已由 Kuikly 调用，类型化 SSE 以显式成功/失败终止帧收束 |
| 多端安全区 | Header、浮层、底部输入区使用固定边距 | 使用 Kuikly 官方 `pageData.safeAreaInsets`；Header/浮层避让顶部，列表页尾和 AI Composer 避让底部手势区 |
| 主题 | 切换后局部仍使用浅色 token | AdaptiveColors 动态代理保证表面、文字、边框与图表同步切换 |

## Interaction and quality gates

- AI Composer：新建、历史会话、角色切换、发送按钮均通过；发送宁德时代问题后返回对应标的回复。
- AI 网关：发送“分析贵州茅台”返回 1285.13、RSI 54.70、MA20 1301.66、12 条资讯及 `eastmoney-search` 来源；最新结果自动滚动到可视区。
- 详情标签：概览/资金/财务/新闻/AI解读内容断言 5/5 通过；修复一次性渲染导致的假点击。
- K 线：放大 `80根 → 68根`、复位 `→ 80根`，鼠标滚轮可继续改变可见根数。
- 行情：实时贵州茅台 1290.88；“沪市”筛选后主表仅保留 SH 标的。
- 主题：Light / Dark 切换完整，无黑字黑底或白色断层。
- Console：本轮最终路由控制台 0 error。
- Build：`:shared:compileKotlinJs` 与 `scripts/build.sh` 通过；脚本已启用 `pipefail`，不再掩盖 Gradle 失败。
- Backend：`.venv/bin/python -m unittest discover -s tests -v` 39/39 通过；覆盖研究 SSE 正常终止与错误终止。
- Test：`:shared:jsTest` 在测试可执行文件编译阶段触发 Kotlin/JS IR `IrSimpleFunctionSymbolImpl is already bound`；`clean` 后仍复现，未进入测试用例执行，判定为当前 Kuikly/Kotlin 工具链问题而非本次 UI 回归。

## Severity summary

- P0: 0
- P1: 0（本轮发现的窄屏核心任务阻断已修复）
- P2: 0
- P3: 1（正式品牌 Logo 尚待用户提供，当前使用纯文字，不阻断功能）

Final result: passed.
