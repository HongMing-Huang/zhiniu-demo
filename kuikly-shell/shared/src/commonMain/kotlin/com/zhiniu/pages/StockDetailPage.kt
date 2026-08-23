/* 知牛 · 个股详情页（接线：OHLC 量额 / AI 四卡 / 跳转闭环）
 * 布局：头部(名/码/价/涨跌) → OHLC 卡(含量/额) → 五档(数据就绪，渲染预留 KuiklyTableView) → K 线 → AI 诊股 4 卡。
 * ⚠️ 指令(vfor/vif)与组件签名以 Kuikly SDK 官方模板为准；navigator 由壳工程注入实现 openPage。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text
import com.zhiniu.domain.model.AppError
import com.zhiniu.domain.model.Quote
import com.zhiniu.domain.model.QuoteDisplay
import com.zhiniu.pages.components.appErrorCard
import com.zhiniu.pages.components.buildCandlestickPaint
import com.zhiniu.pages.components.colorOf
import com.zhiniu.pages.components.jumpCard
import com.zhiniu.pages.components.riskBadgeGroup
import com.zhiniu.pages.components.risksToBadges
import com.zhiniu.pages.components.signalPillGroup
import com.zhiniu.pages.components.signalsToPills
import com.zhiniu.pages.components.skeletonKline
import com.zhiniu.pages.components.summaryCard
import com.zhiniu.pages.components.toJumpUi
import com.zhiniu.pages.components.toSummaryUi
import com.zhiniu.viewmodel.StockDetailVM

@Page("StockDetail")
internal class StockDetailPage(
    private val vm: StockDetailVM,
    /** 壳工程注入的路由实现：openPage(page, args)。 */
    private val navigator: (String, Map<String, String>) -> Unit = { _, _ -> },
) : Pager() {

    override fun body(): ViewBuilder {
        return {
            View {
                attr { flex(1f); flexDirection(FLEX_DIRECTION_COLUMN) }

                // ===== 头部 =====
                vif(vm.quote != null) { header(vm.quote.value!!) }

                // ===== OHLC 卡（含量/额）=====
                vif(vm.quote != null) {
                    val qq = vm.quote.value!!
                    View {
                        attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
                        ohlc("今开", QuoteDisplay.price(qq.open))
                        ohlc("最高", QuoteDisplay.price(qq.high))
                        ohlc("最低", QuoteDisplay.price(qq.low))
                        ohlc("昨收", QuoteDisplay.price(qq.prevClose))
                        ohlc("量", QuoteDisplay.volume(qq.volume))
                        ohlc("额", QuoteDisplay.amountFromWan(qq.amount / 10000.0))
                    }
                }

                // ===== 五档盘口：数据就绪，渲染预留 KuiklyTableView（T2-2.1，见 DEVELOPMENT-ISSUES）=====
                vif(vm.quote != null) {
                    val rows = QuoteDisplay.depthRows(vm.quote.value!!.bids, vm.quote.value!!.asks)
                    Text { attr { text("五档（${rows.size} 行，表格待 UI 接 TableView）"); color(colorOf(com.zhiniu.pages.components.Palette.SUB)); fontSize(11f) } }
                }

                // ===== K 线：加载骨架 → 就绪后由 Canvas 自绘（paint 已计算，渲染契约见 CandlestickChart.kt）=====
                vif(vm.loading) { skeletonKline(height = 180f) }
                vif(!vm.loading) {
                    val paint = buildCandlestickPaint(vm.kline.value, widthPx = 360f, heightPx = 180f, showMA = true)
                    Text { attr { text("日K（${paint.candles.size} 根 · Canvas 自绘）"); fontSize(11f); color(colorOf(com.zhiniu.pages.components.Palette.SUB)) } }
                }

                // ===== AI 诊股区 =====
                Text {
                    attr {
                        text(if (vm.diagnosing.value) "AI 分析中…" else "AI 看看")
                        color(colorOf(com.zhiniu.pages.components.Palette.UP))
                        onClick { vm.diagnose() }
                    }
                }
                vif(vm.diagnosing) { skeletonKline(height = 120f) }
                vif(vm.aiError != null) {
                    // 独立 AI 错误重试卡（不并入全局 Error）
                    appErrorCard(AppError.Unknown(vm.aiError.value)) { vm.diagnose() }
                }
                vif(vm.insight != null) {
                    val i = vm.insight.value!!
                    summaryCard(i.toSummaryUi())
                    signalPillGroup(signalsToPills(i.signals))
                    riskBadgeGroup(risksToBadges(i.risks))
                    // 跳转卡：路由到 AiCard.page / args（Mock 已带 symbol/name）
                    i.cards.firstOrNull { it.type == "JUMP" }?.let { c ->
                        jumpCard(c.toJumpUi()) { page, args -> navigator(page, args) }
                    }
                }
            }
        }
    }

    private fun header(qq: Quote): ViewBuilder = {
        View {
            attr { flexGrow(0f); flexDirection(FLEX_DIRECTION_COLUMN) }
            Text { attr { text(qq.name + " " + qq.symbol); fontSize(16f); color(colorOf(com.zhiniu.pages.components.Palette.TEXT)) } }
            Text {
                attr {
                    text(com.zhiniu.pages.components.priceValue(qq) + "  " + com.zhiniu.pages.components.pricePct(qq))
                    color(colorOf(com.zhiniu.pages.components.priceColor(qq)))
                    fontSize(18f)
                }
            }
        }
    }

    private fun ohlc(label: String, value: String): ViewBuilder = {
        Text { attr { text("$label $value  "); fontSize(12f); color(colorOf(com.zhiniu.pages.components.Palette.TEXT)) } }
    }
}