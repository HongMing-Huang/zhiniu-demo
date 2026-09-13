// 知牛 · Android actual：OkHttp 引擎实现 GatewayTransport（androidApp 已允许明文流量访问本机/局域网网关）。
package com.zhiniu.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line

internal class KtorGatewayTransport : GatewayTransport {
    override suspend fun get(url: String): String = client.get(url).bodyAsText()

    override suspend fun postJson(url: String, json: String): String =
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(json)
        }.bodyAsText()

    override suspend fun postSse(url: String, json: String, onLine: (String) -> Unit): Boolean = try {
        client.preparePost(url) {
            contentType(ContentType.Application.Json)
            setBody(json)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                onLine(line)
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

private val client by lazy {
    HttpClient(OkHttp) { expectSuccess = false }
}

actual fun createPlatformGatewayTransport(): GatewayTransport = KtorGatewayTransport()

// 模拟器场景默认走宿主机别名（真机无效时回落离线快照，可用 gateway 参数覆盖）
actual fun platformDefaultGateway(): String? = "http://10.0.2.2:8000"
