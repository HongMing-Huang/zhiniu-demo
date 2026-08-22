/* 知牛 · LLM 网关客户端
 * 调用自建 backend /v1/chat/completions（OpenAI 兼容，SSE 流式）。
 * 统一模型别名：zhiniu/quick·think·flash。真实后端设计见 docs/backend-llm-gateway-design.md。
 */
package com.zhiniu.data.remote

import com.zhiniu.domain.model.MessageRole
import com.zhiniu.domain.model.StreamChunk
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class ChatIn(
    val model: String,
    val messages: List<MsgIn>,
    val stream: Boolean = true,
) {
    @Serializable
    data class MsgIn(
        val role: String,
        val content: String = "",
    )
}

class LlmGatewayClient(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:8000",
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 流式对话：逐块返回文本增量；以 [StreamChunk.Done] 收尾。 */
    fun chatStream(model: String, history: List<com.zhiniu.domain.model.ChatMessage>): Flow<StreamChunk> =
        flow {
            val body = ChatIn(
                model = model,
                messages = history.map {
                    when (it.role) {
                        MessageRole.USER -> ChatIn.MsgIn("user", it.content)
                        MessageRole.ASSISTANT -> ChatIn.MsgIn("assistant", it.content)
                        else -> ChatIn.MsgIn("user", it.content)
                    }
                },
            )
            val resp = client.post("$baseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Accept", "text/event-stream")
                setBody(json.encodeToString(body))
            }.bodyAsText()

            val acc = StringBuilder()
            resp.lineSequence().forEach { line ->
                val s = line.trim()
                if (!s.startsWith("data:")) return@forEach
                val payload = s.removePrefix("data:").trim()
                if (payload == "[DONE]") return@forEach
                val obj = runCatching {
                    json.parseToJsonElement(payload).let { el ->
                        if (el !is kotlinx.serialization.json.JsonObject) null
                        else el
                    }
                }.getOrNull() ?: return@forEach
                when {
                    obj["error"] != null -> {
                        val msg = obj["error"]?.let { it as? kotlinx.serialization.json.JsonObject }
                            ?.get("message")?.toString()?.trim('"') ?: "上游异常"
                        emit(StreamChunk.Error(msg))
                        return@flow
                    }
                    else -> {
                        val delta = obj["choices"]?.toString()?.let { parseDelta(it) } ?: ""
                        if (delta.isNotEmpty()) {
                            acc.append(delta)
                            emit(StreamChunk.Delta(delta))
                        }
                    }
                }
            }
            emit(StreamChunk.Insight(acc.toString()))
            emit(StreamChunk.Done)
        }.flowOn(Dispatchers.IO)

    private fun parseDelta(choicesStr: String): String {
        // 从 choices JSON 中抽取首项的 delta.content
        return runCatching {
            val choices = json.parseToJsonElement(choicesStr) as kotlinx.serialization.json.JsonArray
            if (choices.isEmpty()) return@runCatching ""
            val first = choices[0] as kotlinx.serialization.json.JsonObject
            val delta = first["delta"] as? kotlinx.serialization.json.JsonObject ?: return@runCatching ""
            val c = delta["content"]
            return@runCatching c?.toString()?.trim('"') ?: ""
        }.getOrDefault("")
    }
}