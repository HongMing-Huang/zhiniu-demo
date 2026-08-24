package com.zhiniu.base

import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

/** 页面跳转工具：跨端统一用官方 RouterModule.openPage(pageName, pageData) 跳转（H5 走 KRRouterModule/SPA）。 */
internal fun Pager.openZhiniuPage(pageName: String, params: Map<String, String> = emptyMap()) {
    val pageData = JSONObject()
    params.forEach { (k, v) -> pageData.put(k, v) }
    acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)
}