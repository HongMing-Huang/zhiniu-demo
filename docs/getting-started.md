# 知牛 ZhiNiu · 快速开始

> 版本：v0.1（2026-08-22）。环境要求来自 Kuikly 官方环境搭建页（kuikly.tds.qq.com/QuickStart/env-setup.html）。

---

## 0. 环境要求

| 工具 | 版本 | 用途 |
|:--|:--|:--|
| JDK | **17** | Gradle 构建（Android Studio ≥2024.2.1 需手动把 Gradle JDK 切到 17） |
| Android Studio | ≥ 2024.2.1 | 前端壳工程 + Kuikly 插件 |
| Kuikly Android Studio 插件 | ≥ 1.1.0（含鸿蒙） | 生成 Kuikly 工程/ComposeView/Pager |
| Python | 3.10+ | 后端 LLM 网关 |
| （可选）Xcode / DevEco | — | iOS / 鸿蒙端 |

---

## 1. 后端 LLM 网关（先跑这个）

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # 填 DeepSeek/智谱/混元 Key（可选，缺失走 Mock）
uvicorn app.main:app --port 8000
```

自检：`curl http://localhost:8000/healthz` → 各厂商 `key_configured`。

---

## 2. 前端（Kuikly）

推荐用**官方脚手架**生成 gradle（手写坐标易错）：

- **方式 A（推荐）**：Android Studio `File → New → New Project → Kuikly Project Template`
- 方式 B（CLI）：`npx create-kuikly-app create zhiniu --package com.zhiniu --dsl kuikly`

将仓库 `shared/src/commonMain/kotlin/com/zhiniu/`（pages/viewmodel/domain/data/di）内的源码并入生成的 `shared` 模块，`HomePage` 为 Hello 页，随后在 Android Studio 运行 `androidApp`。

环境访问：`GET /v1/chat/completions` 走 `http://localhost:8000`；行情走新浪 `hq.sinajs.cn`。

---

## 3. 运行顺序与三种数据模式

| 场景 | 设置 | 效果 |
|:--|:--|:--|
| 完整真数据 | .env 填 3 厂 Key + 有网 | 新浪行情 + 多模型 AI |
| 仅行情真 | 只填新浪（无 Key） | 行情真、AI 走 Mock |
| 完全离线 | 无网 | 全部走 `data/mock/` 兜底 |

---

## 4. 常见问题

- **Gradle 依赖失败**：把工程 Gradle 版本切到 **7.5.1**（Kuikly 兼容），低版本需在 `settings.gradle.kts` 加 `enableFeaturePreview("VERSION_CATALOGS")`。
- **iOS 不装环境**：注释 `shared/build.gradle.kts` 中 iOS 相关 target。
- **新浪乱码**：接口 GBK，需按 GBK 解码。

详见 `docs/api-matrix.md`（字段/端点）与 `docs/tech-stack.md`（选型）。