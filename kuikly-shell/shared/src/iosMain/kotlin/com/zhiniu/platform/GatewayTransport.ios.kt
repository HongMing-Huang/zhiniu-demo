// 知牛 · iOS actual：NSURLSession（completion-handler）传输。
// 背景：Ktor Darwin 引擎在本工程内响应回调永不触发（请求到达服务端并返回 200，
// 客户端 continuation 不恢复，HttpTimeout 亦不生效；ktor#678/KTOR-5502 一类死锁）。
// 改用 NSURLSession dataTask completion API + 信号量在 Worker 线程同步等待：
// completion 由 NSURLSession 自有队列触发，与被阻塞的 Worker 无耦合，杜绝死锁路径。
// （SSE 在 iOS 仍走「流不可用 → 一次性接口」降级。）
package com.zhiniu.platform

import kotlinx.coroutines.runBlocking
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPMethod
import platform.Foundation.allHTTPHeaderFields
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.AtomicReference
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.timer.setTimeout

internal class IosGatewayTransport : GatewayTransport {

    private val session by lazy {
        NSURLSession.sessionWithConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration())
    }

    /** 同步执行请求（仅在 OffThread Worker 线程调用；completion 在 NSURLSession 队列触发）。 */
    private fun execText(request: NSURLRequest): String {
        val sem = dispatch_semaphore_create(0)
        var payload: NSData? = null
        var failure: NSError? = null
        val task = session.dataTaskWithRequestCompletionHandler(request) { data, _, error ->
            payload = data
            failure = error
            dispatch_semaphore_signal(sem)
        }
        task.resume()
        // 15s 上限：网络异常时保证 Worker 不永久阻塞
        val waitResult = dispatch_semaphore_wait(sem, 15_000_000_000UL)
        if (waitResult.toInt() != 0) throw RuntimeException("ios transport timeout")
        val err = failure
        if (err != null) throw RuntimeException("ios transport error: ${err.localizedDescription}")
        val body = payload ?: throw RuntimeException("ios transport empty body")
        return NSString.create(body, NSUTF8StringEncoding) as String
    }

    private fun request(url: String, method: String, jsonBody: String?): NSMutableURLRequest {
        val req = NSMutableURLRequest.requestWithURL(NSURL.URLWithString(url)!!)
        req.HTTPMethod = method
        if (jsonBody != null) {
            req.allHTTPHeaderFields(mapOf("Content-Type" to "application/json"))
            req.HTTPBody = (jsonBody as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        }
        return req
    }

    override suspend fun get(url: String): String = execText(request(url, "GET", null))

    override suspend fun postJson(url: String, json: String): String = execText(request(url, "POST", json))

    override suspend fun postSse(url: String, json: String, onLine: (String) -> Unit): Boolean = false
}

// Context 线程上注册 native timer；回调经渲染核心编组回 Context 线程，协程在此恢复。
private suspend fun hopWait(ms: Int) = suspendCoroutine { cont ->
    val pagerId = BridgeManager.currentPageId
    if (pagerId.isEmpty()) {
        cont.resume(Unit)
    } else {
        setTimeout(pagerId, ms) { cont.resume(Unit) }
    }
}

private var pollSeq = 0
private const val MAX_HOPS = 800 // 30ms × 800 ≈ 24s 上限（与传输层 15s 超时对齐后留余量）

internal actual suspend fun <T> runOffMainThread(block: suspend () -> T): T {
    val box = AtomicReference<Result<T>?>(null)
    val seq = pollSeq++
    val worker = Worker.start(name = "zn-off-$seq")
    worker.executeAfter(0L) {
        box.value = runCatching { runBlocking { block() } }
    }
    var hops = 0
    while (box.value == null) {
        hops += 1
        if (hops > MAX_HOPS) {
            box.value = Result.failure(RuntimeException("off-thread timeout"))
            break
        }
        hopWait(30)
    }
    worker.requestTermination(processScheduledJobs = false)
    return box.value!!.getOrThrow()
}

internal actual val sseStreamingSupported: Boolean = false
