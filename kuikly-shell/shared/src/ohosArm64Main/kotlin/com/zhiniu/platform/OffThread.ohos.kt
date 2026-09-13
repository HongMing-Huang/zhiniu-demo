// 知牛 · HarmonyOS actual：daemon 线程执行 + Kuikly native timer 轮询编组回 Context 线程。
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
// 30ms × 5000 ≈ 150s 上限：网络接入后 agent/research 单请求在中转站抖动下可达数十秒；
// 此轮询仅维持 Context 线程空闲，等待本身在 Worker 上挂起，不占 UI。
private const val MAX_HOPS = 5000

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

internal actual val sseStreamingSupported: Boolean = true
