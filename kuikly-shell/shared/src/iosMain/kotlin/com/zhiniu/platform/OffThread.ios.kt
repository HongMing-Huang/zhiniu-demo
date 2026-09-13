// 知牛 · iOS actual：后台 Worker 执行 + Kuikly native timer 轮询编组回 Context 线程。
// （iOS 渲染层 Debug 断言严格，Ktor 恢复线程的任何 callNative 都会 SIGABRT——
//   网络结果必须经本包装跳回 Context 线程后再更新页面状态。）
package com.zhiniu.platform

import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.timer.setTimeout
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.Worker
import kotlinx.coroutines.runBlocking

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
private const val MAX_HOPS = 400 // 30ms × 400 ≈ 12s 上限，防页面销毁后 timer 取消导致挂死

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

// iOS 渲染层 SSE 行回调在 Ktor 线程触发 callNative → 断言崩溃；页面已实现
// 「流不可用 → 回退一次性接口」降级，故 iOS 关闭流式。
internal actual val sseStreamingSupported: Boolean = false
