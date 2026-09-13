package com.zhiniu.platform

/**
 * 网关传输层抽象（鸿蒙桥接基座）。
 *
 * commonMain 只依赖本接口；各平台 actual 提供实现：
 * - js / androidMain / iosMain：Ktor（Js / OkHttp / Darwin 引擎）
 * - ohosMain：预留 napi 桥（经 KRBridgeModule 调 ETS @ohos.net.http；DevEco 环境就绪前
 *   返回失败，页面按既有降级设计走离线快照，不白屏）
 */
interface GatewayTransport {
    /** GET 文本响应。 */
    suspend fun get(url: String): String

    /** POST JSON，返回文本响应。 */
    suspend fun postJson(url: String, json: String): String

    /**
     * POST SSE 流式响应：每行回调 onLine（含 "event:"/"data:" 前缀行）。
     * 返回 false 表示流式不可用（旧网关/代理限制），调用方回退 postJson。
     */
    suspend fun postSse(url: String, json: String, onLine: (String) -> Unit): Boolean
}

/** 平台传输实现工厂（ohos 为 napi 桥预留位）。 */
expect fun createPlatformGatewayTransport(): GatewayTransport
