/* 知牛 · 总结卡 SummaryCard（AI 诊股第 1 卡：Markdown 摘要 + 0-100 评分色条）
 *
 * // Example: summaryCard(SummaryCardUi(summary="震荡向上…", score=78, trendLabel="看多"))
 *
 * 说明：summary 最终交由 KuiklyMarkdown 渲染（T2-1.1），此处得分条用纯 View 表达。
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder
import com.zhiniu.domain.model.AiInsight

/** 总结卡展示模型（纯数据，可单测）。 */
data class SummaryCardUi(
    val summary: String,
    val score: Int,          // 0..100
    val trendLabel: String,  // 看多/看空/震荡
)

/** 由 AiInsight 装配（复用 Widgets.toUi 的 trend 标签）。 */
fun AiInsight.toSummaryUi(): SummaryCardUi =
    SummaryCardUi(summary = summary, score = score.coerceIn(0, 100), trendLabel = toUi().trendLabel)

/** 评分色条宽度占比（0..1）。 */
fun scoreBarFraction(score: Int): Float = score.coerceIn(0, 100) / 100f

/** 生成总结卡 DSL 块（仅 View/Text，Kuikly 内置）。 */
fun summaryCard(ui: SummaryCardUi): ViewBuilder = {
    View {
        attr { flex(1f); marginTop(8f) }
        // 摘要（流式 Markdown 见 KuiklyMarkdown 契约，此处先用文本兜底）
        Text { attr { text(ui.summary); fontSize(14f); color(colorOf(Palette.TEXT)) } }
        // 评分行：标签 + 分数
        Text { attr { text("${ui.trendLabel}  评分 ${ui.score}"); color(colorOf(Palette.SUB)); fontSize(12f); marginTop(6f) } }
        // 评分色条：两段 flex 等比（填充段 = score%，其余 = 100%-score%）
        View {
            attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f); marginTop(4f); height(6f) }
            View { attr { flexGrow(scoreBarFraction(ui.score) * 100f); height(6f); background(colorOf(scoreColor(ui.score))) } }
            View { attr { flexGrow((1f - scoreBarFraction(ui.score)) * 100f); height(6f); background(colorOf(Palette.BG)) } }
        }
    }
}

/** 评分 → 色（低~高：红→橙→黄→绿）。 */
fun scoreColor(score: Int): String = when {
    score >= 80 -> Palette.DOWN
    score >= 60 -> "#f5a623"
    score >= 40 -> "#f78f1e"
    else -> Palette.UP
}