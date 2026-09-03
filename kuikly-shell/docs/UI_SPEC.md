# 知牛 · UI_SPEC

> 视觉与组件规范（设计→实现的契约）。所有页面与组件**只**参照本文件；不在此文件出现的样式值 = 不允许。

---

## 1. Color（赛题规范 · 严格；Light 白底 / Dark 分层深灰）

### 1.1 Light
| Token | Hex | 用途 |
|---|---|---|
| `pageBg` | `#FFFFFF` | App Background（白） |
| `surface` | `#FFFFFF` | 卡片/浮层/Header 表面 |
| `surfaceSubtle` | `#F2F4F6` | 次表面（输入背景/标签） |
| `hover` | `#EEF1F4` | Row/Button hover |
| `border` | `#E7E9ED` | 默认描边/分割线 |
| `borderStrong` | `#D9DDE3` | 强描边（卡片边） |
| `textPrimary` | `#171A1F` | 主文字/价格 |
| `textSecondary` | `#66707A` | 次文字/标签 |
| `textMuted` | `#969FA9` | 弱化/提示 |
| `up` | `#E5484D` | A 股涨 |
| `down` | `#13A66A` | A 股跌 |
| `ma5` / `ma10` / `ma20` | `#E3A24C` / `#5B7CFA` / `#A879D8` | MA 均线 |
| `chartGrid` | `#EDF0F2` | 图表网格 |

### 1.2 Dark
| Token | Hex |
|---|---|
| `pageBg` | `#0E1013` |
| `surface` | `#15181C` |
| `surfaceSubtle` | `#1A1E23` |
| `hover` | `#20252B` |
| `border` | `#292E35` |
| `borderStrong` | `#343A42` |
| `textPrimary` | `#F2F3F4` |
| `textSecondary` | `#9CA4AE` |
| `textMuted` | `#68717B` |
| `up` | `#E5484D`（Light/Dark 一致） |
| `down` | `#14A66A`（不变） |
| `ma5/10/20` | `#E3A24C` / `#7B8DFB` / `#B78CE0` |
| `chartGrid` | `#272C32` |

## 2. Typography（系统 Sans）

| 角色 | size / weight | 样例 |
|---|---|---|
| Brand | 20 / Semibold | 知牛 |
| Page Title | 24 / Semibold | 市场 · AI研究 |
| Stock Name | 20 / Semibold | 贵州茅台 |
| Large Price | 32 / Semibold | 1292.83 |
| Section | 16 / Semibold | 关键数据 · 趋势 |
| Body | 14 / Regular | 综合状态 |
| Table | 14 / Medium | 涨跌额 / 涨跌幅 |
| Caption | 12 / Regular | Demo 行情 · 14:32 更新 |
| Stat | 18 / Semibold | 上涨/下跌 5,128 / 1,932 |

数字使用等宽字体栈（仅 H5 视觉生效，原生端优雅降级）：
```
ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace
```

## 3. Spacing / Radius

| 名称 | 值 | 用途 |
|---|---|---|
| `space1` | 4 | 极小间距 |
| `space2` | 8 | 标签间距 |
| `space3` | 12 | 段落间距 |
| `space4` | 16 | 面板内边距 |
| `space5` | 20 | 区块间距（小） |
| `space6` | 24 | 区块间距（中） |
| `space8` | 32 | 区块间距（大）/ 水平留白 |
| `space10` | 40 | Header 与正文分隔 |
| 区块间距 | 28 | 区块之间 |
| 面板间距 | 16 | 面板之间 |
| Radius 6 | 6 | 状态徽章 / Chip |
| **Radius 8** | 8 | 按钮 / 卡片 / 浮层 / 输入框 |
| Radius 10 | 10 | 历史默认值，部分浮层 |

## 4. 尺寸

| 名称 | 值 |
|---|---|
| Header 高 | 64 |
| 内容最大宽 | 1360 |
| 水平留白 | 32 |
| Button 高 | 34 / 38 |
| Input 高 | 38 |
| Table 行 | 60 |
| QuoteHeader | ~160 |
| K线 Canvas 高 | 440 |
| Drawer 宽 | 380 |
| SearchOverlay 宽 | 480 |
| ThemePopover 宽 | 220 |
| AiResearch 中栏 | flex(1) |
| AiResearch 右栏 | 288 |

## 5. 状态

| 状态 | 视觉 |
|---|---|
| Default | 背景 = surface/secondary，文字 = textPrimary/secondary |
| Hover | 背景 → hover 色（120ms） |
| Pressed | 背景 → textPrimary @ 8% 透明度（100ms） |
| Focused | 描边 → borderStrong + 背景 surfaceSecondary |
| Disabled | 文字 textMuted，背景 surfaceSecondary，cursor not-allowed |
| Loading | 骨架条（surfaceSecondary 80% 宽度） |
| Empty | Icon + 标题 + 描述 + 可选 Action |
| Error | 标题 + 重新加载按钮 |

## 6. 组件

### 6.1 AppButton
| 变体 | 高度 | 背景 | 文字 | 用途 |
|---|---|---|---|---|
| Primary | 34/38 | textPrimary | 反色 | AI分析 / 发送 |
| Secondary | 34 | surface | textPrimary | 加自选 / 查看详情 |
| Ghost | 34 | 透明 | textSecondary | 排序 / 筛选 |
| Icon | 36 | 透明 | textSecondary | 主题/关闭/返回 |

### 6.2 AppTabs
- 下划线指示条：底部 2px textPrimary，160ms 滑动
- 槽宽 64 (market) / 56 (chart) / 88 (detail)

### 6.3 SegmentedTabs
- 表面+描边，30px 高

### 6.4 SearchField
- 240×36，surfaceSecondary 底，搜索图标 + placeholder
- 点击 → 打开 StockSearchOverlay（不跳页）

### 6.5 StockTable
- 列：股票 | 最新价 | 涨跌额 | 涨跌幅 | 今日走势 | 高/低 | 成交额
- 列宽：120/100/100/170/150/130 + flex 股票
- 行高 60，hover 120ms，整行点击
- 数字右对齐 + 等宽字体
- 表头：fs11 / textMuted / textTertiary
- 排序：列点击循环 asc/desc；右侧 chevron-up/down 指示

### 6.6 MarketPulse
- 5 组（3 指数 + 上涨/下跌 + 成交额），1px 竖向 Divider 分隔
- 不带 Card 外壳（border radius 8 + border 1 包裹整体）
- 高度自适应
- <1280：隐藏"两市成交额"组

### 6.7 QuoteHeader
- 返回 + 名称代码 + 自选 + AI分析
- 价格 32/SemiBold + 涨跌/涨跌幅（14/Medium，红色）
- 8 指标 2×4 grid
- 高度 ≈ 150

### 6.8 ChartToolbar
- 周期：分时/日K/周K/月K（Tab，64/40）
- 指标：MA/MACD/RSI（SegmentedTabs，30）
- MA 图例：fs11 + 等宽

### 6.9 KLineChart
- 图表：priceTop~priceBottom（72%），volume 区（13%），indicator 区（20%）
- 蜡烛：实体 + 影线，A 股红涨绿跌
- 均线：MA5/10/20 覆盖主图
- 副图：MACD（hist + DIF/DEA 线）或 RSI（30/50/70 三条参考线）
- Grid：5 条横线 + Y 轴价格 + X 轴时间
- Crosshair：虚线 + 价格标签 + OHLC Tooltip
- 分时：折线 + 渐变面积 + Volume

### 6.10 AiStockCard
- 圆角 8 + borderStrong 描边
- 名称/代码/价格/涨跌 + 趋势/RSI 键值 + 查看详情
- 整卡可点击进入 StockDetail

### 6.11 AiInsightDrawer
- 宽 380，右侧滑入，200ms translateX+opacity
- 头部：知牛 AI + close
- 综合状态（20/SemiBold）
- 趋势/量能/技术信号/风险 四段
- 追问 chips + 问答流 + 输入框

### 6.12 ThemePopover
- 宽 220，160ms opacity+translateY
- 三档：跟随系统 / 浅色 / 深色

## 7. 动画

| 元素 | 属性 | 时长 | 缓动 |
|---|---|---|---|
| Button | backgroundColor | 100ms | easeOut |
| StockRow | backgroundColor | 120ms | easeOut |
| Tabs Indicator | translateX | 160ms | easeOut |
| SearchOverlay | opacity+translateY | 160ms | easeOut |
| ThemePopover | opacity+translateY | 160ms | easeOut |
| Drawer | translateX+opacity | 200ms | easeOut |
| Theme | backgroundColor | 180ms | easeOut |
| K线周期切换 | Canvas 重绘 | 100ms | crossfade |

禁：Spring / Bounce / 3D / Glow / 无限装饰 / Card Zoom。

## 8. Light / Dark 切换

- 主题状态保存在 `AppTheme.mode`（enum SYSTEM / LIGHT / DARK）
- 切换 → `applyHostTheme(dark)` 设置 `<html data-theme="dark">` + `AppTheme.isDark` 触发全部 `animate(ANIM_THEME, value = AppTheme.isDark)`
- SYSTEM 模式下监听 `matchMedia('(prefers-color-scheme: dark)')` 变化


## 10. 图标（Tencent TDesign · 本地 PNG）
- 资源：`Tencent/tdesign-icons`（develop 分支 `svg/`），官方文件名经 GitHub API 核验
- 落地：`shared/src/commonMain/assets/common/icons/`（native）+ `web-host/assets/common/icons/`（H5）
- 加载：Kuikly `Image` + `tintColor`（H5 SVG filter / 原生 ImageView tint）
- 渲染：16/18/20px 自由调用；主题色按 `colors.textPrimary/secondary/tertiary` 染色
- 禁：Emoji、Unicode 字符、自绘 Canvas 图标
