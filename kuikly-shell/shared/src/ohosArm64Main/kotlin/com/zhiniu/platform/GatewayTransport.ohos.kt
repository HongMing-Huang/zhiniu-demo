// 知牛 · HarmonyOS actual：GatewayTransport 走 Kuikly NetworkModule。
//
// 链路：Kotlin NetworkModule.httpRequest → native KRNetworkModule → ArkTS @ohos.net.http
// （@kuikly-open/render 内置实现，无需自研 napi 桥）。整包返回（无增量回调），
// 因此 postSse 返回 false，AI 流式问答自动降级为一次性 postJson（与 iOS 同策略）。
//
// 线程：httpRequest 的发起要求在 Kuikly Context 线程；GatewayMarketClient 的调用方
// 在 runOffMainThread 的 Worker 上，故经 setTimeout(pagerId) 编组回 Context 线程发起，
// 回调到达后续在原协程恢复。
package com.zhiniu.platform

import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.timer.setTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class OhosGatewayTransport : GatewayTransport {

    override suspend fun get(url: String): String = request(url, isPost = false, body = null)

    override suspend fun postJson(url: String, json: String): String = request(url, isPost = true, body = json)

    override suspend fun postSse(url: String, json: String, onLine: (String) -> Unit): Boolean = false

    private suspend fun request(url: String, isPost: Boolean, body: String?): String =
        suspendCoroutine { cont ->
            var settled = false
            fun fail(t: Throwable) {
                if (!settled) { settled = true; cont.resumeWithException(t) }
            }
            fun start() {
                val module = runCatching {
                    PagerManager.getCurrentPager().acquireModule<NetworkModule>(NetworkModule.MODULE_NAME)
                }.getOrNull()
                if (module == null) {
                    fail(IllegalStateException("NetworkModule 不可用（无页面上下文）"))
                    return
                }
                val param = when {
                    body != null -> runCatching { JSONObject(body) }.getOrElse {
                        JSONObject().apply { put("data", body) }
                    }
                    else -> JSONObject()
                }
                val headers = if (isPost) {
                    JSONObject().apply { put("Content-Type", "application/json") }
                } else null
                try {
                    // 60s：agent/research 单请求在中转站延迟抖动下可达数十秒
                    module.httpRequest(url, isPost, param, headers, null, 60) { data, success, errorMsg, _ ->
                        if (settled) return@httpRequest
                        settled = true
                        if (success) cont.resume(data.toString())
                        else cont.resumeWithException(RuntimeException("网关请求失败: $errorMsg"))
                    }
                } catch (t: Throwable) {
                    fail(t)
                }
            }
            val pagerId = BridgeManager.currentPageId
            if (pagerId.isEmpty()) start() else setTimeout(pagerId, 0) { start() }
        }
}

actual fun createPlatformGatewayTransport(): GatewayTransport = OhosGatewayTransport()

// DevEco 模拟器与 Android 同为 QEMU NAT（模拟器 eth0 = 10.0.2.15），宿主机环回即 10.0.2.2。
// 真机联调：与电脑同网段时在页面 URL 用 ?gateway=http://<电脑IP>:8000 覆盖。
actual fun platformDefaultGateway(): String? = "http://10.0.2.2:8000"
