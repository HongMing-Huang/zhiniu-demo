/* 知牛 · AI 分析输出模型（对齐技术方案 §6.4 输出 Schema）*/
package com.zhiniu.domain.model

import kotlinx.serialization.Serializable

/** 涨跌/趋势枚举。 */
@Serializable
enum class Trend { UP, DOWN, SIDEWAYS }

/** 结构化信号。 */
@Serializable
data class AiSignal(val type: String, val detail: String)

/** 结构化风险。 */
@Serializable
data class AiRisk(val type: String, val level: String)

/** 可跳转/内嵌的卡片载荷。 */
@Serializable
data class AiCard(
    val type: String,          // CHART | JUMP
    val title: String,
    val page: String? = null,  // 跳转目标页名
    val args: Map<String, String> = emptyMap(),
    val chart: String? = null, // 预留：迷你走势引用
)

/** AI 诊股 / 问答的结构化结论。 */
@Serializable
data class AiInsight(
    val summary: String,
    val score: Int = 0,
    val trend: Trend = Trend.SIDEWAYS,
    val signals: List<AiSignal> = emptyList(),
    val risks: List<AiRisk> = emptyList(),
    val cards: List<AiCard> = emptyList(),
)