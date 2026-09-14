/* 知牛 · AI 个股洞察模型（AiInsightDrawer 内容源；网关 LLM 诊股 / 规则降级 / 本地 Mock 三源同构） */
package com.zhiniu.domain.model

import kotlinx.serialization.Serializable

/** 个股综合 AI 解读。定位为「信息解释 / 研究辅助」，禁止荐股式结论。 */
@Serializable
data class AiInsight(
    val symbol: String,
    val verdict: String,      // 综合状态：中性偏强 / 中性 / 中性偏弱（LLM 模式为 偏强/中性/偏弱）
    val trend: String,        // 趋势解读
    val volume: String,       // 量能解读
    val indicator: String,    // 指标信号（RSI/MA 等；LLM 模式为信号列表拼接）
    val risk: String,         // 风险提示
    val valuation: String = "",  // 估值判断（高估/合理/低估，真实 PE/PB；空 = 数据不足）
    val earnings: String = "",   // 业绩/卖点解读（真实财报摘要；空 = 数据不足）
    val followUps: List<String> = emptyList(), // 追问建议
    // —— 网关诊股（/agent/insight）扩展字段；本地 Mock 保持默认值 ——
    val source: String = "本地规则生成 · 非模型输出",  // 来源诚实标注
    val signals: List<String> = emptyList(),           // LLM 信号列表（每条引用服务端证据数值）
    val buyZone: String = "",       // 买入观察区间（服务端校验回填；空 = 未提供）
    val sellZone: String = "",      // 卖出观察区间
    val riskLabel: String = "",     // 风险五档中文（低/中低/中/中高/高）
    val riskRationale: String = "", // 风险分级依据
    val generatedAt: String = "",   // 诊股生成时间（后端缓存最长 1h，用户需能辨别新鲜度）
)
