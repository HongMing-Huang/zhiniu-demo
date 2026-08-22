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