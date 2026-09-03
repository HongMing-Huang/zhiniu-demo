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

/** 关闭当前页（H5 SPA 下等价浏览器 Back，返回上一页并恢复滚动位置）。 */
internal fun Pager.closeCurrentPage() {
    acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
}
