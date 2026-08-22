/* 知牛 · 聊天消息与流式块 */
package com.zhiniu.domain.model

import kotlinx.serialization.Serializable

/** 聊天消息角色。 */
@Serializable
enum class MessageRole { USER, ASSISTANT, SYSTEM, TOOL }

/** 单条会话消息。 */
@Serializable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String = "",
    val cards: List<AiCard> = emptyList(), // 结构化卡片（混排在 Markdown 中显示）
    val agentDone: Int = 0,                // Agent 时间线已完成步数（0..4）
    val isStreaming: Boolean = false,
)

/** SSE 流式增量块。 */
sealed interface StreamChunk {
    /** 文本增量。 */
    data class Delta(val text: String) : StreamChunk
    /** 结构化洞见（JSON 解析完成/最终）。 */
    data class Insight(val data: String) : StreamChunk
    /** 工具执行进度事件（Agent 时间线点亮）。 */
    data class Progress(val step: Int, val label: String) : StreamChunk
    object Done : StreamChunk
    data class Error(val msg: String) : StreamChunk
}