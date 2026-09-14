/* 知牛 · Mock AI 服务（确定性文案：同问题永远同答案，便于录屏）
 * 接真实 LLM 时替换 MarketStore.aiService（见 docs/ARCHITECTURE.md）。
 */
package com.zhiniu.data.mock

import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.repository.AiBlock
import com.zhiniu.domain.repository.AiInsightFundamentals
import com.zhiniu.domain.repository.AiService
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.domain.repository.MetricCell
import com.zhiniu.pages.components.fmtAmount
import kotlin.math.roundToInt

class MockAiService(
    private val repository: MarketRepository = MockMarketRepository(),
) : AiService {

    override fun insightFor(symbol: String, fundamentals: AiInsightFundamentals?): AiInsight {
        val q = repository.quoteOf(symbol)
        val name = q?.name ?: symbol
        val h = hash(symbol)
        val rsi = 38.0 + (h % 320) / 10.0
        val trendText = "价格仍位于 MA20 上方，短期趋势保持稳定；均线多头排列，未出现明显背离。"
        val volumeText = "近 5 日量能温和，没有明显放大；上涨斜率平缓，追涨资金有限。"
        val indicatorText = "RSI ${fmt1(rsi)}，处于中性区域；MACD 红柱缩短，动能有所减弱。"
        val riskText = "若价格重新跌破 MA20，当前区间结构需要重新评估；大盘波动率上升时个股跟随性增强，注意仓位纪律。"
        // 估值判断（课题评分点「高估/低估判断」）：真实 PE/PB 规则化，无数据时如实标注
        val valuationText = fundamentals?.let { f ->
            val pe = f.pe
            val pb = f.pb
            when {
                pe != null && pe <= 0 -> "PE ${fmt1(pe)}（亏损或微利），估值锚失效，需以 PB ${pb?.let { fmt2(it) } ?: "—"} 与业务拐点为主。"
                pe != null && pb != null -> {
                    val verdict = when {
                        pe < 15 && pb < 2 -> "偏低估"
                        pe <= 40 -> "合理区间"
                        else -> "偏高估"
                    }
                    "PE ${fmt1(pe)} / PB ${fmt2(pb)}，规则判定「$verdict」；适合与行业中位及自身历史分位对照。"
                }
                pe != null -> "PE ${fmt1(pe)}（PB 缺失），单一口径参考意义有限，建议等基本面数据补齐后再判断。"
                else -> ""
            }
        } ?: ""
        // 业绩解读（课题评分点「卖点/业绩解读」）：真实财报摘要规则化
        val earningsText = fundamentals?.takeIf { it.revenue != null || it.netProfit != null }?.let { f ->
            val parts = mutableListOf<String>()
            f.reportDate.takeIf { it.isNotBlank() }?.let { parts.add("$it 报告期") }
            f.revenue?.let { parts.add("营收 ${fmtAmount(it)}") }
            f.netProfit?.let { parts.add("归母净利 ${fmtAmount(it)}") }
            f.grossMargin?.let { parts.add("毛利率 ${fmt1(it)}%") }
            f.roe?.let { parts.add("ROE ${fmt1(it)}%") }
            val quality = when {
                (f.netProfit ?: 0.0) > 0 && (f.roe ?: 0.0) >= 12.0 -> "盈利质量较强，是当前核心卖点"
                (f.netProfit ?: 0.0) > 0 -> "整体盈利，关注增长的持续性"
                else -> "盈利承压，反转节奏是关键变量"
            }
            "${parts.joinToString("，")}；$quality。"
        } ?: ""
        return AiInsight(
            symbol = symbol,
            verdict = if (h % 3 == 0) "中性偏弱" else "中性偏强",
            trend = trendText,
            volume = volumeText,
            indicator = indicatorText,
            risk = riskText,
            valuation = valuationText,
            earnings = earningsText,
            followUps = listOf("为什么说量能不足？", "解释 RSI 指标", "结合日K分析", "关键支撑位在哪"),
        )
    }

    override fun chatReply(sessionId: String, question: String): List<AiBlock> {
        val q = question.trim()
        val target = repository.stockQuotes()
            .firstOrNull { q.contains(it.name) || q.contains(it.code) || q.contains(it.pinyin) }
        return if (target != null) {
            stockReply(target.symbol, q)
        } else {
            marketReply(q)
        }
    }

    /** 个股问答 → 结构化块。 */
    private fun stockReply(symbol: String, question: String): List<AiBlock> {
        val q = repository.quoteOf(symbol)!!
        val h = hash(symbol)
        val rsi = 38.0 + (h % 320) / 10.0
        val pe = 12.0 + (h % 300) / 10.0
        val pb = 1.2 + (h % 90) / 10.0
        val volRatio = 0.7 + (h % 130) / 100.0
        val cap = q.amount * (9 + (h % 40))
        val capText = if (cap >= 1e12) fmt2(cap / 1e12) + "T" else fmt2(cap / 1e8) + "亿"
        return listOf(
            // Markdown 演示（Task2 渲染评分点）：标题 / 列表 / 代码块 / 来源，离线态同样可演示
            AiBlock.Text(
                buildString {
                    appendLine("### ${q.name}（${q.code}.${q.marketSuffix}）研究快照")
                    appendLine()
                    appendLine("当前报 **${fmt2(q.price)} 元**，涨跌 ${fmt1(q.changePercent)}%。")
                    appendLine("从日 K 与成交量看，处于震荡整理结构，短线情绪中性。")
                    appendLine()
                    appendLine("**关注要点**")
                    appendLine("- 量比 ${fmt2(volRatio)}，观察量能能否同步放大")
                    appendLine("- MA20 得失决定短线结构")
                    appendLine("- RSI(14) = ${fmt1(rsi)}，尚未触及极值区间")
                    appendLine()
                    appendLine("RSI 参考口径：")
                    appendLine("```text")
                    appendLine("RSI14 = 100 - 100 / (1 + avg_gain_14 / avg_loss_14)")
                    appendLine("```")
                    // 来源标注放末尾：诚实性保留，视觉不再抢占首屏
                    appendLine()
                    appendLine("> 来源：本地确定性快照 · 离线演示模式")
                },
            ),
            AiBlock.StockCard(
                symbol = symbol,
                trend = if (h % 3 == 0) "中性偏弱" else "中性偏强",
                rsi = rsi,
                summary = "价格位于 MA20 上方，量能温和，趋势未破坏。",
            ),
            AiBlock.Metrics(
                title = "关键指标",
                rows = listOf(
                    MetricCell("市盈率", fmt2(pe)),
                    MetricCell("市净率", fmt2(pb)),
                    MetricCell("总市值", capText),
                    MetricCell("量比", fmt2(volRatio)),
                    MetricCell("RSI(14)", fmt1(rsi)),
                ),
            ),
            AiBlock.Risk(
                title = "风险提示",
                content = "量能没有同步扩大，注意突破持续性；若价格跌破 MA20，短线结构需重新评估。本结论仅供研究参考，不构成投资建议。",
            ),
            AiBlock.FollowUps(
                questions = listOf("为什么说量能不足？", "解释 RSI 指标", "结合日K分析", "对比宁德时代"),
            ),
        )
    }

    /** 非个股问题 → 市场观点。 */
    private fun marketReply(question: String): List<AiBlock> {
        val breadth = repository.breadth()
        return listOf(
            AiBlock.Text(
                "今天沪深两市上涨 ${breadth.upCount} 家、下跌 ${breadth.downCount} 家，成交 ${breadth.amountYi} 亿，" +
                    "市场宽度${breadth.status}。以下是今日要点："
            ),
            AiBlock.Metrics(
                title = "市场速览",
                rows = repository.indices().map {
                    MetricCell(it.name, fmt2(it.price) + "  " + fmtSignedPct(it.changePercent))
                } + listOf(MetricCell("上涨/下跌", "${breadth.upCount}/${breadth.downCount}")),
            ),
            AiBlock.Risk(
                title = "提示",
                content = "当前行情由实时网关提供，网络不可用时自动保留最近快照；数据仅供研究参考。",
            ),
            AiBlock.FollowUps(
                questions = listOf("分析贵州茅台", "今天哪些板块强势？", "帮我看看宁德时代"),
            ),
        )
    }

    private fun hash(s: String): Int {
        var h = 7
        for (c in s) { h = h * 31 + c.code }
        return h and Int.MAX_VALUE
    }

    private fun fmt2(v: Double): String {
        val neg = v < 0
        val a = kotlin.math.abs(v)
        val s = (a * 100).roundToInt().toString().padStart(3, '0')
        return (if (neg) "-" else "") + s.substring(0, s.length - 2) + "." + s.substring(s.length - 2)
    }

    private fun fmt1(v: Double): String {
        val neg = v < 0
        val a = kotlin.math.abs(v)
        val s = (a * 10).roundToInt().toString().padStart(2, '0')
        return (if (neg) "-" else "") + s.substring(0, s.length - 1) + "." + s.substring(s.length - 1)
    }

    private fun fmtSignedPct(v: Double): String = (if (v >= 0) "+" else "") + fmt2(v) + "%"
}
