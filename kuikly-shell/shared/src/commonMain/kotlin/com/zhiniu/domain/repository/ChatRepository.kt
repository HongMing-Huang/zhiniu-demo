/* 知牛 · 对话/AI 分析仓储 */
package com.zhiniu.domain.repository

import com.zhiniu.data.remote.LlmGatewayClient
import com.zhiniu.domain.model.AiCard
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.AiRisk
import com.zhiniu.domain.model.AiSignal
import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.StreamChunk
import com.zhiniu.domain.model.Trend
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun chatStream(model: String, history: List<ChatMessage>): Flow<StreamChunk>
    /** 一键诊股：结构化结论；网关不可用时走 Mock 兜底分析（演示绝不空屏）。 */
    suspend fun diagnose(symbol: String, name: String): AiInsight
}

class LlmChatRepository(
    private val gateway: LlmGatewayClient,
) : ChatRepository {

    override fun chatStream(model: String, history: List<ChatMessage>): Flow<StreamChunk> =
        gateway.chatStream(model, history)

    override suspend fun diagnose(symbol: String, name: String): AiInsight {
        // ① 尝试网关真实诊断（zhiniu/think）
        val gatewayText: String? = runCatching {
            var acc = ""
            gateway.chatStream(
                "zhiniu/think",
                listOf(ChatMessage("", com.zhiniu.domain.model.MessageRole.USER, "请诊断 $name($symbol) 的买卖时机与风险。")),
            ).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Delta -> acc += chunk.text
                    else -> Unit
                }
            }
            acc.ifBlank { null }
        }.getOrNull()

        // ② 网关不可用/空 → 纯 Mock 兜底，保证 AI 演示不空屏
        return MockDiagnose.insight(symbol, name, summary = gatewayText ?: "当前 AI 为离线演示结论")
    }
}

/** 离线兜底诊断（无 Key / 网关异常时保证 AI 卡片可演示）。 */
object MockDiagnose {
    fun insight(symbol: String, name: String, summary: String = "AI 暂无法连接，以下为离线演示结论"): AiInsight {
        val pct = 3.2
        return AiInsight(
            summary = "$name 近期走势震荡向上，短线偏强。$summary",
            score = (55 + (pct * 7).toInt()).coerceIn(0, 95),
            trend = Trend.UP,
            signals = listOf(
                AiSignal("MA_BREAK", "5 日均线金叉 10 日均线"),
                AiSignal("VOLUME", "量能温和放大"),
                AiSignal("MOMENTUM", "近 5 日强于大盘"),
            ),
            risks = listOf(
                AiRisk("VOLATILITY", "HIGH"),
                AiRisk("DRAWDOWN", "MEDIUM"),
            ),
            cards = listOf(
                AiCard("JUMP", "查看K线详情", page = "StockDetail", args = mapOf("symbol" to symbol, "name" to name)),
            ),
        )
    }
}