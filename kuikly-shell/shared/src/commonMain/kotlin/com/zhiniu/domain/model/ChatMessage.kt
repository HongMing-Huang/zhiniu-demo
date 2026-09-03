/* 知牛 · 聊天消息（保留基础模型；AI 回复结构化块见 domain/repository/AiService.kt） */
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
    val isStreaming: Boolean = false,
)
