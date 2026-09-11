// 知牛 · Android actual：OkHttp 引擎（androidApp 已允许明文流量访问本机/局域网网关）
package com.zhiniu.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
}
