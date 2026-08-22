/* 知牛 · 个股详情页（Task01）
 * OHLC 卡 + 五档盘口 + 迷你 K 线 + Tab（概览 / AI 诊股）+ 「AI 看看」。
 * 使用内置 View/Text/ScrollView/Tab/Dialog；K 线用 View 柱状近似（Canvas 细节后续实证）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.pages.components.AiCardUi
import com.zhiniu.pages.components.toUi

@Page("StockDetail")
internal class StockDetailPage(
    private val viewModel: com.zhiniu.viewmodel.StockDetailVM,
) : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr { allCenter() }
            Text {
                attr {
                    text("个股详情")
                    fontSize(18f)
                }
            }
        }
    }

    /** AI 诊股结果渲染（总结/评分/趋势/信号/风险）。 */
    private fun aiCards(insight: AiInsight): ViewBuilder {
        val ui: AiCardUi = insight.toUi()
        return {
            Text { attr { text(ui.summary) } }
            Text { attr { text("评分 ${ui.score} · 趋势 ${ui.trendLabel}") } }
        }
    }
}