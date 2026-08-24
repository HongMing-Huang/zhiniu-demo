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

---

## 6. 参考来源（2026-08-24 实测）

1. CherryHQ/cherry-studio — https://github.com/CherryHQ/cherry-studio （PR #19006 端点解析链、#18745 静默过滤、#17661 FTS 泄漏、packages/provider-registry 架构、sync-registry-data.yml CI 分发）
2. 知牛现有网关实现 — `backend/app/config.py` / `gateway.py` / `quote.py`（提交 df04a8b、8b71e1d）
3. 评分标准 — `docs/zhiniu-technical-design.md` §12 评分落地矩阵
