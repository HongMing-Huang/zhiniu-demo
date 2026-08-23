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
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
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

    /** 流式对话：真流式订阅，逐 chunk 到达即 emit（T2-6）；以 [StreamChunk.Done] 收尾。 */
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
            }

            // ① HTTP 状态映射为统一错误（非 2xx 直接进入错误收尾）
            if (resp.status.value !in 200..299) {
                val appErr = AppError.fromHttpStatus(resp.status.value)
                val reason = runCatching { resp.bodyAsText().take(200) }.getOrNull()
                emit(StreamChunk.Error(if (appErr.code == "UNKNOWN") "[UNKNOWN] ${reason ?: "HTTP ${resp.status.value}"}" else appErr.display()))
                emit(StreamChunk.Done)
                return@flow
            }

            // ② 真流式：逐行读取 SSE（网络字节到达即解析，不整包读入）
            val channel = resp.bodyAsChannel()
            var pendingEvent: String? = null
            val acc = StringBuilder()
            while (true) {
                val rawLine = channel.readUTF8Line() ?: break
                val frame = SseParser.parseLine(rawLine) ?: run { pendingEvent = null; continue }
                frame.event?.let { pendingEvent = it; continue }
                val payload = frame.data ?: continue
                when (SseParser.kind(pendingEvent, payload)) {
                    SseParser.EventKind.DONE -> {
                        emit(StreamChunk.Insight(acc.toString()))
                        emit(StreamChunk.Done)
                        pendingEvent = null
                        return@flow
                    }
                    SseParser.EventKind.STEP_PROGRESS -> {
                        val js = runCatching { json.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject }.getOrNull()
                        val step = js?.get("step")?.toString()?.trim('"')?.toIntOrNull()
                        val label = js?.get("label")?.toString()?.trim('"')
                        emit(StreamChunk.Progress(step ?: 0, label ?: "分析中"))
                        pendingEvent = null
                    }
                    SseParser.EventKind.ERROR -> {
                        emit(StreamChunk.Error(errorDisplay(payload)))
                        return@flow
                    }
                    SseParser.EventKind.DATA -> {
                        val obj = runCatching { json.parseToJsonElement(payload) as? kotlinx.serialization.json.JsonObject }.getOrNull() ?: continue
                        // reasoning_content 与 content 并行接收：content 正常累积，reasoning 不污染正文
                        val delta = obj["choices"]?.toString()?.let { parseDelta(it) } ?: ""
                        if (delta.isNotEmpty()) {
                            acc.append(delta)
                            emit(StreamChunk.Delta(delta))
                        }
                    }
                }
                pendingEvent = null
            }
        }.flowOn(Dispatchers.IO)

    /** 错误帧 data → 统一错误文案（映射后端 error.code）。 */
    private fun errorDisplay(payload: String): String {
        val err = runCatching {
            (json.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject)["error"]
                ?.let { it as? kotlinx.serialization.json.JsonObject }
        }.getOrNull()
        val code = err?.get("code")?.toString()?.trim('"')
        val backendMsg = err?.get("message")?.toString()?.trim('"')
        val appError = AppError.fromBackendCode(code)
        return if (code == "unknown" && !backendMsg.isNullOrBlank()) "[${appError.code}] $backendMsg"
        else appError.display()
    }

    /**
     * 从 choices JSON 抽取首项 delta：仅取 content（reasoning_content 不进入正文，
     * 保证"reasoning 与 content 并行、先后不假设"的正确渲染语义）。
     */
    private fun parseDelta(choicesStr: String): String {
        return runCatching {
            val choices = json.parseToJsonElement(choicesStr) as kotlinx.serialization.json.JsonArray
            if (choices.isEmpty()) return@runCatching ""
            val first = choices[0] as kotlinx.serialization.json.JsonObject
            val delta = first["delta"] as? kotlinx.serialization.json.JsonObject ?: return@runCatching ""
            delta["content"]?.toString()?.trim('"') ?: ""
        }.getOrDefault("")
    }
}