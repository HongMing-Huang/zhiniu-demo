/* 知牛 · 聊天归档（会话 + 消息本地持久化）
 * 对标 KuiklyStock ChatStore（SharedPreferences 持久化）：刷新 / 冷启动不再丢会话。
 * 页面层在消息落定后 serialize() 写 SharedPreferences，进入聊天页时 restore() 恢复。
 * 结构化卡片（Metrics / KLine / TradeAdvice 等）不归档，只保留 Markdown 正文、
 * 工具反馈卡与结论徽章——恢复后仍可读、可点，重问即可重新生成完整卡片。
 */
package com.zhiniu.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.zhiniu.domain.repository.AiBlock

@Serializable
data class ChatRecord(
    val role: String,               // "user" | "ai"
    val text: String = "",          // user 原文 / ai Markdown 正文
    val toolTitle: String = "",     // AiBlock.ToolResult（恢复后重建反馈卡）
    val toolDetail: String = "",
    val toolAction: String = "",
    val degraded: Boolean = false,  // ai 规则降级回复（恢复后保留降级标识）
    val verdictRisk: String = "",   // 结论徽章（【AI观点】）
    val verdictAction: String = "",
)

@Serializable
data class ChatArchiveSession(
    val id: String,
    val title: String,
    val createdAt: String,
    val records: List<ChatRecord> = emptyList(),
)

object ChatArchive {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun serialize(sessions: List<ChatArchiveSession>): String = json.encodeToString(sessions)

    fun deserialize(raw: String): List<ChatArchiveSession> =
        runCatching { json.decodeFromString<List<ChatArchiveSession>>(raw) }.getOrDefault(emptyList())

    /** AI 回复（结构化块）→ 归档记录：正文取 Text 块，反馈卡/徽章取对应块。 */
    fun aiRecord(blocks: List<AiBlock>): ChatRecord {
        val text = aiTextOf(blocks)
        val tool = blocks.filterIsInstance<AiBlock.ToolResult>().firstOrNull()
        val verdict = blocks.filterIsInstance<AiBlock.Verdict>().firstOrNull()
        val degraded = blocks.filterIsInstance<AiBlock.Risk>()
            .any { it.title.contains("规则降级") }
        return ChatRecord(
            role = "ai",
            text = text,
            toolTitle = tool?.title.orEmpty(),
            toolDetail = tool?.detail.orEmpty(),
            toolAction = tool?.action.orEmpty(),
            degraded = degraded,
            verdictRisk = verdict?.risk.orEmpty(),
            verdictAction = verdict?.action.orEmpty(),
        )
    }

    /** AI 消息的纯文本投影（多轮上下文 / 归档共用）：Text 全文 + 卡片摘要行。 */
    fun aiTextOf(blocks: List<AiBlock>): String = blocks.mapNotNull { block ->
        when (block) {
            is AiBlock.Text -> block.content
            is AiBlock.ToolResult -> "（${block.title}：${block.detail}）"
            is AiBlock.Risk -> "${block.title}：${block.content}"
            is AiBlock.Verdict -> "【AI观点】风险：${block.risk}｜操作建议：${block.action}"
            else -> null
        }
    }.joinToString("\n").trim()

    /** 归档记录 → 可渲染块（用户消息走页面分支，不经过这里）。 */
    fun blocksOf(record: ChatRecord): List<AiBlock> {
        val blocks = mutableListOf<AiBlock>()
        if (record.degraded) {
            blocks += AiBlock.Risk("规则降级 · 未伪装模型", record.text)
        } else if (record.text.isNotBlank()) {
            blocks += AiBlock.Text(record.text)
        }
        if (record.verdictRisk.isNotBlank() && record.verdictAction.isNotBlank()) {
            blocks += AiBlock.Verdict(record.verdictRisk, record.verdictAction)
        }
        if (record.toolTitle.isNotBlank()) {
            blocks += AiBlock.ToolResult(
                title = record.toolTitle,
                detail = record.toolDetail,
                action = record.toolAction,
                actionLabel = if (record.toolAction.isNotBlank()) "查看" else "",
            )
        }
        return blocks
    }
}
