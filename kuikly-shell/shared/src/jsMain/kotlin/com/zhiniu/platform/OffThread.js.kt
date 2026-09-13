// 知牛 · JS actual：单线程事件循环，无跨线程断言问题，直接执行。
package com.zhiniu.platform

internal actual suspend fun <T> runOffMainThread(block: suspend () -> T): T = block()

internal actual val sseStreamingSupported: Boolean = true
