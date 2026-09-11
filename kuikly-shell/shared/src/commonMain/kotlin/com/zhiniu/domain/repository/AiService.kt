/* 知牛 · AI 服务接口与结构化回复模型
 * Mock/Real 差异仅发生在实现层；UI 只消费 AiBlock 列表与 AiInsight。
 */
package com.zhiniu.domain.repository

import com.zhiniu.domain.model.AiInsight

/** AI 回复中的一种结构化块（聊天流逐块渲染）。 */
sealed class AiBlock {
    /** 纯文本段落（可含 **加粗**、\n 换行）。 */
    data class Text(val content: String) : AiBlock()

    /** 股票卡片：点击进入 StockDetailPage。 */
    data class StockCard(
        val symbol: String,
        val trend: String,
        val rsi: Double,
        val summary: String,
    ) : AiBlock()

    /** 指标组：标题 + 多行 label/value。 */
    data class Metrics(val title: String, val rows: List<MetricCell>) : AiBlock()

    /** 风险块。 */
    data class Risk(val title: String, val content: String) : AiBlock()

    /** 追问建议。 */
    data class FollowUps(val questions: List<String>) : AiBlock()
}

/** 指标组内单行。 */
data class MetricCell(val label: String, val value: String)

/** AI 服务：个股洞察 + 会话回复。 */
interface AiService {
    /** 个股综合洞察（AiInsightDrawer 内容源）；fundamentals 为网关真实估值/财报，缺省时对应解读如实标注数据不足。 */
    fun insightFor(symbol: String, fundamentals: AiInsightFundamentals? = null): AiInsight

    /** 问答回复：问题 → 结构化块列表（Mock 确定性；Real 时由 LLM 输出解析而来）。 */
    fun chatReply(sessionId: String, question: String): List<AiBlock>
}

/** 估值/财报解读所需的最小基本面快照（与 UI 层 StockFundamentals 解耦）。 */
data class AiInsightFundamentals(
    val pe: Double? = null,
    val pb: Double? = null,
    val marketCap: Double? = null,
    val reportDate: String = "",
    val revenue: Double? = null,
    val netProfit: Double? = null,
    val roe: Double? = null,
    val grossMargin: Double? = null,
)
