/* 知牛 · AI 个股洞察模型（AiInsightDrawer 内容源；确定性 Mock 生成） */
package com.zhiniu.domain.model

import kotlinx.serialization.Serializable

/** 个股综合 AI 解读。定位为「信息解释 / 研究辅助」，禁止荐股式结论。 */
@Serializable
data class AiInsight(
    val symbol: String,
    val verdict: String,      // 综合状态：中性偏强 / 中性 / 中性偏弱
    val trend: String,        // 趋势解读
    val volume: String,       // 量能解读
    val indicator: String,    // 指标信号（RSI/MA 等）
    val risk: String,         // 风险提示
    val valuation: String = "",  // 估值判断（高估/合理/低估，真实 PE/PB 规则化；空 = 数据不足）
    val earnings: String = "",   // 业绩/卖点解读（真实财报摘要；空 = 数据不足）
    val followUps: List<String> = emptyList(), // 追问建议
)
