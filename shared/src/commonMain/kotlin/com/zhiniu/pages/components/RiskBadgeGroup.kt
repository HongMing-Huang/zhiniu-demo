/* 知牛 · 风险徽章组 RiskBadgeGroup（AI 诊股第 3 卡：HIGH 红/MEDIUM 橙/LOW 黄 + 左边框色条）
 *
 * // Example: riskBadgeGroup(listOf(RiskBadgeUi("HIGH","波动风险")))
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder
import com.zhiniu.domain.model.AiRisk

/** 风险徽章展示模型。 */
data class RiskBadgeUi(val level: String, val label: String)

/** AiRisk → 徽章模型（level 规范为大写）。 */
fun risksToBadges(value: List<AiRisk>): List<RiskBadgeUi> =
    value.map { RiskBadgeUi(it.level.uppercase(), "${it.level} · ${it.type}") }

/** 三级风险配色。 */
fun riskColor(level: String): String = when (level.uppercase()) {
    "HIGH" -> Palette.UP      // 红
    "MEDIUM" -> "#f5a623"     // 橙
    "LOW" -> "#f7c948"        // 黄
    else -> Palette.SUB
}

/** 生成风险徽章组 DSL（左边框色条 + 文本）。 */
fun riskBadgeGroup(badges: List<RiskBadgeUi>): ViewBuilder = {
    View {
        attr { flexDirection(FLEX_DIRECTION_COLUMN); flexGrow(1f); marginTop(8f) }
        badges.forEach { b ->
            View {
                attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f); marginTop(4f) }
                // 左边框色条
                View { attr { flexGrow(0f); width(4f); height(20f); background(colorOf(riskColor(b.level))) } }
                Text { attr { text(b.label); color(colorOf(riskColor(b.level))); fontSize(12f); marginStart(6f) } }
            }
        }
    }
}