// 知牛 · Android actual：daemon 线程执行 + Kuikly native timer 轮询编组回 Context 线程。
package com.zhiniu.platform

import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.timer.setTimeout
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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

internal actual suspend fun <T> runOffMainThread(block: suspend () -> T): T {
    val box = AtomicReference<Result<T>?>(null)
    val name = "zn-off-" + (pollSeq++)
    thread(name = name, isDaemon = true) {
        box.set(runCatching { runBlocking { block() } })
    }
    while (box.get() == null) hopWait(30)
    return box.get()!!.getOrThrow()
}

internal actual val sseStreamingSupported: Boolean = true
