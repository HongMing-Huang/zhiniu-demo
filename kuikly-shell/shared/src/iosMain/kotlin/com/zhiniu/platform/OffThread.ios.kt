// 知牛 · iOS actual：直发网络请求 + GCD hop 回主线程（Context）。
// 背景：Ktor Darwin 引擎在「Kotlin Worker + runBlocking」内会永久挂起
// （ktor#678 / KTOR-5502 一类死锁：回调恢复路径被阻塞；Android OkHttp 同模式正常）。
// 方案：在调用协程直接挂起发起请求（NSURLSession 标准异步）——请求由 Darwin 回调队列
// resume；随后必须把结果送回 Context 线程再返回（页面 observable 更新有 iOS 渲染层
// Debug 断言，且 Kuikly setTimeout 不允许非 Context 线程注册），故用 GCD dispatch
// 到主队列完成一次 hop（GCD 可从任意线程调用）。
package com.zhiniu.platform

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// 主队列（= Kuikly iOS Context 线程）上恢复协程；dispatch_async 可从任意线程调用。
private suspend fun hopToMainQueue() = suspendCoroutine { cont ->
    dispatch_async(dispatch_get_main_queue()) {
        cont.resume(Unit)
    }
}

internal actual suspend fun <T> runOffMainThread(block: suspend () -> T): T {
    // 网络挂起点由 Darwin 回调队列 resume（无 runBlocking 阻塞，杜绝死锁路径）；
    // 结果经一次 GCD hop 送回主线程，调用方的状态更新全部落在 Context 线程。
    val result = runCatching { block() }
    hopToMainQueue()
    return result.getOrThrow()
}

// iOS 渲染层 SSE 行回调在 Ktor 线程触发 callNative → 断言崩溃；页面已实现
// 「流不可用 → 回退一次性接口」降级，故 iOS 关闭流式。
internal actual val sseStreamingSupported: Boolean = false
