package com.zhiniu.base

import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

/** 页面跳转工具：统一用官方 RouterModule（H5 SPA 下被劫持为 push/back，同一浏览器 Tab）。 */
internal fun Pager.openZhiniuPage(pageName: String, params: Map<String, String> = emptyMap()) {
    val pageData = JSONObject()
    params.forEach { (k, v) -> pageData.put(k, v) }
    acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)
}

internal fun Pager.openMarketPage() = openZhiniuPage("MarketList")

internal fun Pager.openStockDetail(symbol: String) =
    openZhiniuPage("StockDetail", mapOf("symbol" to symbol))

internal fun Pager.openAiResearchPage() = openZhiniuPage("AiResearch")

/** 携带初始问题跳转 AI 研究（图表选点追问 / 详情页深聊入口）。 */
internal fun Pager.openAiResearchPage(question: String) =
    openZhiniuPage("AiResearch", mapOf("question" to question))

internal fun Pager.openWatchlistPage() = openZhiniuPage("Watchlist")

/** 「我的」页（用户卡 / 外观偏好 / 服务状态 / 关于）。 */
internal fun Pager.openProfilePage() = openZhiniuPage("Profile")

/** 拉起双股对比页（AI 工具指令 open_compare / 快捷入口）。 */
internal fun Pager.openComparePage(symbolA: String, symbolB: String) =
    openZhiniuPage("Compare", mapOf("symbolA" to symbolA, "symbolB" to symbolB))

/** 关闭当前页（H5 SPA 下等价浏览器 Back，返回上一页并恢复滚动位置）。 */
internal fun Pager.closeCurrentPage() {
    acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
}

/** 跨页一次性传参：图表选点/详情 CTA → AI 研究自动提问（SPA 同 bundle 内可靠；URL 直达不支持）。 */
object PendingAsk {
    var question: String? = null
}
