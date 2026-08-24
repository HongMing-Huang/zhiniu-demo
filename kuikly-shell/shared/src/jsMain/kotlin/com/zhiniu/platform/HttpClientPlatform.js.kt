package com.zhiniu.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

/** H5(JS) actual：浏览器 Js engine（B3 调后端 LLM 网关 SSE）。 */
actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js) {
    expectSuccess = false
}