// 知牛 · iOS actual：网络已改走原生壳桥（见 GatewayTransport.ios.kt），
// 回调由 Kuikly 编组回 Context 线程，故本 actual 直接执行 block（保持 Context 串行）。
package com.zhiniu.platform

internal actual suspend fun <T> runOffMainThread(block: suspend () -> T): T = block()

// iOS 无 Ktor 流式：页面侧已有「流不可用 → 回退一次性接口」降级
internal actual val sseStreamingSupported: Boolean = false
