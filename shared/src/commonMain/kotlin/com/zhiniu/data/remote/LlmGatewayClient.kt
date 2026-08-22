/* 知牛 · LLM 网关客户端
 * 调用自建 backend /v1/chat/completions（OpenAI 兼容，SSE 流式）。
 * 统一模型别名：zhiniu/quick·think·flash。真实后端设计见 docs/backend-llm-gateway-design.md。
 */
package com.zhiniu.data.remote

import com.zhiniu.domain.model.AppError
import com.zhiniu.domain.model.MessageRole
import com.zhiniu.domain.model.SseParser
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
            var pendingEvent: String? = null
            resp.lineSequence().forEach { rawLine ->
                val frame = SseParser.parseLine(rawLine) ?: run { pendingEvent = null; return@forEach }
                // event 行只更新帧类型（与随后的 data 行同帧）
                frame.event?.let { pendingEvent = it; return@forEach }
                val payload = frame.data ?: return@forEach
                when (SseParser.kind(pendingEvent, payload)) {
                    SseParser.EventKind.DONE -> {
                        emit(StreamChunk.Insight(acc.toString()))
                        emit(StreamChunk.Done)
                        pendingEvent = null
                        return@forEach
                    }
                    SseParser.EventKind.STEP_PROGRESS -> {
                        // agent_progress：{ "step": N, "label": "..." } → 点亮 Agent 时间线
                        val step = runCatching {
                            (json.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject)["step"]
                                ?.toString()?.trim('"')?.toIntOrNull()
                        }.getOrNull()
                        val label = runCatching {
                            (json.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject)["label"]
                                ?.toString()?.trim('"')
                        }.getOrNull()
                        emit(StreamChunk.Progress(step ?: 0, label ?: "分析中"))
                        pendingEvent = null
                    }
                    SseParser.EventKind.ERROR -> {
                        val err = runCatching {
                            val obj = json.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject
                            obj["error"]?.let { it as kotlinx.serialization.json.JsonObject }
                        }.getOrNull()
                        val code = err?.get("code")?.toString()?.trim('"')
                        val backendMsg = err?.get("message")?.toString()?.trim('"')
                        val appError = AppError.fromBackendCode(code)
                        val shown =
                            if (code == "unknown" && !backendMsg.isNullOrBlank()) "[${appError.code}] $backendMsg"
                            else appError.display()
                        emit(StreamChunk.Error(shown))
                        return@flow
                    }
                    SseParser.EventKind.DATA -> {
                        val obj = runCatching {
                            json.parseToJsonElement(payload).let { el ->
                                if (el is kotlinx.serialization.json.JsonObject) el else null
                            }
                        }.getOrNull() ?: return@forEach
                        val delta = obj["choices"]?.toString()?.let { parseDelta(it) } ?: ""
                        if (delta.isNotEmpty()) {
                            acc.append(delta)
                            emit(StreamChunk.Delta(delta))
                        }
                    }
                }
                pendingEvent = null // 一个完整帧结束：重置 event
            }
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