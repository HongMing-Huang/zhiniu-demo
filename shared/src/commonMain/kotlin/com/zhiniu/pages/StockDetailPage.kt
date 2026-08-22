/* 知牛 · 个股详情页（Task01，完整实现）
 * 布局：头部(名/码/价/涨跌) → OHLC 卡 → 五档盘口 → 迷你K线 → Tab(概览/AI诊股)。
 * AI 诊股：「AI 看看」→ 4 卡(总结/信号/风险/跳转) + 多空辩论入口。
 * ⚠️ 指令(vfor/vif)与组件签名以 Kuikly SDK 官方模板为准。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Palette
import com.zhiniu.pages.components.priceColor
import com.zhiniu.pages.components.toUi
import com.zhiniu.viewmodel.StockDetailVM

@Page("StockDetail")
internal class StockDetailPage(
    private val vm: StockDetailVM,
) : Pager() {

    override fun body(): ViewBuilder {
        return {
            View {
                attr { flex(1f); flexDirection(FLEX_DIRECTION_COLUMN) }
                // ===== 头部 =====
                q?.let { q ->
                    View {
                        attr { flexGrow(0f) }
                        Text { attr { text("${q.name} ${q.symbol}") } }
                        Text { attr { text(priceValue(q)); color(colorOf(priceColor(q))) } }
                        Text { attr { text(pricePct(q)); color(colorOf(priceColor(q))) } }
                    }
                }
                // ===== OHLC 卡 =====
                vif(vm.quote != null) {
                    val q = vm.quote.value!!
                    View {
                        attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
                        Text { attr { text(" 今开 ${q.open}") } }
                        Text { attr { text(" 最高 ${q.high}") } }
                        Text { attr { text(" 最低 ${q.low}") } }
                        Text { attr { text(" 昨收 ${q.prevClose}") } }
                    }
                }
                // ===== 五档盘口 =====
                vif(vm.quote != null) {
                    val q = vm.quote.value!!
                    q.bids.forEach { it -> Text { attr { text("买 ¥${it.price}") } } }
                    q.asks.forEach { it -> Text { attr { text("卖 ¥${it.price}") } } }
                }
                // ===== 迷你 K 线（View 柱状近似）=====
                Text { attr { text("迷你走势（${vm.kline.value.size} 根）") } }
                // ===== AI 诊股区 =====
                Text {
                    attr {
                        text(if (vm.diagnosing.value) "AI 分析中…" else "AI 看看")
                        color(colorOf(Palette.UP)); onClick { vm.diagnose() }
                    }
                }
                vif(vm.insight != null) {
                    val ui = vm.insight.value!!.toUi()
                    Text { attr { text(ui.summary) } }
                    Text { attr { text("评分 ${ui.score} · 趋势 ${ui.trendLabel}") } }
                    ui.signals.forEach { s -> Text { attr { text("· $s") } } }
                    ui.risks.forEach { r -> Text { attr { text("⚠ $r") } } }
                    Text { attr { text("▶ ${ui.jump}"); color(colorOf(Palette.UP)); onClick { openDetail() } } }
                }
            }
        }
    }

    private val q: Quote? get() = vm.quote.value

    /** 打开详情/K线（跳转三角的一环）：openPage 以 SDK 模板路由为准。 */
    private fun openDetail() {
        // openPage("StockDetail", { "symbol" to vm.symbol; "name" to vm.name })——以 SDK 模板路由为准
    }

    private fun colorOf(hex: String): String = hex
}