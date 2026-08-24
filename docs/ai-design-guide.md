# 知牛 ZhiNiu · AI 设计指导文档（Light Theme Edition）

> **版本**：v1.0 · 2026-08-24
> **定位**：本文档是知牛项目**所有界面设计的统一方法论**，整合自技术方案（zhiniu-technical-design.md）、组件选型（kuikly-common-assets.md）、UI 规范（ui-design.md）与 LLM 路由设计（llm-router-design.md）四份文档的设计相关内容。
> **读者**：AI 执行者（负责产出界面代码 / 设计稿的智能体）
> **核心目标**：教会 AI 按照统一方法出色地完成设计任务——**浅色调、官方组件优先、扩展严格守规、可验证**。
> **方向修正**：本文档为 **Light Theme（浅色）唯一权威版本**。ui-design.md 附录 B 的暗色规范仅作历史参考，不再执行。

---

## 导读 · 如何使用本文档

```
设计任何界面前：
  ① 通读第一部分 → 确认参考内容已收集齐（§1.6 Checklist 全绿）
  ② 按 §2 六步顺序执行，不跳步
  ③ 每步结束做该步的检查点
  ④ 最终用 §2 Step 6 + 附录 D 的 40 项 Checklist 做整体验证

遇到组件拿不准：
  ① 先查附录 B 官方组件映射表
  ② 查不到 → 按附录 C 模板登记为扩展组件（遵守 §2 Step 4 约束）
  ③ 仍拿不准 → 停下来对照官方 API 文档原文（§1.1），禁止臆造
```

**四条铁律**（违反任何一条 = 设计不合格）：

1. **浅色调统一**：全站 Light Theme，禁止局部暗色混搭（仅深色代码块等极小场景例外）
2. **官方组件优先**：能拼装就不自绘，能复用就不新建（Kuikly 35+ 官方组件 + Material Symbols 图标）
3. **扩展守规**：允许按需扩展，但扩展组件的尺寸/间距/圆角/颜色/交互状态必须取自官方规范与本文档 Token
4. **可验证**：每个设计决策都能指出官方文档出处或本文档章节号

---

# 第一部分 · 参考内容收集（设计前必读）

## 1.1 官方设计规范（权威基准，最高优先级）

| # | 资料 | 地址 | 获取方式 | 用途 |
|---|---|---|---|---|
| R1 | **Kuikly 组件 API 目录** | https://kuikly.tds.qq.com/API/ | WebFetch 直读 | 35+ 组件的参数签名（View/Text/List/Tab/Dialog/Canvas/AI Chat/LiquidGlass…）+ 10 个 Module |
| R2 | **Compose DSL 组件列表** | https://kuikly.tds.qq.com/ComposeDSL/allApi.html | WebFetch 直读 | Compose DSL 风格的组件参数（TabRow/Card/TextField/Surface…） |
| R3 | **KuiklyUI 官方源码 demo** | https://github.com/Tencent-TDS/KuiklyUI 的 `demo/` 目录 | GitHub 网页 / git clone | **唯一可信的真实 DSL 写法范本**（`class X : BasePager()` + `body(){ attr{} ... }` + `observable` delegate + `List{ vforLazy(...) }` + Float 属性） |
| R4 | **Kuikly 快速开始** | https://kuikly.tds.qq.com/QuickStart/hello-world.html | WebFetch 直读 | Hello World 与工程结构 |
| R5 | **官方 AI 规则文件** | Tencent-TDS/KuiklyUI-AI 仓库 `rules/kuiklyDSL.mdc` | GitHub 直读 | 官方写给 AI 的 DSL 约束 |
| R6 | **Kuikly 官方脚手架** | `npx create-kuikly-app create --package com.zhiniu --dsl kuikly` | 本地执行 | 真实可编译工程壳（含 androidApp/iosApp/ohosApp/shared） |

> ⚠️ **关键认知**（来自项目实测，血泪教训）：Kuikly **不是标准 Jetpack Compose**。它的 Compose DSL 是"对齐 Compose 写法"的自研 DSL，包名为 `com.tencent.kuikly.core.*` 而非 `androidx.compose.*`。**凡未经官方 demo 验证的写法一律视为臆造**——项目早期曾用 `com.tencent.kuikly.ref.*`（不存在的包）+ `vfor/vif` 自造语法，导致整层 UI 返工。正确范本见 R3。

## 1.2 组件文档（官方 35+ 组件速查）

| 分类 | 组件 | 典型用途（知牛场景） |
|---|---|---|
| 基础容器 | `View` / `Scroller` | 页面容器、卡片外壳（`backgroundImage` / `glassEffectIOS`） |
| 列表族 | `List` / `PageList` / `WaterFallList` | 行情列表（分页+预加载+首屏优化）、资讯瀑布流 |
| 文本族 | `Text` / `RichText` | 价格、涨跌幅；财务富文本 |
| 输入族 | `Input` / `TextArea` | 搜索框、聊天输入 |
| 反馈族 | `Dialog` / `AlertDialog` / `ActionSheet` / `Modal` / `Mask` / `Toast` | Key 配置弹窗、长按菜单、Agent 进度遮罩 |
| 选择族 | `Switch` / `Slider` / `CheckBox` / `DatePicker` / `ScrollPicker` | 预警设置、主题开关、模型选择 |
| 导航族 | `Tabs` / `iOSSegmentedControl` / `Carousel` / `SliderPage` | 行情 Tab、多空辩论轮播 |
| 刷新族 | `Refresh` / `FooterRefresh` | 行情下拉刷新、翻页加载 |
| 媒体族 | `Image` / `Video` / `APNG` / `PAG` | Logo、加载动画 |
| 高级 | **`Canvas`**（K 线自绘）/ **`AI Chat`**（官方 AI 对话组件）/ `LiquidGlass` / `Blur` / `Hover` / `ActivityIndicator` | K 线蜡烛图、聊天页、iOS 26+ 玻璃质感 |

**官方 Module（后端能力）**：`RouterModule`（跨页跳转）、`NetworkModule`（HTTP）、`SharedPreferencesModule`（本地 KV）、`MemoryCacheModule`（行情缓存）、`SnapshotModule`（状态持久化）、`NotifyModule`（预警推送）、`PerformanceModule`（埋点）。

## 1.3 图标与公共资源

| 资源 | 选型 | 获取方式 | 约束 |
|---|---|---|---|
| 图标 | **Google Material Symbols**（字体方式） | https://fonts.google.com/icons | 字体渲染跨渲染层通用，规避 Kuikly 非标准 Compose 的 ImageVector 兼容问题；**不自绘 SVG 图标** |
| Markdown 流式 | **KuiklyMarkdown**（社区官方贡献） | https://github.com/Kuikly-contrib/KuiklyMarkdown | AI 回答渲染首选；备选 `RichText` |
| 表格 | KuiklyTableView 或 `List` 组合 | 社区仓库 | 五档盘口、财务表 |

> ImageVector 类图标库（compose-icons / compose-material-symbols）在 Kuikly 的兼容性**未实证**，默认禁用；确需使用先按 §2 Step 6 做编译验证。

## 1.4 优秀案例（浅色金融风参考）

| 案例 | 参考点 | 获取方式 |
|---|---|---|
| **Kuikly 官方 ChatDemo** | AI 聊天页结构、Markdown 流式接法、Ktor 桥接 | 腾讯云社区搜"Kuikly ChatDemo" |
| **Kuikly 自选股生产案例** | 行情列表的 List 用法（资讯页卡迁移后 iOS 加载 -22% 的先例） | 腾讯云开发者社区文章 2585113 |
| **富途牛牛 / 雪球 / 同花顺（浅色模式）** | 浅色金融的专业感：白底卡片 + 红涨绿跌 + 等宽数字 + 三栏详情 | App Store 下载体验 / 官网截图 |
| **Material Design 3（Light）** | 浅色主题的 tonal palette 方法论、状态层（state layer）规范 | https://m3.material.io/styles/color/system |

## 1.5 参考内容获取方式汇总

```
优先级从高到低：
① 本地已有 —— zhiniu/kuikly-shell/（官方脚手架，含真实可编译 demo）+ upstream-kuiklyui/（官方仓库浅克隆）
② 官方文档 —— WebFetch kuikly.tds.qq.com 的 API / ComposeDSL / QuickStart 三页
③ 官方源码 —— github.com/Tencent-TDS/KuiklyUI 的 demo/ 目录（真实 DSL 范本）
④ 社区扩展 —— github.com/Kuikly-contrib/*（Markdown / TableView）
⑤ 体验参考 —— 浅色金融 App 截图（富途/雪球/同花顺）
```

## 1.6 收集完成 Checklist（设计开工前逐项打勾）

- [ ] R1 组件 API 目录已通读，重点组件（本页要用的）参数已记录
- [ ] R3 官方 demo 中找到 ≥1 个同类页面的真实写法范本
- [ ] 本页所需组件全部能在 §1.2 官方清单中找到（找不到的已列入扩展计划）
- [ ] 图标在 Material Symbols 中确认存在
- [ ] 附录 A 的 Light Token 已加载为本次设计的唯一色彩来源

---

# 第二部分 · 设计实施方法（六步执行）

## Step 1 · 布局规划（信息架构 → 线框）

**输入**：页面职责（来自 llm-router-design.md §6 页面矩阵）+ 数据源清单
**输出**：区块级线框（顶部 / 内容 / 底部三段式）

### 1.1 规划规则

1. **三段式骨架**：每个页面 = 顶部区（标题/Tab/搜索，高 44–56px，吸顶）+ 内容区（唯一可滚动）+ 底部区（操作/TabBar，高 48–56px，吸底）
2. **移动优先**：以 **375×812（iPhone 基准）** 起稿；Web 端居中限宽（内容区 max-width 480px，两侧留白），不铺满桌面全宽
3. **栅格**：内容区水平 padding 16px（`--space-4`）；卡片间距 12px（`--space-3`）；列表行高 ≥ 56px（保证触摸目标 44px+）
4. **区块卡片化**：内容区按职责切成圆角卡片（`--radius-lg` 12px），卡片内再分三列式数据（币安式：标签列 / 主数值列 / 变化列）
5. **跳转关系**：先画页面流（附录含 19 页跳转图），确保每个页面有进有出，不留死胡同

### 1.2 检查点

- [ ] 三段式骨架成立，内容区是唯一滚动容器
- [ ] 所有触摸目标 ≥ 44×44px
- [ ] 页面在跳转图中有明确入口与出口

## Step 2 · 视觉层级（三件套：字号 / 颜色 / 间距）

**输入**：Step 1 线框
**输出**：每个区块的层级标注

### 2.1 层级规则

1. **每个视图最多 3 层视觉层级**：一级（价格/标题，`--fs-h1` 以上 + `--color-text-primary`）→ 二级（副信息/代码，`--fs-body` + `--color-text-secondary`）→ 三级（提示/时间，`--fs-caption` + `--color-text-tertiary`）
2. **数字永远等宽**（`--font-mono`）：价格、涨跌幅、成交量——防跳动、显专业
3. **颜色只做语义**（涨/跌/警示/链接/品牌），**不做装饰**：一张卡片内的颜色种类 ≤ 3
4. **间距即层级**：同级元素间距 8px（`--space-2`），跨级区块间距 24px（`--space-6`）；用间距分组，不滥用分割线
5. **F 型阅读**：关键数据放左上→右上的扫描路径上；涨跌幅这类"结果型"数据放行尾右对齐

### 2.2 检查点

- [ ] 每个区块层级 ≤ 3 层
- [ ] 数字全部等宽字体
- [ ] 单卡片颜色 ≤ 3 种，且全部来自附录 A Token

## Step 3 · 组件选用与组合（官方优先原则）

**输入**：Step 2 层级标注
**输出**：组件清单（含每个区块的官方组件选型）

### 3.1 选型决策树（依次自上而下判断）

```
① 官方内置组件能否直接满足？ ──能──→ 用官方组件（记录 R1 文档出处）
        │不能
② 官方组件组合能否满足？（如 View+Text 拼 OHLC 卡）
        │能──→ 组合实现（每个子组件仍用官方 API）
        │不能
③ 社区官方贡献组件？(KuiklyMarkdown / KuiklyTableView)
        │是──→ 引入社区组件
        │否
④ 需要自定义绘制？(如 K 线)
        │是──→ 用官方 Canvas 自绘（数据层与绘制层分离，参考项目 KLineChart.kt 模式）
        │否
⑤ 登记【扩展组件】→ 进入 Step 4 约束流程
```

### 3.2 官方组件 → 知牛场景映射（附录 B 完整版，此处高频项）

| 场景 | 首选官方组件 | 组合方式 |
|---|---|---|
| 行情列表 | `List`（`vforLazy` + 分页 + `firstContentLoadMaxIndex` 首屏优化） | 行内 `View`+`Text`+微型 `Canvas` |
| 指数条 | 横向 `Scroller` + 卡片 `View` | — |
| Tab 切换 | `Tabs`（自研 DSL）/ `TabRow`（Compose DSL） | 指示器随选中滑移 |
| K 线 | `Canvas` 自绘 | 蜡烛+MA+十字游标（逻辑层复用 `KLineChart.kt`） |
| 聊天消息流 | 官方 `AI Chat` 组件 或 `List`+`KuiklyMarkdown` | 流式 SSE 逐字追加 |
| 服务商列表（图 2 形态） | `List` + 行内徽章 `View` + `Dialog` 配 Key | 弹窗内 `Input`+`Switch` |
| 弹窗 | `Dialog` / `Modal`（底部 sheet 用 Modal 变体） | — |
| 下拉刷新 | `Refresh`（官方手势组件） | — |

### 3.3 检查点

- [ ] 每个选型都能指出官方出处（R1/R2/R3 或 §1.2 清单）
- [ ] 没有出现清单外组件在未登记扩展的情况下使用
- [ ] K 线等自绘场景的数据计算与绘制已分层

## Step 4 · 扩展组件设计规范（按需扩展 · 严格守规）

> 原则：**扩展是官方组件的"薄封装"，不是另起炉灶**。

### 4.1 什么情况允许扩展

- 官方组件 + 组合 + Canvas 三条路都走不通（Step 3 决策树到 ⑤）
- 高频复用（≥3 个页面都要用）才值得抽成扩展组件；一次性需求直接组合实现，不抽组件

### 4.2 扩展组件硬约束（逐项对照官方规范）

| 维度 | 约束 | 违规示例（禁止） |
|---|---|---|
| **命名** | `ZN` 前缀 + 大驼峰：`ZNPriceTag` / `ZNSkeletonRow`；文件与类同名 | `MyCard` / `price_tag` |
| **尺寸** | 宽高必须取自 8 倍数间距体系或语义尺寸（行高 56 / 图标 24 / 头像 40） | 随手写 37px |
| **间距** | 内边距用 `--space-*` Token 值 | 写死 13px |
| **圆角** | 只允许 `--radius-sm/md/lg/xl/full` 五档 | 写死 10px |
| **颜色** | 只允许附录 A Token；状态色必须走语义（up/down/warn） | 硬编码 `#ff0000` |
| **字号** | 只允许 7 级字号 Token | 写死 15px |
| **交互状态** | 必须实现五态：default / hover（Web）/ pressed / disabled / loading；状态反馈用透明度叠加（pressed = 主色 12% 蒙层）而非换色 | 只有 default 一态 |
| **动效** | 时长/缓动只允许 4 档 motion Token | 自造 300ms linear |
| **无障碍** | 可点击元素提供无障碍标签；焦点态 `focus-visible` 双像素描边 | 无焦点样式 |
| **API 风格** | 属性命名对齐官方（`backgroundColor` 而非 `bg`；Float 类型如 `fontSize(14f)`） | 自造 `setColor()` |

### 4.3 扩展组件登记（附录 C 模板）

每个新扩展组件必须在 `docs/extension-components.md` 登记：

```markdown
## ZNPriceTag
- 用途：行情行右侧"价格+涨跌幅"组合
- 复用页面：MarketList / Watchlist / Search（≥3 ✅）
- 官方依据：View+Text 组合（R1 §View/Text）；字号 --fs-h3/--fs-caption；颜色 --color-up/down
- 状态五态：default / pressed(12% 蒙层) / disabled(60% 透明) / loading(骨架) / stale(黄色角标)
- 登记日期 / 负责人
```

### 4.4 检查点

- [ ] 每个扩展组件有登记条目（附录 C）
- [ ] 十项硬约束逐项自检通过
- [ ] 扩展组件内部仍然只调用官方 API

## Step 5 · 交互状态与微动效（六态全覆盖）

### 5.1 六态状态机（每个数据页面必须全实现）

| 状态 | UI 要求 | 官方组件 |
|---|---|---|
| Idle | 与 Success 同骨架占位 | `View` 占位 |
| Loading | 骨架屏（尺寸 1:1 匹配成功态，杜绝跳位） | 自组 Skeleton（`ZN` 扩展）或 `ActivityIndicator` |
| Success | 真实数据 + 入场动效 | — |
| Empty | 空态插画（Material Symbols 图标组合）+ 引导按钮 | `Image`+`Text`+`Button` |
| Error | **内嵌错误卡**（不弹全局 Toast）+ 错误码徽章 + Retry 按钮 | `Dialog` 仅用于确认类；错误用卡片 |
| Stale | 顶部黄条"数据 X 秒未更新" + 旧数据保留 | `Text` 横条 |

### 5.2 微动效（只允许 4 档 Token）

| 场景 | 动效 | Token |
|---|---|---|
| 价格变化 | 红/绿闪烁 | `--motion-flash` 600ms |
| 列表项入场 | 渐显 + 上移 8px | `--motion-fast` 150ms |
| Tab 切换 | 内容滑移 + 指示器 | `--motion-base` 250ms |
| 弹窗进出 | 缩放 0.92→1 + 蒙版淡入 | `--motion-slow` 400ms |
| AI 流式 | 逐字追加 | 即时（无动效） |
| Agent 步骤点亮 | ✓ 缩放 0→1 | `--motion-base` |

> 遵循 `prefers-reduced-motion`：用户系统开启减弱动效时，全部动效降为瞬时切换。

### 5.3 检查点

- [ ] 六态全部可演示（录屏逐一验证）
- [ ] 动效全部来自 4 档 Token，无自造时长

## Step 6 · 设计验证（对照官方文档自检）

> 三层验证，层层递进；任何一层失败即打回修正。

### 6.1 第一层：API 签名验证（静态）

- 每个用到的组件属性，逐项对照 R1（kuikly.tds.qq.com/API/）或 R2（ComposeDSL/allApi.html）原文
- DSL 写法对照 R3 官方 demo：`BasePager` / `body(){ attr{} }` / Float 字面量（`fontSize(14f)`）/ `observable` delegate / `List{ vforLazy(...) }`
- **红线**：出现官方文档查无出处的属性或包名（如 `com.tencent.kuikly.ref.*`）= 立即打回

### 6.2 第二层：编译验证（动态）

```
隔离 JDK17（zhiniu/.toolchains/ 已备）：
  JAVA_HOME=<隔离JDK17> GRADLE_USER_HOME=.gradle-home \
  ./gradlew :shared:compileKotlinJs        # Web 主验证
  ./gradlew :shared:compileKotlinIosSimulatorArm64   # iOS 交叉验证
```
- 编译零 error 才进入下一层（warning 逐条评估）

### 6.3 第三层：视觉走查（运行时）

- Web 端浏览器打开（8090 渲染宿主 + 8083 业务 bundle），逐页对照：
  - **Token 合规**：取色器抽查 ≥5 处，全部命中附录 A Token
  - **对比度**：正文 ≥ 4.5:1（用 WCAG 计算器抽查文字/背景组合，附录 A 已预验证）
  - **触目标准**：可点元素 ≥ 44px
  - **六态触发**：断网 / 无 Key / 空列表三种手段逐一触发 Empty/Error/Stale

### 6.4 40 项整体验证 Checklist

见**附录 D**（打印随行）；重点摘要：

- 布局类 8 项（三段式 / 滚动容器唯一 / 触摸目标 / 限宽…）
- Token 类 12 项（颜色全命中 / 字号 7 级 / 间距 8 倍数 / 圆角 5 档 / 等宽数字…）
- 组件类 10 项（全部官方出处 / 扩展已登记 / Canvas 分层…）
- 状态类 6 项（六态全覆盖 / Retry 可达 / 骨架不跳位…）
- 无障碍 4 项（对比度 / 焦点态 / 标签 / reduced-motion）

---

# 附录

## 附录 A · Light Theme Design Token（唯一权威色板）

> 全部组合已按 WCAG AA 预验证（正文 4.5:1，大字/图形 3:1）。禁止使用本表之外的任何颜色值。

### A.1 中性色（浅色四层）

| Token | 值 | 用途 | 对白底对比度 |
|---|---|---|---|
| `--color-bg-page` | `#F5F7FA` | 页面底（微灰，衬托白卡） | — |
| `--color-bg-card` | `#FFFFFF` | 卡片底 | — |
| `--color-bg-hover` | `#F0F3F7` | 悬停 / 二级面板 | — |
| `--color-border` | `#E5E9F0` | 描边 / 分割线 | — |
| `--color-text-primary` | `#1F2329` | 主文字 | 15.9:1 ✅ |
| `--color-text-secondary` | `#4E5561` | 次要文字 | 7.5:1 ✅ |
| `--color-text-tertiary` | `#8A919C` | 提示（仅 caption） | 3.5:1（限 12px 辅助） |

### A.2 金融语义色（浅底加深的版本）

| Token | 值 | 用途 | 对白底对比度 |
|---|---|---|---|
| `--color-up` | `#D93025` | 涨（A 股红涨） | 4.7:1 ✅ |
| `--color-down` | `#1E8E3E` | 跌（大号数字场景） | 3.9:1（≥18px ✅） |
| `--color-warn` | `#B45309` | 警示 / Stale | 6.0:1 ✅ |
| `--color-info` | `#7C3AED` | 资讯标签 | 5.2:1 ✅ |

### A.3 品牌色

| Token | 值 | 用途 | 对白底对比度 |
|---|---|---|---|
| `--color-brand` | `#00A870` | 知牛主绿（按钮 / Tab 选中 / 品牌元素） | 3.0:1（限大元素） |
| `--color-brand-deep` | `#00875A` | 主绿文字版（可读场景） | 4.6:1 ✅ |
| `--color-link` | `#1A73E8` | 链接 | 4.6:1 ✅ |
| `--color-scrim` | `rgba(31,35,41,.45)` | 弹窗蒙版 | — |

### A.4 字体 / 字号 / 行高

| Token | 值 | 用途 |
|---|---|---|
| `--font-sans` | system-ui, -apple-system, "PingFang SC", "Microsoft YaHei" | 全局 |
| `--font-mono` | "JetBrains Mono", "SF Mono", Menlo, monospace | **所有数字** |
| `--fs-display` | 32px / 500 / lh 1.2 | 详情页大价 |
| `--fs-h1` | 24px / 500 / lh 1.2 | 页面标题 |
| `--fs-h2` | 20px / 500 / lh 1.3 | 区块标题 |
| `--fs-h3` | 16px / 500 / lh 1.4 | 列表项标题 |
| `--fs-body` | 14px / 400 / lh 1.5 | 正文 |
| `--fs-caption` | 12px / 400 / lh 1.5 | 提示 / 角标 |

### A.5 间距（8 倍数）

```
--space-1:4px  --space-2:8px  --space-3:12px  --space-4:16px
--space-5:20px --space-6:24px --space-8:32px  --space-12:48px
```

### A.6 圆角 / 阴影 / 动效

| Token | 值 | 用途 |
|---|---|---|
| `--radius-sm/md/lg/xl/full` | 6 / 8 / **12（卡片默认）** / 16 / 9999px | 徽章 / 控件 / 卡片 / 弹窗 / 圆形 |
| `--shadow-card` | `0 1px 3px rgba(31,35,41,.08)` | 卡片（浅色阴影要轻） |
| `--shadow-pop` | `0 8px 24px rgba(31,35,41,.14)` | 弹窗 / 下拉 |
| `--motion-fast/base/slow/flash` | 150ms / 250ms / 400ms ease-out / 600ms | 见 §2 Step 5 |

## 附录 B · 官方组件 → 知牛 19 页映射总表

> 与 llm-router-design.md §6 页面矩阵一一对应；此处列出每页的组件组合要点。

| 页面 | 官方组件组合 | 扩展组件（ZN） |
|---|---|---|
| SplashPage | `View`+`Image` | — |
| HomePage / MainTabBar | `Tabs`（4 Tab） | `ZNTabIcon`（如需） |
| MarketListPage | `Refresh`+`List`+`Tabs`+`View`+`Text`+`Canvas`(迷你图) | `ZNPriceTag` / `ZNSkeletonRow` |
| IndexListPage | 同上 | 复用 |
| StockSearchPage | `Input`+`List`+`Text` | `ZNHotTag`(词云) |
| StockDetailPage | `Tabs`+`Canvas`+`View`(五档)+`Text` | `ZNOhlcard` / `ZNDebateEntry` |
| IndexDetailPage | 复用详情骨架 | — |
| SectorBoardPage | `List`+`Text` | `ZNSectorRow` |
| NewsListPage | `Tabs`+`PageList`+`Refresh`+`FooterRefresh`+`Image` | `ZNNewsCard` |
| NewsDetailPage | `Scroller`+`RichText`+`Text` | — |
| ChatHomePage | 官方 `AI Chat` 或 `List`+`KuiklyMarkdown`+`TextArea`+`Button` | `ZNQuickChip` / `ZNAgentTimeline` |
| ChatSessionListPage | `List` | `ZNSessionRow` |
| ChatDetailPage | 同 ChatHome 主区 | `ZNToolCallCard`(工具调用灰卡) |
| ModelSettingsPage | `Input`(搜索)+`List`(服务商)+`Dialog`(配Key)+`Switch` | `ZNProviderRow` / `ZNStatusBadge` |
| DebateViewPage | `Carousel`+`Tabs`+`Text` | `ZNDebateBubble` |
| AgentProgressOverlay | `Mask`+`Modal`+`Text` | 复用 `ZNAgentTimeline` |
| WatchlistPage | `List`(拖拽)+`Button` | 复用 `ZNPriceTag` |
| AlertSettingsPage | `List`+`Dialog`+`ScrollPicker`+`Input` | `ZNAlertRow` |
| UserSettingsPage | `iOSSegmentedControl`+`Switch`+`Text` | `ZNSettingRow` |

## 附录 C · 扩展组件登记模板

```markdown
## ZN<Name>
- 用途：
- 复用页面（≥3 才登记）：
- 官方依据（R1/R2/R3 章节 + 组合的官方组件）：
- Token 依赖（颜色/字号/间距/圆角/动效各来自哪个 Token）：
- 五态设计（default/hover/pressed/disabled/loading）：
- 无障碍（标签 / 焦点态）：
- 登记日期：
```

## 附录 D · 40 项设计验证 Checklist

**布局（8）**
- [ ] 三段式骨架（顶/内容/底）
- [ ] 内容区是唯一滚动容器
- [ ] 移动优先 375px 起稿，Web 限宽居中
- [ ] 内容区水平 padding = 16px
- [ ] 卡片间距 = 12px
- [ ] 列表行高 ≥ 56px
- [ ] 触摸目标 ≥ 44×44px
- [ ] 页面有进有出（跳转图验证）

**Token（12）**
- [ ] 页面底 #F5F7FA / 卡片 #FFFFFF
- [ ] 文字三色来自 A.1
- [ ] 涨跌色 #D93025 / #1E8E3E（红涨绿跌）
- [ ] 品牌绿仅用于大元素，文字用 deep 版
- [ ] 单卡片颜色 ≤ 3 种语义色
- [ ] 字号全部来自 7 级 Token
- [ ] 数字全部等宽字体
- [ ] 间距全部 8 倍数
- [ ] 圆角全部 5 档内
- [ ] 阴影 2 档内且为浅色阴影
- [ ] 动效全部 4 档 Token
- [ ] 无任何硬编码色值（取色器抽查 ≥5 处）

**组件（10）**
- [ ] 每个组件能指出 R1/R2/R3 出处
- [ ] 无臆造包名/属性（对照官方 demo）
- [ ] DSL 写法符合官方范本（BasePager/attr{}/Float）
- [ ] 扩展组件全部已登记（附录 C）
- [ ] 扩展组件十项硬约束自检通过
- [ ] 图标全部 Material Symbols 字体
- [ ] K 线数据层与绘制层分离
- [ ] 列表用 List+vforLazy（非手写循环）
- [ ] 编译验证通过（JS + iOS 双目标）
- [ ] 无未验证的第三方 Compose 组件

**状态（6）**
- [ ] Idle / Loading 骨架 / Success / Empty / Error+Retry / Stale 六态全实现
- [ ] 骨架屏与成功态 1:1 尺寸（不跳位）
- [ ] 错误用内嵌卡片而非全局 Toast
- [ ] Retry 按钮一键可达
- [ ] Stale 黄条不遮挡旧数据
- [ ] reduced-motion 时动效降级

**无障碍（4）**
- [ ] 正文对比度 ≥ 4.5:1（附录 A 已预验证，抽查）
- [ ] 焦点态可见（focus-visible）
- [ ] 可点元素有无障碍标签
- [ ] 200% 文字缩放不破版

## 附录 E · 反模式清单（出现即打回）

1. ❌ 暗色背景混入浅色页面（Light Theme 是唯一主题）
2. ❌ 使用 `com.tencent.kuikly.ref.*` 等臆造包名（真实包是 `com.tencent.kuikly.core.*`）
3. ❌ 硬编码颜色 `Color(0xFF123456)`、尺寸 `37f`、时长 `300`（必须走 Token）
4. ❌ 绿涨红跌（中国习惯是**红涨绿跌**）
5. ❌ 用 Toast 报错（错误必须内嵌卡片 + Retry）
6. ❌ 自绘 SVG 图标（必须 Material Symbols 字体）
7. ❌ 一次性需求抽扩展组件（≥3 页复用才抽）
8. ❌ 扩展组件未登记就合入（附录 C 流程必走）
9. ❌ 跳过编译验证直接宣称完成
10. ❌ 数字用比例字体（必须等宽）

---

## 修订记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-24 | 初版：整合技术方案/组件选型/UI 规范/路由设计四文档；确立 Light Theme 唯一权威；官方组件优先 + 扩展守规 + 三层验证方法论 |
