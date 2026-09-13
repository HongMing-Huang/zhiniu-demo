// 知牛 · H5(JS) actual：浏览器 Js engine 实现 GatewayTransport（B3 调后端 LLM 网关 SSE）。
package com.zhiniu.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
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
    HttpClient(Js) { expectSuccess = false }
}

actual fun createPlatformGatewayTransport(): GatewayTransport = KtorGatewayTransport()

// 浏览器同源/局域网直连，沿用 commonMain 默认 127.0.0.1
actual fun platformDefaultGateway(): String? = null
