// 知牛 · 个股基础事实 + 确定性 AI 观点（演示数据，随 symbol 哈希稳定生成）
package com.zhiniu.pages.components

import com.zhiniu.domain.model.Quote

/** 稳定哈希（同 symbol 恒定）。 */
internal fun stableHash(s: String): Int {
    var h = 7
    for (c in s) { h = h * 31 + c.code }
    return h and Int.MAX_VALUE
}

/** 个股关键数据（演示）。 */
data class StockFacts(
    val pe: Double,        // 市盈率
    val pb: Double,        // 市净率
    val marketCap: String, // 总市值
    val floatCap: String,  // 流通值
    val turnover: Double,  // 换手率 %
    val volumeRatio: Double, // 量比
    val week52High: Double,
    val week52Low: Double,
)

fun factsOf(q: Quote): StockFacts {
    val h = stableHash(q.symbol)
    val pe = 12.0 + (h % 300) / 10.0
    val pb = 1.2 + (h % 90) / 10.0
    val turnover = 0.25 + (h % 160) / 100.0
    val vr = 0.7 + (h % 130) / 100.0
    val cap = q.amount * (9 + (h % 40))
    val capStr = if (cap >= 1e12) fmt2(cap / 1e12) + "T" else fmt2(cap / 1e8) + "亿"
    return StockFacts(
        pe = pe, pb = pb,
        marketCap = capStr, floatCap = capStr,
        turnover = turnover, volumeRatio = vr,
        week52High = q.price * (1.08 + (h % 5) / 40.0),
        week52Low = q.price * (0.78 - (h % 4) / 100.0),
    )
}

/** AI 观点（确定性：同股票永远同文案）。 */
data class AiView(
    val verdict: String,       // 综合观点
    val trend: String,         // 趋势
    val momentum: String,      // 动量
    val rsi: Double,           // RSI
    val valuation: String,     // 估值
    val observe: String,       // 核心观察
    val risk: String,          // 风险
)

fun aiViewOf(q: Quote): AiView {
    val h = stableHash(q.symbol)
    val verdicts = listOf("中性偏强", "偏强", "中性", "偏强", "中性偏强")
    val trends = listOf("偏强", "中性偏强", "偏强", "中性")
    val momentums = listOf("中性", "偏强", "中性偏弱", "偏强")
    val rsi = 38.0 + (h % 320) / 10.0
    val observes = listOf(
        "价格仍处于 MA20 上方，短期趋势保持稳定；量能温和，未出现明显背离。",
        "股价围绕短期均线震荡上行，结构保持健康，可关注波段低吸机会。",
        "短线动能有所增强，但上方压力仍需时间消化，持股待涨为宜。",
    )
    val risks = listOf(
        "量能没有同步扩大，注意突破持续性；若跌破 MA20 需警惕回踩。",
        "板块轮动较快，谨防冲高回落；高位分歧加大，不宜追高。",
        "大盘波动率上升，个股跟随性增强，注意仓位与止损纪律。",
    )
    return AiView(
        verdict = verdicts[h % verdicts.size],
        trend = trends[(h / 3) % trends.size],
        momentum = momentums[(h / 5) % momentums.size],
        rsi = rsi,
        valuation = if ((h / 7) % 2 == 0) "合理" else "偏低",
        observe = observes[(h / 11) % observes.size],
        risk = risks[(h / 13) % risks.size],
    )
}

/** AI 市场观点（AI研究 页）。 */
data class MarketInsight(val tag: String, val text: String, val tagHex: String)

fun marketInsights(): List<MarketInsight> = listOf(
    MarketInsight("板块", "白酒、新能源获主力净流入居前，医药板块有资金回流迹象。", "#5B7CFA"),
    MarketInsight("情绪", "全市场风险偏好回升，涨停家数创近 5 日新高，短线情绪偏暖。", "#2F9E6E"),
    MarketInsight("机会", "关注业绩预增且估值处于历史分位低位的消费与先进制造龙头。", "#D94F4F"),
)
