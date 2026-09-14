// 知牛 · iOS actual：HTTP 走原生壳桥（KRBridgeModule.httpRequest → NSURLSession）。
// 背景：Ktor Darwin 引擎在本工程内响应回调永不触发（请求到达服务端并返回 200，
// 客户端 continuation 不恢复，HttpTimeout 亦不生效；ktor#678/KTOR-5502 一类死锁）。
// 桥方案（对齐 ohosArm64 NetworkModule 模式）：Kotlin → callNative → ObjC
// NSURLSession → completion → Kuikly callback 编组回 Context 线程 resume。
package com.zhiniu.platform

import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.timer.setTimeout
import com.zhiniu.base.BridgeModule
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal class BridgeGatewayTransport : GatewayTransport {

    // GET（行情/搜索/榜单等短请求）：同步桥直返（Context 阻塞百毫秒级，可接受）
    override suspend fun get(url: String): String {
        val module = PagerManager.getCurrentPager().acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
        val obj = module.httpRequestSync(url, "GET", null)
        val error = obj["error"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (error.isNotEmpty()) throw RuntimeException("bridge http: $error")
        return obj["result"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    // POST（LLM 长请求）：异步桥（避免长时间阻塞 UI；失败由页面侧 mock 降级承接）
    override suspend fun postJson(url: String, json: String): String = request(url, "POST", json)

    // iOS 无 Ktor 流式：页面侧已有「流不可用 → 回退一次性接口」降级
    override suspend fun postSse(url: String, json: String, onLine: (String) -> Unit): Boolean = false

    private suspend fun request(url: String, method: String, body: String?): String =
        suspendCoroutine { cont ->
            var settled = false
            fun fail(t: Throwable) {
                if (!settled) { settled = true; cont.resumeWithException(t) }
            }
            fun start() {
                val module = runCatching {
                    PagerManager.getCurrentPager().acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
                }.getOrNull()
                if (module == null) {
                    fail(IllegalStateException("BridgeModule 不可用（无页面上下文）"))
                    return
                }
                try {
                    module.httpRequestAwaitVia(url, method, body) { result ->
                        if (settled) return@httpRequestAwaitVia
                        settled = true
                        result.fold({ cont.resume(it) }, { fail(it) })
                    }
                } catch (t: Throwable) {
                    fail(t)
                }
            }
            // 确保在 Context 线程发起（callNative 约束）；已在 Context 时立即执行
            val pagerId = BridgeManager.currentPageId
            if (pagerId.isEmpty()) start() else setTimeout(pagerId, 0) { start() }
        }
}

actual fun createPlatformGatewayTransport(): GatewayTransport = BridgeGatewayTransport()

// iOS 模拟器 127.0.0.1 直通宿主机 loopback，沿用 commonMain 默认
actual fun platformDefaultGateway(): String? = null
