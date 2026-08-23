/* 知牛 · 共享 UI 枚举
 * 说明：页面状态现由 Kuikly 原生 observable/observableList 驱动（见各 VM），
 * 不再需要 sealed UiState 四态容器；此处仅保留跨 VM 共用枚举。
 */
package com.zhiniu.viewmodel

enum class MarketMode { ALL, WATCHLIST, GAINERS, LOSERS }