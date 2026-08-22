# PR-01 · 前端脚手架（Kuikly）

> 状态：**结构骨架 + 最小 Hello 页**；gradle 接线与编译需在 IDE 按官方模板生成后验证。

## 已建立

```
shared/src/commonMain/kotlin/com/zhiniu/
├── pages/            # 页面（HomePage 骨架 + components 组件库占位）
├── viewmodel/        # StateFlow 四态（占位）
├── domain/
│   ├── model/        # Quote/KLine/AiInsight/ChatMessage（占位）
│   ├── usecase/      # GetStockList/GetStockDetail/AskChat（占位）
│   └── repository/   # WatchlistRepo（占位）
├── data/
│   ├── remote/       # SinaQuoteApi/网关客户端（占位）
│   ├── local/        # SQLDelight（占位）
│   └── mock/         # 离线 JSON（占位）
└── di/               # Koin（占位）
```

- `HomePage.kt`：严格对齐官方最小页，仅用已确认 DSL。

## 官方脚手架路径（真实来源，2026-08-22 调研）

1. **Android Studio 插件（官方推荐）**：`File → New → New Project → Kuikly Project Template`，生成 `shared + androidApp + iosApp (+ ohosApp/h5App)` 宿主工程与 gradle。参考 https://kuikly.tds.qq.com/DevGuide/as-plugin.html
2. **CLI（社区，AI 友好）**：`npx create-kuikly-app create <name> --package com.zhiniu --dsl kuikly`，零 IDE 交互，支持 `--json`。

## 待验证（需 IDE / 真机）

- [ ] Pager / ViewBuilder / Text 的**真实包路径**（以 SDK 模板为准，当前为占位）
- [ ] gradle plugin / Kotlin 2.x / KSP / Koin 坐标（用官方模板生成，不手写）
- [ ] D8：Kuikly DSL 是否直接支持 `ImageVector` 图标（决定图标走字体还是图标库）
- [ ] 各端壳工程（androidApp/iosApp/ohosApp）能否编译运行 Hello 页

> 原则：不臆造 API；凡未证实即标注占位，待官方模板/IDE 校对后再落地。