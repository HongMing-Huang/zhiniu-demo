// 知牛 · iOS actual：Darwin(NSURLSession) 引擎（Info.plist 已开放 NSAllowsArbitraryLoads 访问 http 网关）
package com.zhiniu.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin) {
    expectSuccess = false
}
