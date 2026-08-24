# 知牛 · 多 LLM 统一路由管理设计（借鉴 Cherry Studio）

> 版本：v1.0 · 2026-08-24
> 背景：用户指示——LLM 不是简单接一个模型，要参考 [CherryHQ/cherry-studio](https://github.com/CherryHQ/cherry-studio) 做多厂商适配、统一路由、内容管理的完整能力；其他端暂停，先完成 Web 端。
> 本文档 = 调研结论 + 差距对照 + 增强设计 + Web 冲刺计划。

---

## 1. Cherry Studio 调研结论（2026-08-24 实测仓库）

> 项目体量：PR 编号已达 #19236+，近两万个 PR，9+ 活跃贡献者，2026-08 连续多日密集提交，工程化成熟度极高（CI/CD + 分支保护 + i18n 门禁 + changeset 版本管理）。技术栈：Electron + React + TypeScript，pnpm monorepo。

### 1.1 它的多 LLM 管理 core design（我们借鉴的四件事）

| # | 设计 | Cherry Studio 实现 | 对知牛的价值 |
|---|---|---|---|
| ① | **声明式 Provider preset** | 每个 provider 一个 TS 配置文件（`packages/provider-registry/src/providers/*.ts`）：`baseUrl` + `endpointTypes` + `adapterFamily` + `defaultChatEndpoint`，编译生成 `providers.json` / `models.json` / `provider-models.json` catalog | Provider 配置数据化：新增厂商 = 加一个 JSON 条目，不改业务代码 |
| ② | **模型自动发现** | 从 provider 自身 API（`/models`）拉取模型列表 + 中央 `models.json` 目录（含能力标签、contextWindow 元数据） | 网关有 Key 时动态聚合各厂商真实模型清单，而非手写死 |
| ③ | **三层端点解析链** | `模型级 endpointTypes → 网关路由 resolveGatewayChatRoute → provider defaultChatEndpoint`，优先级明确 | 路由不只是全局降级链：模型可覆盖路由（vision 走 vision 模型、reasoning 走 reasoner） |
| ④ | **配置热更新** | catalog 发布到数据镜像分支，CI 签名分发到所有客户端，模型配置免发版更新 | 知牛场景简化：providers.json 放后端，重启即生效，无需发版 |

### 1.2 它踩过的坑（我们直接绕开）

| 坑 | Cherry Studio Issue | 知牛对策 |
|---|---|---|
| 模型不兼容被**静默过滤**（选择器里直接消失、无解释） | #18745 | 设置页/模型列表对不可用模型**明示原因**（缺 Key / 限流 / 不支持端点），绝不静默 |
| 动态 provider 省略 `defaultChatEndpoint` 导致端点解析失败 | #19006 | 每个 provider 必须声明 `defaultModel`，配置加载时校验完整性 |
| FTS 索引泄漏隐藏 reasoning 内容 | #17661 | 网关日志不落盘 reasoning_content；会话历史仅存用户侧 |

---

## 2. 知牛网关现状 vs Cherry Studio 差距对照

现有 `backend/app/config.py` 的能力与缺口：

| 能力 | 现状（config.py） | Cherry Studio | 差距 → 增强动作 |
|---|---|---|---|
| Provider 注册 | Python dict 硬编码（3 厂商） | 声明式 preset + catalog | **P0**：抽到 `providers.json`，运行时加载 |
| 模型清单 | 手写 `models` 列表 | API 自动发现 + 中央目录 | **P0**：网关加 `refresh_models()`（有 Key 时拉 `/v1/models` 聚合缓存） |
| 路由 | 别名降级链（zhiniu/quick…） | 三层解析（模型级→网关级→provider 默认） | **P1**：models.json 加 `capabilities`（vision/reasoning/fast），别名解析时按能力过滤候选 |
| 模型元数据 | 无 | 能力标签 + contextWindow | **P1**：随 providers.json 一起声明 |
| Key 状态 | `/v1/models` 返回 `key_configured` | 设置 UI 管理 | **P1**：`/healthz` 扩展 per-provider 探活（真实调一次轻请求） |
| 管理 UI | 无 | 完整设置页 | **P0（Web 端）**：新增「模型设置页」（见 §4） |
| 错误结构化 | ✅ ProviderError 5 类 + 中文提示 | 类似 | 已达标，保持 |

> 结论：我们的**降级链和错误处理已达标甚至更细**（结构化 reason + 中文排障提示是加分项）；核心差距在「**声明式配置 + 自动发现 + 管理 UI**」三件事，正好都是 Web 端可交付的。

---

## 3. 增强设计：统一路由管理（后端）

### 3.1 providers.json（声明式配置，替代硬编码 dict）

```json
{
  "providers": [
    {
      "id": "deepseek",
      "baseUrl": "https://api.deepseek.com",
      "apiKeyEnv": "DEEPSEEK_API_KEY",
      "defaultModel": "deepseek-chat",
      "models": [
        { "id": "deepseek-chat",    "capabilities": ["fast", "chat"] },
        { "id": "deepseek-reasoner","capabilities": ["reasoning"] },
        { "id": "deepseek-v4-pro",  "capabilities": ["vision", "chat"] }
      ]
    },
    {
      "id": "glm",
      "baseUrl": "https://open.bigmodel.cn/api/paas/v4",
      "apiKeyEnv": "ZHIPUAI_API_KEY",
      "defaultModel": "glm-4.6",
      "models": [
        { "id": "glm-4.6", "capabilities": ["fast", "chat"] },
        { "id": "glm-4.5", "capabilities": ["vision"] }
      ]
    },
    {
      "id": "hunyuan",
      "baseUrl": "https://api.hunyuan.cloud.tencent.com/v1",
      "apiKeyEnv": "HUNYUAN_API_KEY",
      "defaultModel": "hunyuan-turbos-latest",
      "models": [
        { "id": "hunyuan-turbos-latest", "capabilities": ["fast", "chat"] }
      ]
    }
  ],
  "routes": {
    "zhiniu/quick":  { "want": ["fast"],      "fallbackOrder": ["deepseek", "glm", "hunyuan"] },
    "zhiniu/think":  { "want": ["reasoning"], "fallbackOrder": ["deepseek", "glm", "hunyuan"] },
    "zhiniu/flash":  { "want": ["fast"],      "fallbackOrder": ["glm", "deepseek", "hunyuan"] },
    "zhiniu/vision": { "want": ["vision"],    "fallbackOrder": ["deepseek", "glm", "hunyuan"] }
  }
}
```

**解析规则（三层，对齐 Cherry Studio）**：

```
1. 模型级：显式 "deepseek/deepseek-reasoner" → 单候选直连
2. 路由级：别名 "zhiniu/think" → fallbackOrder 顺序 × want 能力过滤
   （某厂商缺 Key / 模型无该能力 → 跳过并在响应头 X-Gateway-Skip 标注原因）
3. Provider 默认级：纯模型名 → 按声明顺序找第一个提供者
```

### 3.2 模型自动发现（新增端点）

```
POST /admin/refresh-models
  → 对每个 key_configured 的厂商真实调 GET {baseUrl}/models
  → 结果并入内存模型目录（capabilities 未知时标记 "discovered": true）
  → 返回 { provider, discovered: n, error?: reason }
GET /v1/models
  → 聚合：声明的 + 发现的 + 每个的 key_configured / capabilities / available
```

**规则**（绕开 Cherry Studio 的静默过滤坑）：

- 不可用模型**仍出现在列表**，带 `"available": false, "reason": "no_key" | "rate_limited"`，前端置灰但可见
- `/admin/*` 用 `GATEWAY_API_KEY` 保护，不设 Key 时仅本机可访问

### 3.3 兼容性保持

- `resolve_candidates()` 签名不变（读 JSON 而非 dict，逻辑等价迁移）
- `ALIASES` → `routes`；`PROVIDERS` → `providers.json`；旧 `deepseek/deepseek-chat` 显式形式继续可用
- 单测全部保持（9 项）+ 新增：配置加载校验（缺 defaultModel 报错）、能力过滤、发现聚合

---

## 4. Web 端新增页面：模型设置页（ModelSettingsPage）

> 借鉴 Cherry Studio 的设置面板，落到知牛 Web 端（比赛「AI 场景设计 25%」+「体验优化」加分项的直贡献）。

### 4.1 页面结构（Kuikly core DSL，与三页同构）

| 区块 | 内容 | 数据源 |
|---|---|---|
| Provider 列表 | 每厂商一行：名称 / base_url / Key 状态徽章（✅已配置 / ⚠️未配置）/ 模型数 | `GET /v1/models` 的 provider 段 |
| 模型列表 | 按厂商分组；每模型：id / 能力标签（fast·reasoning·vision）/ 可用性（可用置蓝、不可用置灰+原因） | 同上 |
| 别名路由面板 | 4 条路由（quick/think/flash/vision）各自的降级链可视化：`deepseek → glm → hunyuan` 箭头链 | `routes` 段 |
| 健康检查 | 「检测连通性」按钮 → 逐厂商探活结果 + 延迟 ms | `GET /healthz?probe=true` |
| 操作 | 「刷新模型列表」（admin，需网关 Key） | `POST /admin/refresh-models` |

### 4.2 交互细节（对齐 §12 的五要素规范）

- Key 未配置：行内黄条提示「在服务端 .env 填入 XXX_API_KEY 后重启网关」——**不静默**（Cherry Studio #18745 教训）
- 探活中：按钮 Loading + 每行独立 spinner；完成逐行点亮 ✅/❌ + 延迟
- 路由链可视化：候选用 `→` 连接，被跳过的候选标删除线 + 悬停显示原因（缺 Key / 无该能力）

### 4.3 入口

聊天页右上「模型」图标 → `openPage("ModelSettings")`；设置页返回回聊天页。路由名注册进 `@Page` 体系。

---

## 5. Web 端聚焦冲刺计划（其他端暂停）

> 指令：iOS / Android / 鸿蒙 / macOS 全部挂起，现有编译产物保留不删；全部精力收敛到 Web(H5) 一条线打穿。

### 5.1 冲刺任务清单（按序，P0 先行）

| # | 任务 | 验收标准（Web 浏览器） | 评分映射 |
|---|---|---|---|
| W-A1 | providers.json 声明式配置迁移 | 网关启动读 JSON；单测 9+3 过 | 25% 工程质量 |
| W-A2 | 模型自动发现 + /admin/refresh-models | 配 Key 后能拉到厂商真实模型清单 | 10% 真实 API |
| W-A3 | 聊天页接通真实 LLM（网关 SSE → SseParser → 流式气泡） | 输入问题，逐字流式回答（无 Key 时 Mock 也流式） | 25% AI 场景 |
| W-A4 | ModelSettingsPage（§4 全部区块） | 可视化 provider/模型/路由/健康 | 25% AI 场景 + 体验 |
| W-A5 | 多空辩论气泡（PR-09） | 「换个角度看」→ 多/空轮播 + 分歧高亮 | 25% 创新场景 |
| W-A6 | K 线 Canvas 真渲染（KLineChart 逻辑层已就绪） | 详情页蜡烛图 + MA5/10/20 + 十字游标 | 40% 功能 |
| W-A7 | 状态机六态 Web 全验证（骨架/空/错/重试/Stale） | 逐态录屏 | 40% 状态完整性 |
| W-A8 | 演示收尾：8083+8090 双 server 一键脚本 + 录屏 | ≤90s 视频覆盖全链路 | 交付物 |

### 5.2 明确暂停项（恢复条件）

| 暂停项 | 恢复触发条件 |
|---|---|
| iOS 模拟器运行 | 用户 Xcode 下载 iOS Simulator Runtime 后 |
| Android installDebug | 用户配置 Android SDK / 模拟器后 |
| 鸿蒙 / macOS | Web 端全部 P0 完成且时间富余 |

### 5.3 比赛要求（评分）对照——Web 单端能拿多少分

| 维度 | Web 端可达成 | 依赖 |
|---|---|---|
| 功能完整性 40% | ✅ 全额可达成（三页+设置页+六态全在 Web 验证） | W-A1~A8 |
| 工程质量 25% | ✅ 全额（四层 + 单测 + 声明式配置是亮点） | W-A1 |
| AI 场景 25% | ✅ 全额（真实 LLM + 流式 + 辩论 + 模型管理可视化） | W-A3~A5 + Key |
| 加分 10% | 🔶 部分（真实 API✅、体验优化✅；多端覆盖 4 分暂弃） | Web 完成后再回补多端 |

> 策略结论：**Web 打穿 = 40+25+25+6 ≈ 96 分的上限路径**，多端 4 分作为时间富余后的回补项，不阻塞主线。


### 5.4 业务深度冲刺清单（W-B，借鉴 Cherry Studio 真实使用形态）

> §5.1 是工程冲刺，本节是**业务深度冲刺**——把"日常会用"的体验补齐。

| # | 任务 | 验收标准 | 评分映射 |
|---|---|---|---|
| W-B1 | **14 家预置服务商**集成到 providers.json | 配置生效，UI 列出全部 | 10% 真实 API |
| W-B2 | **ModelSettingsPage** 服务商卡片 + 弹窗填 Key + 一键探活 | 浏览器端可演示填 Key → 看到模型列表 | 25% AI 场景 + 体验 |
| W-B3 | **StockSearchPage** 实时搜索 + 联想 + 历史 | 输入 ticker/拼音/汉字联想可点 | 40% 功能 |
| W-B4 | **NewsListPage / NewsDetailPage** 资讯流（7×24 + 个股） | 至少 2 种 tab + AI 摘要 | 40% 功能 |
| W-B5 | **IndexListPage / IndexDetailPage** 指数完整闭环 | 主流指数列表 + 点击进详情 | 40% 功能 |
| W-B6 | **SectorBoardPage** 申万行业板块 | 列表 + 涨跌幅排行 | 40% 功能 |
| W-B7 | **ChatSessionListPage / ChatDetailPage** 会话管理 + 流式 | 多会话管理 + 流式渲染 | 25% AI |
| W-B8 | **WatchlistPage / AlertSettingsPage** 自选 + 预警 | CRUD 完整 + 拖拽排序 | 40% 功能 |
| W-B9 | **Function Calling 7 工具** 后端实现 + 工具调用可视化 | 聊天页能跑通"看茅台 PE" → 工具调 → 返回 | 25% AI 创新 |
| W-B10 | **多空辩论 DebateViewPage**（PR-09 闭环） | 多/空轮播 + 分歧高亮 | 25% AI 创新 |
| W-B11 | **AgentProgressOverlay** 全屏执行进度 | 4 Agent 逐个点亮 | 25% AI 体验 |
| W-B12 | **Analytics 用量面板**（v1 轻量版） | 3 张卡 + 调用分布图 | 10% 体验 + 加分 |


---

## 6. UI/UX 完整页面矩阵（17+ 页面，对齐评图「功能完整性 40%」）

> 当前 Web 只有 4 页（Home / MarketList / StockDetail / ChatHome），离真实可用的股票 App 差距巨大。本节把"全功能 Web 端"所需的页面铺开，按 5 大类组织，每个页面给出最小可用规格。

### 6.1 页面总览（5 大类，19 页）

| 大类 | 页面 | 状态 | 主要职责 |
|---|---|:---:|---|
| **启动 / 导航** | SplashPage | 🆕 | 品牌闪屏 300ms → 进 Home |
| | HomePage（Tab 容器） | ✅ 已有 | 承载底部 4 Tab 切换 |
| | MainTabBar | 🆕 | 底部导航（行情 / 资讯 / AI / 我的） |
| **行情** | MarketListPage | ✅ 已有 | 自选/全部/涨幅/跌幅 Tab + 个股行 |
| | IndexListPage | 🆕 | 指数列表（上证/深证/创业板/北证 50/恒生/纳指/道指） |
| | StockSearchPage | 🆕 | 顶部搜索 + 联想 + 历史 + 热门 |
| | StockDetailPage | ✅ 已有 | OHLC + 五档 + K 线 + AI 诊股 |
| | IndexDetailPage | 🆕 | 指数详情（复用 StockDetail 骨架，无五档） |
| | SectorBoardPage | 🆕 | 申万一级/二级行业 + 涨跌幅排行 + 板块详情 |
| **资讯 / 社区** | NewsListPage | 🆕 | 7×24 快讯流 / 个股新闻 / 大盘解读 |
| | NewsDetailPage | 🆕 | 资讯正文 + 相关个股标签 + AI 摘要 |
| **AI** | ChatHomePage | ✅ 已有 | 4 快捷指令 + 会话入口 + 模型入口 |
| | ChatSessionListPage | 🆕 | 历史会话列表（侧栏 / 抽屉） |
| | ChatDetailPage | 🆕 | 单会话流式 + 工具调用可视化 + 卡片 |
| | ModelSettingsPage | ✅ §4 已设计 | 服务商列表 + Key 状态 + 模型管理 |
| | DebateViewPage | 🆕 | 多空辩论气泡轮播（PR-09 闭环） |
| | AgentProgressOverlay | 🆕 | AI 执行进度全屏遮罩（基本面→技术→舆情→风控） |
| **自选 / 设置** | WatchlistPage | 🆕 | 自选股分组管理（默认 / 自建 / 拖拽排序） |
| | AlertSettingsPage | 🆕 | 预警条件（价格突破 / 涨跌幅阈值 / 异动放量） |
| | UserSettingsPage | 🆕 | 主题（深/浅/跟随系统）/ 字号 / 关于 / 免责声明 |

> 增量 14 页（🆕），加上已有 5 页 = **总 19 页**。对照图 1（当前只有 MarketList 一页的简陋状态），需要补的量很清楚。

### 6.2 关键页面规格（节选，避免文档膨胀）

**StockSearchPage（搜索页）**

- 数据：实时搜索（SSE 流式联想）+ 热搜词（10 个）+ 历史（本地存 10 条）
- 交互：顶部输入框获焦 → 联想下拉浮层（每次按键触发防抖 300ms）→ 点击联想项 → 跳详情；底部"热门"标签云 + "最近搜索"列表
- 状态：Idle（空）/ Typing（联想中 0.6s 超时）/ Success（联想结果）/ Empty（无匹配）

**ModelSettingsPage（完整规格补充）**

- 服务商区域（参考图 2 形态）：每家一行卡片，左侧 logo + 名称，右侧状态徽章（✅/⚠️/❌） + 「配置」按钮
- 已配置项：展开后显示 Key 末四位 + 模型数 + 探活延迟
- 未配置项：行变浅灰 + 「添加 Key」按钮 → 弹窗填 Key + 选服务地域（如有）
- 顶部搜索框（参考图 2 的「搜索模型平台」）：按键过滤服务商
- 底部「+ 添加服务商」 → 弹窗让用户填自定义 baseUrl（自托管/代理/第三方聚合）
- Key 安全：Key 存后端 / 配置文件，**前端永不回传原始 Key**（只显示后四位）

**ChatDetailPage（单会话流式页）**

- 顶部：会话标题（点击可改名）+ 模型徽章（点击跳 ModelSettings 切换）
- 中部：消息列表（用户右 / AI 左 / 工具调用灰色提示 / 结构化卡）
- 底部：输入框（多行自适应）+ 发送按钮 + 停止按钮（流式中变红）
- Agent 进度：流式时顶部出现 `AgentProgressOverlay`，逐个点亮「基本面 ✓ → 技术 ✓ → 舆情 ⟳ → 风控 ○」

**DebateViewPage（多空辩论）**

- 入口：StockDetail AI 诊股结果下「换个角度看」/ ChatDetail 提及「多空」时跳转
- 布局：上半屏多空气泡轮播（左右切换卡片）+ 下半屏分歧点高亮清单
- 交互：卡片左右滑切多/空 + 顶部 Tab 切换 2 轮辩论 + 「总结」按钮 → 收束为中立结论卡

**AlertSettingsPage（预警）**

- 数据：当前自选股 + 各股已设预警条数（角标）
- 交互：单股 → 「添加预警」 → 弹窗：条件类型（价格突破 / 涨跌幅阈值 / 异动放量 / 财报日历）+ 阈值输入 + 推送渠道（Web 通知 / 邮件占位）
- 状态：列表已存预警 / 「+ 新建」入口

### 6.3 页面间跳转关系（部分）

```
HomePage (Tab Bar)
├── 行情 Tab → MarketListPage → StockDetailPage / IndexListPage → IndexDetailPage
│                                ↘ SearchPage (搜索)
│                                ↘ SectorBoardPage
├── 资讯 Tab → NewsListPage → NewsDetailPage
├── AI Tab → ChatHomePage → ChatSessionListPage → ChatDetailPage
│                              ↘ ModelSettingsPage
│                              ↘ DebateViewPage (从 ChatDetail / StockDetail 进入)
└── 我的 Tab → WatchlistPage / AlertSettingsPage / UserSettingsPage
```

---

## 7. 服务商集成设计（参考图 2，14 家预置 + 1 个自定义）

> 用户明确要求"日常会用的服务商即可，只需填 Key 就能用"。**不要逐个模型配 capability**——选服务商 → 填 Key → 自动拉取该平台所有模型，能力按模型名启发式判断。

### 7.1 预置服务商清单（14 家 + 自定义）

| 名称 | 协议 | 站点 | baseUrl | 备注 |
|---|---|---|---|---|
| **OpenAI** | OpenAI | openai.com | `https://api.openai.com/v1` | 国际最主流 |
| **Azure OpenAI** | OpenAI | azure.com | 用户填 | 企业部署 |
| **DeepSeek** | OpenAI | deepseek.com | `https://api.deepseek.com` | 国产首选，价格低 |
| **智谱 GLM** | OpenAI | bigmodel.cn | `https://open.bigmodel.cn/api/paas/v4` | 国产 + 工具调用强 |
| **硅基流动** | OpenAI | siliconflow.cn | `https://api.siliconflow.cn/v1` | 多模型聚合 + 限时免费 |
| **阿里通义千问** | OpenAI（DashScope 兼容层） | dashscope.aliyuncs.com | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 国产大厂 |
| **月之暗面 Kimi** | OpenAI（Moonshot 兼容） | moonshot.cn | `https://api.moonshot.cn/v1` | 长上下文 |
| **字节豆包** | OpenAI（火山引擎） | volcengine.com | 用户填（地域相关） | 国产大厂 |
| **百度千帆** | OpenAI（千帆兼容） | qianfan.baidubce.com | `https://qianfan.baidubce.com/v2` | 国产大厂 |
| **腾讯混元** | OpenAI | hunyuan.cloud.tencent.com | `https://api.hunyuan.cloud.tencent.com/v1` | 知牛框架同源 |
| **OpenRouter** | OpenAI | openrouter.ai | `https://openrouter.ai/api/v1` | 全球模型聚合 |
| **CherryIN** | OpenAI | cherryin.ai | `https://api.cherryin.ai/v1` | 国内聚合 |
| **AiHubMix** | OpenAI | aihubmix.com | `https://aihubmix.com/v1` | 国内聚合 |
| **DMXAPI** | OpenAI | dmxapi.com | `https://www.dmxapi.com/v1` | 国内聚合 |
| **+ 自定义** | OpenAI | — | 用户填 | 自托管 / 代理 / 第三方 |

> 选型标准：**OpenAI 协议覆盖 14/15**（极个别需兼容层），意味着网关代码无需为某家定制——和现有 config.py 的"三厂商 OpenAI 兼容"判断完全一致。

### 7.2 服务商配置文件（providers.json 扩展）

```json
{
  "providers": [
    {
      "id": "openai",
      "name": "OpenAI",
      "logo": "🟢",
      "baseUrl": "https://api.openai.com/v1",
      "apiKeyEnv": "OPENAI_API_KEY",
      "protocol": "openai",
      "supportsDiscover": true,
      "keyHint": "以 sk- 开头",
      "tier": "international"
    },
    {
      "id": "deepseek",
      "name": "DeepSeek",
      "logo": "🐋",
      "baseUrl": "https://api.deepseek.com",
      "apiKeyEnv": "DEEPSEEK_API_KEY",
      "protocol": "openai",
      "supportsDiscover": true,
      "keyHint": "以 sk- 开头",
      "tier": "domestic"
    },
    {
      "id": "azure",
      "name": "Azure OpenAI",
      "logo": "☁️",
      "baseUrl": null,
      "apiKeyEnv": "AZURE_OPENAI_API_KEY",
      "protocol": "openai",
      "supportsDiscover": false,
      "keyHint": "需同时配置 AZURE_OPENAI_ENDPOINT",
      "tier": "international",
      "requiresEndpoint": true
    }
  ]
}
```

### 7.3 用户体验路径（极简）

```
1. 首次进入 ModelSettingsPage
   → 看到 14 家服务商列表（参考图 2 形态）
   → 状态徽章全部 ⚠️ 未配置

2. 点击某家「配置」
   → 弹窗：仅 1 个输入框（Key）+ 1 个"使用代理"开关
   → 提交 → 后端校验（curl /models 一次）→ 成功显示该家模型数

3. 自动刷新该家模型列表
   → 后端调 GET {baseUrl}/models → 聚合到 /v1/models 响应
   → 前端看到"已配置 12 个模型"展开列表

4. 回到聊天页
   → 模型下拉新增该家所有模型（按 tier 分组）
   → 选一个 → 立即可用

总时间：1 分钟内可完成"选服务商 → 填 Key → 获得所有模型"全流程。
```

### 7.4 模型能力自动启发式（不需手工配）

| 模型名模式 | 自动打 capability 标签 |
|---|---|
| `*-reasoner` / `*-thinking` / `o1*` / `o3*` / `deepseek-r1*` | `reasoning` |
| `*-vision` / `*-vl` / `glm-4v*` / `gpt-4o*` | `vision` |
| `*-mini` / `*-flash` / `*-lite` / `glm-4-flash` | `fast` |
| `*32k*` / `*128k*` / `*-long*` / `*200k*` | `long_context` |
| 其余 | `chat`（默认） |

> 启发式匹配不准确时仍可后端手修 `models.json` 覆盖；与自动发现共存。

---

## 8. Function Calling 多工具设计

> 评图 AI 场景 25% 的核心武器——"AI 能力贴合股票业务"（买卖/趋势/风险/信号/问答/图表联动）由 7 个工具承载。

### 8.1 工具清单（7 个）

| # | 工具名 | 用途 | 数据源 | 触发场景 |
|---|---|---|---|---|
| 1 | `get_realtime_quote` | 单股实时报价 + 五档 | 新浪 | "现在茅台多少钱" |
| 2 | `get_kline` | K 线数据 | 新浪 / Tushare | "看看近 60 日走势" |
| 3 | `get_financials` | 财务 + 估值 | Tushare | "茅台的 PE / ROE" |
| 4 | `search_news` | 个股新闻 / 舆情 | 财经新闻聚合 | "宁德最近有什么消息" |
| 5 | `screen_stocks` | 条件选股 | 新浪 + Tushare | "市值 500 亿以上的科技股" |
| 6 | `compare_stocks` | 多股对比 | 新浪 | "茅台 vs 五粮液" |
| 7 | `create_alert` | 创建预警 | 本地 SQLDelight | "茅台跌破 1300 提醒我" |

### 8.2 工具 JSON Schema（示范 2 个，完整 7 个见附录）

```json
{
  "tools": [
    {
      "name": "get_realtime_quote",
      "description": "获取股票 / 指数的实时报价与五档盘口；返回最新价 / 涨跌幅 / 最高最低 / 成交量 / 买一卖一",
      "parameters": {
        "type": "object",
        "properties": {
          "ticker": { "type": "string", "description": "股票代码，如 sh600519 / sz000001" }
        },
        "required": ["ticker"]
      }
    },
    {
      "name": "screen_stocks",
      "description": "按条件筛选股票列表（行业 / 市值 / 涨跌幅 / 估值），返回排序后的标的列表",
      "parameters": {
        "type": "object",
        "properties": {
          "industry":    { "type": "string", "description": "申万行业名，如 '白酒' / '半导体'" },
          "marketCapMin":{ "type": "number", "description": "最小市值（亿元）" },
          "marketCapMax":{ "type": "number", "description": "最大市值（亿元）" },
          "changePctMin":{ "type": "number", "description": "涨跌幅下限（%）" },
          "changePctMax":{ "type": "number", "description": "涨跌幅上限（%）" },
          "peMax":       { "type": "number", "description": "PE 上限" },
          "limit":       { "type": "integer","description": "返回数量上限，默认 20" }
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```

### 8.3 后端 UseCase 编排

```python
# gateway.py 简化的工具调用循环
async def chat_with_tools(req):
    messages = req["messages"]
    for round in range(MAX_TOOL_ROUNDS):  # 防无限循环
        resp = await openai_chat(messages, tools=TOOL_SCHEMAS)
        if resp.finish_reason != "tool_calls":
            return stream(resp)
        for tc in resp.tool_calls:
            result = await TOOL_DISPATCH[tc.name](**tc.args)
            messages.append(tool_result(tc.id, result))
        # 下一轮把工具结果回灌，模型继续推理
    return final_answer(messages)
```

**关键约束**：
- `MAX_TOOL_ROUNDS = 5`（防 LLM 幻觉死循环）
- 工具调用串行（避免成本爆炸）
- 工具失败 → 错误回灌模型，让模型决定改问还是兜底回答

### 8.4 前端可视化（ChatDetailPage）

- 流式时工具调用显示为灰色提示卡「🔧 正在调用 get_realtime_quote...」
- 完成后变绿「✓ 行情已抓取（茅台 ¥1292.83 +0.11%）」
- 用户可点击展开看原始返回
- Agent 进度条同步点亮（基本面 ✓ → 技术 ✓ → 舆情 ✓ → 风控 ✓）

---

## 9. 后端 API 完整列表（v2 扩展）

> 现有 5 个路由（chat/completions、/v1/models、/healthz、quote/realtime、quote/kline）远远不够；按页面矩阵和工具集反推，需要扩展到 18 个。

### 9.1 网关端（OpenAI 兼容，4 个）

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/v1/chat/completions` | 聊天（流式 + 工具调用） |
| GET | `/v1/models` | 模型目录（聚合 + capability + available） |
| POST | `/v1/embeddings` | （预留）文本向量化 |
| GET | `/v1/usage` | 用量统计（按模型 / 按天） |

### 9.2 行情端（5 个）

| 方法 | 路径 | 用途 | 数据源 |
|---|---|---|---|
| GET | `/quote/realtime` | 单股实时报价 | 新浪 + 五档 |
| GET | `/quote/kline` | K 线（日/周/月/分钟） | 新浪 / Tushare |
| GET | `/quote/indices` | 主要指数列表 | 新浪 |
| GET | `/quote/sectors` | 申万行业板块 | 新浪 / Tushare |
| GET | `/quote/screener` | 条件选股 | 新浪 + Tushare |

### 9.3 资讯端（2 个）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/news/list` | 7×24 快讯 / 个股新闻 |
| GET | `/news/detail` | 资讯正文 |

### 9.4 管理端（受 GATEWAY_API_KEY 保护，3 个）

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/admin/refresh-models` | 触发模型自动发现 |
| GET | `/admin/providers` | 列出所有服务商（含 Key 是否配置） |
| POST | `/admin/probe/{provider_id}` | 单家探活 |

### 9.5 分析端（1 个）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/analytics/usage` | 用量 / 延迟 / 命中率 / 成本 |

> 总计 **15+ 路由**（v2 计划），覆盖页面矩阵所有数据需求 + 工具调用 + 运营分析。

---

## 10. 数据分析维度（运营面板）

> 给评委与运营方一个「真实在跑」的体感——不是 Demo 一次性产物，而是有数据飞轮的平台。

### 10.1 用量统计（每小时聚合）

| 维度 | 字段 | 用途 |
|---|---|---|
| **按模型** | model_id / provider / request_count / prompt_tokens / completion_tokens / cost_usd | "deepseek-chat 这个月用了多少" |
| **按用户** | user_id / device_id / first_seen / last_seen / total_request | 简化（Demo 阶段按设备维度） |
| **按时段** | hour_bucket / request_count / avg_latency | 流量监控 |

### 10.2 性能监控

- **P50 / P95 延迟**（按模型分桶）
- **首 token 延迟**（流式场景）
- **总时长**（含工具调用）

### 10.3 模型可用性

- **命中率** = 成功请求 / 总请求
- **降级率** = 触发降级的请求 / 总请求
- **空 Key 率** = 无 Key 走 Mock 的请求 / 总请求
- **ProviderError 分布**（5 类 reason 计数）

### 10.4 成本估算

- 按 token 数 × 单价（providers.json 可声明 input/output $/M tokens）
- 国产模型按 ¥ 换算
- 预算告警（Demo 阶段不实现）

### 10.5 可视化（Web 端，ModelSettingsPage 「用量」Tab）

- 三张卡：今日调用 / 本月成本 / 平均延迟
- 折线图：近 7 天调用量
- 饼图：模型调用分布
- 排行榜：Top 5 模型 / Top 5 工具

> Demo 阶段做轻量版（in-memory + 定期 dump），不接 BI。

---

## 11. 参考来源（2026-08-24 实测）

1. CherryHQ/cherry-studio — https://github.com/CherryHQ/cherry-studio （PR #19006 端点解析链、#18745 静默过滤、#17661 FTS 泄漏、packages/provider-registry 架构、sync-registry-data.yml CI 分发）
2. 知牛现有网关实现 — `backend/app/config.py` / `gateway.py` / `quote.py`（提交 df04a8b、8b71e1d）
3. 评分标准 — `docs/zhiniu-technical-design.md` §12 评分落地矩阵
