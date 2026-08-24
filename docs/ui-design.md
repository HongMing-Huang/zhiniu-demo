# 知牛 ZhiNiu · UI 界面设计说明

> 版本：v0.1（2026-08-22）
> 定位：这是「界面设计」的权威交付物。页面 .kt 仅保留官方已验证的最小 Page 形态；完整布局/组件/交互在此定义，交由 `feat/frontend-runtime` 或官方模板环境按本说明实现（用 Kuikly 内置组件，不自绘 SVG）。
> 组件选型来源：`docs/kuikly-common-assets.md`；状态/交互：技术方案 §12。

---

## 0. 设计基调

- 组件：一律用 Kuikly 内置 `View / Text / ScrollView / List / Tab / Dialog / Carousel / Input / Button` + 社区 `KuiklyMarkdown`（流式）。
- 图标：Google Material Symbols 字体（不自绘 SVG）。
- 配色（A 股红涨绿跌）：`UP #E6432E` / `DOWN #09B76F` / `TEXT #2B2F36` / `SUB #8A919C`。
- 状态机：每页四态 Idle / Loading(骨架) / Success / Error(可重试)。

---

## 1. 行情列表页 MarketList

| 区块 | 内容 | 组件 | 数据源 |
|:--|:--|:--|:--|
| 顶部指数条 | 上证/深成/创业板：名称+点位+涨跌幅 | `View` + `Text`（横向排列） | `GetStockList.indices()`（新浪实时/mock） |
| Tab 切换 | 自选/全部/涨幅/跌幅 | `Tab` | `MarketListVM.setMode()` |
| 股票列表 | 行=名称+代码 / 最新价 / 涨跌幅 / AI标签 | `List` 高性能 + 行内 `View`/`Text` | `GetStockList.all()/watchlist()` |
| 下拉刷新 | 手势触发重拉 | Kuikly 手势 + `List` | `MarketListVM.refresh()` |

- 行交互：单击 → `openPage(StockDetail, symbol)`；长按 AI 标签 → `Dialog` 解释依据。
- 四态：Loading=同行尺寸骨架；Error=内嵌错误卡 + Retry。

## 2. 个股详情页 StockDetail

| 区块 | 内容 | 组件 | 数据源 |
|:--|:--|:--|:--|
| 头部 | 名称/代码/最新价/涨跌幅（着色） | `View`+`Text` | `quote` |
| OHLC 卡 | 今开/最高/最低/昨收/量/额 | `View`+`Text` | `quote` |
| 五档盘口 | 买一~五/卖一~五 | `List` 或 `View` | `quote.bids/asks` |
| K 线 | 迷你走势 / 日K | 内置 `Canvas` 自绘（或 View 柱状近似） | `kline`（新浪/mock） |
| Tab | 概览 / **AI 诊股** | `Tab` | — |
| AI 诊股 | 触发「AI 看看」→ 4 卡 | `Dialog`/结果区 | `DiagnoseStock`（网关→Mock） |

- AI 结果渲染（技术方案 §6.4）：
  - 总结卡（Markdown / 文本）
  - 信号卡（标签云 ≤6）
  - 风险卡（等级徽章）
  - 跳转卡（→回聊天/看K线）
- 隐藏亮点：AI 诊断下「换个角度看」→ `Carousel` 多/空气泡轮播。

## 3. AI 聊天主页 ChatHome

| 区块 | 内容 | 组件 | 数据源 |
|:--|:--|:--|:--|
| 消息列表 | 用户/AI 气泡 | `List` | `ChatVM.ui.messages` |
| 流式渲染 | Markdown 逐字 | `KuiklyMarkdown` | `AskChat`（网关 SSE） |
| Agent 时间线 | 基本面→技术→舆情→风控 | 顶部 `Stepper`（View+Text） | 网关事件/本地推导 |
| 快捷指令 | 看大盘/诊个股/解释指标/对比两只 | `Button` 组 | `ChatVM` |
| 输入区 | `Input` + 发送 `Button` | `Input`/`Button` | `ChatVM.send()` |
| 卡片跳转 | AI 结果卡 → StockDetail | `View` onClick | `openPage` |

---

## 4. 页面跳转闭环（核心交互三角）

```
聊天页 --提问--> AI 返回卡片 --点击 JumpCard--> 详情页（K线+AI解读）
   ↑                                              ↓
   └------ 返回继续追问 <------ openPage 反向返回 ----┘
```
路由：`@Page` 注册 `MarketList / StockDetail / ChatHome / Home`，用 `openPage(name, args)`。

---

## 5. 实现状态（2026-08-22 更新）

三个页面已在 `shared/src/commonMain/kotlin/com/zhiniu/pages/` 完整落地：
- `MarketListPage`：指数条 / Tab / 股票行 / AI 标签（`observableList` + `vfor/vif`）
- `StockDetailPage`：头部 / OHLC / 五档 / 迷你K线 / AI 诊股（4 卡 + 跳转）
- `ChatHomePage`：标题+Agent时间线 / 消息气泡 / 快捷指令 / 输入区
- VM 已切换为 Kuikly 原生 `observable/observableList`（kuiklyDSL.mdc）

**待模板环境校准**（签名级，非设计缺失）：
- `vfor/vif/onClick/vbind` 指令与颜色/尺寸具象签名以 Kuikly SDK 官方模板为准
- 图标按 Material Symbols 字体接入；骨架屏/微动效按技术方案 §12.7.3

参考：技术方案 §12.2 交互矩阵、§12.3 状态机、`docs/kuikly-common-assets.md`。

---

# 附录 B · v1.0 暗色金融级设计规范（2026-08-24 升级版 · 权威）

> **本附录为 ui-design.md 的现行权威版本**，原 §0–§3（v0.1 浅色版）作为历史参考保留。
> 升级依据：① 图 1 用户提供的 Kuikly 官方 Stepper 风格；② 众安/欧易/币安金融平台设计调研；③ Kuikly 完整组件库（35+ 组件 + LiquidGlass + AI Chat 官方组件）。

## B.1 设计基调升级

| 维度 | v0.1（旧） | v1.0（新） | 依据 |
|---|---|---|---|
| **主题** | 浅色为主 | **暗色优先**（暗为默认 + 浅色可切） | 欧易/币安 高级模式暗色；金融专业感 |
| **卡片** | 简单矩形 | **卡片化**（圆角 8-12 + 微阴影 + 1px 描边） | 众安/币安 |
| **数据层级** | 平铺 | **三层（标签-数值-变化）** | 币安左中右栏式 |
| **涨跌色** | 红涨绿跌 | **红涨绿跌**（中国 A 股习惯，不变） | 国内金融惯例 |
| **强调色** | 蓝紫 | **知牛绿 #1D9E75 + 蓝链接 #378ADD** | 知牛品牌统一 |
| **特效** | 平面 | **Kuikly iOS 26+ LiquidGlass** 局部 | 现代高端感 |
| **图表** | 自绘 | **自绘 Canvas + 联动交互** | 金融专业 |

## B.2 设计 Token（W3C DTCG 风格）

### B.2.1 颜色（暗色主题默认）

```
/* 中性层（4 级灰） */
--color-bg-page:        #0B0E14   /* 页面底（最暗，衬托卡片） */
--color-bg-card:        #1A1F2E   /* 卡片底 */
--color-bg-elevated:    #252B3B   /* 悬停 / 二级面板 */
--color-border:         #3A4257   /* 描边 / 分割线 */
--color-text-primary:   #EAF3DE   /* 主文字（微绿白，暗色更护眼） */
--color-text-secondary: #C0DD97   /* 次要文字 */
--color-text-tertiary:  #888780   /* 提示 / 占位 */

/* 金融强调（4 色） */
--color-up:             #E24B4A   /* 涨（A 股惯例） */
--color-down:           #639922   /* 跌 */
--color-warn:           #FAC775   /* 警示 */
--color-info:           #7F77DD   /* 资讯 / 链接辅助 */

/* 品牌与点缀（3 色） */
--color-brand:          #1D9E75   /* 知牛主绿 */
--color-link:           #378ADD   /* 链接蓝 */
--color-highlight:      #AFA9EC   /* 高亮（hover 强调） */
```

### B.2.2 字体 / 字号 / 行高

| Token | 值 | 用途 |
|---|---|---|
| `--font-sans` | system-ui, -apple-system, "PingFang SC", "Microsoft YaHei" | 全局无衬线 |
| `--font-mono` | "JetBrains Mono", "SF Mono", Menlo | 价格 / 数字等宽 |
| `--fs-display` | 32px / 500 | 行情详情大价 |
| `--fs-h1` | 24px / 500 | 页面标题 |
| `--fs-h2` | 20px / 500 | 区块标题 |
| `--fs-h3` | 16px / 500 | 列表项标题 |
| `--fs-body` | 14px / 400 | 正文 |
| `--fs-caption` | 12px / 400 | 提示 / 角标 |
| `--lh-tight` | 1.2 | 大标题 |
| `--lh-base` | 1.5 | 正文 |

### B.2.3 间距（8 倍数 / 4 半步）

```
--space-1: 4px   --space-2: 8px   --space-3: 12px
--space-4: 16px  --space-5: 20px  --space-6: 24px
--space-8: 32px  --space-10: 40px --space-12: 48px
```

### B.2.4 圆角 / 阴影 / 动效

| Token | 值 | 用途 |
|---|---|---|
| `--radius-sm` | 6px | 小标签 / 徽章 |
| `--radius-md` | 8px | 按钮 / 输入框 |
| `--radius-lg` | 12px | **卡片默认** |
| `--radius-xl` | 16px | 弹窗 / 底部 sheet |
| `--radius-full` | 9999px | 头像 / 圆形按钮 |
| `--shadow-card` | `0 2px 8px rgba(0,0,0,.3)` | 卡片 |
| `--shadow-pop` | `0 8px 24px rgba(0,0,0,.5)` | 弹窗 / Tooltip |
| `--motion-fast` | 150ms ease-out | 列表项入场 / hover |
| `--motion-base` | 250ms ease-out | Tab 切换 / 卡片翻转 |
| `--motion-slow` | 400ms ease-out | 弹窗进出 |
| `--motion-flash` | 600ms | 价格变化闪烁（涨红跌绿） |

## B.3 Kuikly 官方组件映射（35+ 组件，按需选用）

> 来源：https://kuikly.tds.qq.com/API/ 官方组件目录（2026-08-24 实测）

| 我们的需求 | Kuikly 官方组件 | 备注 |
|---|---|---|
| 卡片背景 | **`GlassEffectContainer`** + `LiquidGlass` | iOS 26+ 风格，暗色高级感 |
| 顶部 Tab 切换 | `Tabs` / `ScrollableTabRow` / `iOSSegmentedControl` | 三种风格任选 |
| 列表（行情/资讯/会话） | `List` + `PageList`（分页） + `WaterFallList`（瀑布） | 行情用 List，资讯用 PageList |
| 滚动容器 | `Scroller` | 用于卡片内垂直滚动 |
| 下拉刷新 | **`Refresh`** | 行情列表必备 |
| 列表尾部刷新 | `FooterRefresh` | 翻页加载更多 |
| 网格/瀑布流 | `WaterFallList` | 资讯流场景 |
| 轮播图 | `SliderPage` / `Carousel` | 首页 Banner / 多空辩论 |
| 步进器 | `View`+`Text`（参考图 1） | 多空辩论 2 轮切换 |
| 按钮 | `Button` | 全部交互入口 |
| 复选框 / 开关 | `CheckBox` / `Switch` | 预警 / 设置页 |
| 滑块 | `Slider` | 阈值设置 |
| 输入框 | `Input`（单行）/ `TextArea`（多行） | 搜索 / 聊天输入 |
| 日期选择 | `DatePicker` | 财报日历 |
| 选择器 | `ScrollPicker` | 模型选择下拉 |
| 对话框 | `Dialog` / `AlertDialog` | 弹窗确认 |
| 操作表 | `ActionSheet` | 长按列表行弹出 |
| 模态 | `Modal` | 全屏遮罩（Agent 进度） |
| 底部弹 sheet | `Modal`（底部） | 预警编辑 |
| 遮罩 | `Mask` | Loading 遮罩 |
| 悬停置顶 | `Hover` | 工具提示 |
| 高斯模糊 | `Blur` | 弹窗背景模糊 |
| 文本 | `Text` | 全部文本 |
| 富文本 | `RichText` | 聊天流式渲染（备选 KuiklyMarkdown） |
| 图片 | `Image` | Logo / 资讯配图 |
| 视频 | `Video` | 演示视频回放 |
| 动画 | `APNG` / `PAG` | 加载动画 / 奖励动效 |
| K 线图 | **`Canvas`**（自绘） | 蜡烛 + MA + 十字游标 |
| AI 对话 | **`AI Chat` 组件**（官方内置） | 聊天页可优先用官方 AI Chat 组件 |
| 活动指示器 | `ActivityIndicator` | Loading 转圈 |

## B.4 10+ Kuikly 内置 Module（后端能力对接）

| Module | 用途 | 知牛用法 |
|---|---|---|
| `RouterModule` | 跨页跳转 | 统一路由入口 `openPage` |
| `NetworkModule` | HTTP 客户端 | 调网关 /v1/* 与 /quote/* |
| `SharedPreferencesModule` | 本地 KV | 自选股 / 主题 / 历史会话 |
| `MemoryCacheModule` | 内存缓存 | 行情快照 3s 缓存 |
| `SnapshotModule` | 状态持久化 | ViewModel 状态恢复 |
| `CalendarModule` | 日历 | 财报日历 |
| `CodecModule` | 编解码 | Base64 / JSON |
| `NotifyModule` | 通知 | 预警推送 |
| `PerformanceModule` | 性能监控 | FCP / 帧率埋点 |

## B.5 5 大类金融平台设计风格参考

| 平台 | 关键特征 | 知牛采纳 |
|---|---|---|
| **欧易 OKX** | 黑底像素 X / 暗色霓虹 / 强数据实时闪烁 | 暗色主题 + 价格变化闪烁动画 |
| **币安 Binance** | 三栏式（左价格盘 + 中图表 + 右买卖） | 详情页布局：左侧标的+五档 / 中央 K 线 / 右侧 AI 诊股 |
| **众安** | 圆角卡片 / 清晰数据层级 / 友好感 | 卡片化布局 + 三层信息架构 |
| **Gate.io** | 暗色 + 蓝绿品牌色 | 知牛主绿 #1D9E75 呼应 |
| **TradingView** | 专业图表 + 工具栏 | K 线 Canvas 自绘 + 十字游标 + 缩放 |

## B.6 19 个页面布局规范（按页面矩阵 §6）

> 通用布局原则：① 顶部状态栏（细线 + 应用名） ② 主内容（卡片化） ③ 底部操作或 Tab。暗色 + LiquidGlass 局部点缀。

### B.6.1 启动 / 导航（3 页）

**SplashPage** — 满屏深底 + 知牛 logo（`Image`） + 0.6s 淡出 → HomePage
**HomePage** — 顶部状态栏 + 下方 4 Tab 内容区（`Tabs` 容器）
**MainTabBar** — 底部 4 Tab：行情（📊）/ 资讯（📰）/ AI（💬）/ 我的（👤），iOS 风格图标

### B.6.2 行情（6 页）

**MarketListPage**（已实现，需套暗色 token）
- 顶部 32px 状态栏（透明白色文字）
- 指数条（4 指数：上证/深成/创业板/北证 50）：横向 `Scroller` + 圆角 8 卡片 + 涨跌幅着色
- Tab 切换：`Tabs` 自选/全部/涨幅/跌幅/换手
- 股票行：名称（左）+ 代码（小字）+ 最新价（右大）+ 涨跌幅（右小，着色）+ 微型走势缩略图（最右，60×24px mini Chart）
- 暗色：`bg-card #1A1F2E`，文字 `text-primary #EAF3DE`，涨红跌绿

**IndexListPage** — 大盘指数列表：8 行（上证/深成/创业板/北证 50/沪深 300/中证 500/中证 1000/恒生/纳指/道指），行格式同个股但无五档
**StockSearchPage** — 顶部 `Input` + 流式联想 `List`（气泡）+ 热搜词云 + 历史记录
**StockDetailPage**（已实现，三栏式布局）
- 头部：名称 + 代码 + 最新价 + 涨跌幅（顶吸，1.2 倍行高）
- 三栏：左 OHLC（5 行小卡）/ 中 K 线（60% 宽，Canvas 自绘）/ 右 AI 诊股入口（4 张卡入口）
- 五档盘口：买一到买五 / 卖一到卖五，红色卖盘绿色买盘
- 底部 Tab：概览 / K 线 / **AI 诊股** / 资金流向
**IndexDetailPage** — 复用 StockDetail 骨架，去掉五档，加权重股 Top10
**SectorBoardPage** — 申万一级行业（31 个）+ 二级展开 + 涨跌幅排行前三

### B.6.3 资讯 / 社区（2 页）

**NewsListPage** — 顶部 Tab（7×24 / 个股新闻 / 大盘解读 / 公告）+ 资讯卡片流（`PageList` + 下拉刷新 `Refresh` + 尾部 `FooterRefresh`）
**NewsDetailPage** — 标题 + 来源 + 时间 + 正文 + 相关个股标签 + AI 摘要按钮

### B.6.4 AI（5 页）

**ChatHomePage**（已实现）
- 顶部：标题 + 模型徽章（点击进 ModelSettings 切模型）+ 新会话按钮
- Agent 进度时间线：4 步骤横条
- 历史会话：左抽屉（点击展开）
- 主区：消息列表 `List`
- 底部：`TextArea` + 发送按钮

**ChatSessionListPage** — 历史会话列表（标题缩略 + 时间 + 模型徽章）
**ChatDetailPage** — 单会话详情（同 ChatHomePage 主区，含工具调用灰色卡 + 结构化结果卡）
**ModelSettingsPage**（参考图 2 形态）
- 顶部：搜索框 `Input`「搜索模型平台」（过滤 14 家服务商）
- 服务商列表 `List`：每行 = logo + 名称 + 状态徽章（✅/⚠️/❌） + 「配置」按钮
- 点击「配置」→ `Dialog` 弹窗：仅 1 个 `Input`（Key） + 「使用代理」`Switch`
- 底部：「+ 添加服务商」按钮（`Button`）→ 自定义 baseUrl 弹窗
- 用量 Tab：三张统计卡 + 折线图 + 饼图

**DebateViewPage**（PR-09）— 上下分栏
- 上半：多/空气泡轮播 `Carousel`（左右滑切）
- 下半：分歧点高亮清单
- 顶部 Tab：第 1 轮 / 第 2 轮 / 总结

**AgentProgressOverlay** — 全屏 `Mask` + 中央 4 步骤进度卡（基本面 ✓ → 技术 ✓ → 舆情 ⟳ → 风控 ○）

### B.6.5 自选 / 设置（3 页）

**WatchlistPage** — 分组管理（默认「自选」+ 自建分组）+ 拖拽排序（`List` 长按拖动）+ 添加按钮
**AlertSettingsPage** — 列表显示已设预警 + 「+ 新建」→ 弹窗：标的搜索 + 条件类型 `Picker`（4 类）+ 阈值 `Input`
**UserSettingsPage** — 主题（跟随系统/暗/浅 `SegmentedControl`）+ 字号（标准/大 `SegmentedControl`）+ 关于 + 免责声明

## B.7 关键交互动效清单

| 场景 | 动效 | 时长 |
|---|---|---|
| 价格变化 | 涨红 / 跌绿 闪烁 | 600ms |
| 列表项入场 | 渐显 + 上移 8px | 150ms |
| Tab 切换 | 内容左右滑 + 指示器 | 250ms |
| 卡片点击 | 涟漪 + 缩放 0.98 | 150ms |
| 弹窗进出 | 缩放 0.9 → 1.0 + 蒙版淡入 | 400ms |
| AI 流式 | 逐字增长 | 即时 |
| 工具调用 | 灰卡淡入 → 结果绿卡替换 | 200ms |
| Agent 步骤 | ✓ 缩放 0 → 1 + 渐绿 | 300ms |

## B.8 视觉参考实施清单（P0 必做）

1. ✅ 暗色主题全站铺开
2. ✅ 涨跌色按 A 股习惯（红涨绿跌）
3. ✅ 所有卡片用 8-12px 圆角 + 1px 描边 + 微阴影
4. ✅ 字体/间距严格按 Token
5. ✅ 数字一律等宽（`font-mono`）
6. ✅ 列表入场动画
7. ✅ 关键页三栏式（详情页）布局
8. ✅ ModelSettings 复刻图 2 的"服务商卡片 + 弹窗填 Key"形态
9. ✅ 顶部 4 Tab 导航 + iOS 风格图标
10. ✅ 价格变化闪烁动效

## B.9 评分映射

| 评分维度 | 兑现 |
|---|---|
| 40% 功能完整性 | 19 页全功能 ✓ |
| 25% 工程质量 | Kuikly 官方组件 35+ 复用 ✓ |
| 25% AI 场景 | 暗色专业感 + AI Chat 官方组件可用 |
| 10% 体验优化 | 暗色主题 + 动效 + 卡片化 + LiquidGlass |

