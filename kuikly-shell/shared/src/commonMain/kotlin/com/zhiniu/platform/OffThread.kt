/* 知牛 · 后台线程执行器（跨平台线程契约）。
 *
 * 背景：Kuikly 协程框架（Builders.kt）无 ContinuationInterceptor——lifecycleScope.launch
 * 的挂起点（如 Ktor 网络请求）恢复后跑在引擎回调线程上；此时更新 observable 会触发
 * setShadowProp → callNative，在 iOS 渲染层触发严格线程断言（SIGABRT）。
 *
 * 约定：页面协程内的网络调用一律经 runOffMainThread 包装——
 * - 移动/桌面原生端：block 在 daemon 线程执行，协程经 Kuikly native timer（delay 轮询）
 *   挂起等待；timer 回调由渲染核心编组回 Context 线程，恢复后的状态更新回到合法线程。
 * - JS：单线程事件循环天然安全，直接执行。
 *
 * SSE 流式同理仅在有多线程断言问题的 iOS 渲染层关闭（sseStreamingSupported=false），
 * 页面侧已有「流不可用 → 回退一次性接口」的既有降级。
 */
package com.zhiniu.platform

/** 在平台后台线程执行 block（不触碰 Kuikly 渲染管线），挂起至完成并返回结果。 */
internal expect suspend fun <T> runOffMainThread(block: suspend () -> T): T

/** 当前渲染层是否支持 SSE 流式回调直接驱动页面状态（iOS Debug 断言下不支持）。 */
internal expect val sseStreamingSupported: Boolean
