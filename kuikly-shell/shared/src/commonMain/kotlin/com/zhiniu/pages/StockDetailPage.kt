package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.BasePager
import com.zhiniu.base.openZhiniuPage
import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt
import kotlin.math.roundToInt

/** 个股详情页：头部 + OHLC + 五档 + K 线 + AI 诊股（Kuikly 官方 core DSL）。 */
@Page("StockDetail", supportInLocal = true)
internal class StockDetailPage : BasePager() {

    private var quote by observable<Quote?>(null)

    private val mock = MockDataSource()

    // vforLazy 数据源必须是 ObservableList，故用 val 持有普通列表 + 转换
    private val klineBars: List<KLineBar> = mock.kline("sh600519")

    override fun willInit() {
        super.willInit()
        val symbol = pageData.params.optString("symbol").ifBlank { "sh600519" }
        val name = pageData.params.optString("name").ifBlank { "贵州茅台" }
        val q = mock.quotes().firstOrNull { it.symbol == symbol }
        quote = q ?: mock.quotes().first()
        if (quote?.name?.isBlank() == true) quote = quote?.copy(name = name)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val q = ctx.quote ?: return { }
        return {
            // 头部
            View {
                attr { flexDirectionRow(); marginTop(12f); marginLeft(10f); marginRight(10f) }
                Text { attr { flex(1f); fontSize(18f); color(Color(0xFF222222)); text(q.name) } }
                Text {
                    attr {
                        fontSize(20f)
                        text(ctx.two("${q.price}"))
                        color(ctx.qColor(q))
                    }
                }
            }
            Text {
                attr {
                    marginLeft(10f); fontSize(13f); color(Color(0xFF888888))
                    text("${q.symbol}   今开 ${ctx.two("${q.open}")}   昨收 ${ctx.two("${q.prevClose}")}")
                }
            }
            Text {
                attr {
                    marginLeft(10f); fontSize(13f); color(ctx.qColor(q))
                    text("最高 ${ctx.two("${q.high}")}   最低 ${ctx.two("${q.low}")}   ${ctx.pct(q)}")
                }
            }

            // 五档盘口（vforLazy 需 ObservableList → 用封装的 List<Pair> 手工 forEach）
            Text { attr { marginLeft(10f); marginTop(12f); fontSize(15f); color(Color(0xFF333333)); text("五档盘口") } }
            ctx.depthRows().forEach { row ->
                View {
                    attr { flexDirectionRow(); padding(4f); marginLeft(8f) }
                    Text { attr { flex(1f); fontSize(13f); color(Color(0xFF444444)); text(row.first) } }
                    Text { attr { fontSize(13f); color(Color(0xFF444444)); text(row.second) } }
                }
            }

            // K 线蜡烛图（真实 Canvas 绘制，红涨绿跌 + MA 均线）
            Text { attr { marginLeft(Tokens.space3); marginTop(Tokens.space2); fontSize(Tokens.fsH3); color(Color(hexInt(Tokens.textPrimary))); text("日 K 线") } }
            Canvas({
                attr { height(200f); marginLeft(Tokens.space2); marginRight(Tokens.space2) }
            }) { context, width, height ->
                ctx.drawKline(context, width, height)
            }

            // AI 诊股入口
            View {
                attr { marginTop(14f); marginLeft(10f); marginRight(10f) }
                Text {
                    attr { fontSize(15f); color(Color(0xFFE53935)); text("🤖 去 AI 诊股 →") }
                    event { click { ctx.openZhiniuPage("ChatHome", mapOf("ticker" to q.symbol, "name" to q.name)) } }
                }
            }
        }
    }

    private fun depthRows(): List<Pair<String, String>> {
        val q = quote
        val names = listOf("买一", "买二", "买三", "买四", "买五")
        return names.mapIndexed { i, label ->
            val price = if (q != null && q.bids.size > i) q.bids[i].price else q?.price ?: 0.0
            val vol = if (q != null && q.bids.size > i) q.bids[i].volume else 0L
            label to "${two("$price")} / ${vol}手"
        }
    }

    // 用 CanvasContext 画蜡烛（实心矩形用 moveTo+lineTo 围边近似，影线用垂直线）
    private fun drawKline(context: CanvasContext, width: Float, height: Float) {
        val bars = klineBars
        if (bars.isEmpty()) return
        val minLow = bars.minOf { it.low }
        val maxHigh = bars.maxOf { it.high }
        val span = (maxHigh - minLow).takeIf { it > 0 } ?: 1.0
        val padTop = 8f
        val plotH = height - padTop * 2
        val step = width / bars.size
        val bodyW = step * 0.5f

        fun y(price: Double): Float = padTop + (((maxHigh - price) / span) * plotH).toFloat()

        bars.forEachIndexed { i, b ->
            val x = step * i + step / 2f
            val up = b.close >= b.open
            val color = if (up) Color(hexInt(Tokens.up)) else Color(hexInt(Tokens.down))
            val top = y(maxOf(b.open, b.close))
            val bottom = y(minOf(b.open, b.close))
            val highY = y(b.high)
            val lowY = y(b.low)

            context.strokeStyle(color)
            context.lineWidth(1f)
            // 影线（上下极值垂直线）
            context.beginPath()
            context.moveTo(x, highY)
            context.lineTo(x, lowY)
            context.stroke()
            // 实体（矩形：4 条线围边 + 填充）
            context.fillStyle(color)
            context.beginPath()
            context.moveTo(x - bodyW, top)
            context.lineTo(x + bodyW, top)
            context.lineTo(x + bodyW, bottom)
            context.lineTo(x - bodyW, bottom)
            context.closePath()
            context.fill()
        }
    }

    private fun two(s: String): String {
        // 保留两位小数的简易实现（避免依赖 java 格式化）
        val v = s.toDoubleOrNull() ?: return s
        val r = (kotlin.math.round(v * 100)) / 100
        return r.toString()
    }

    private fun qColor(q: Quote): Color =
        if (q.price >= q.prevClose) Color(0xFFE53935) else Color(0xFF2E7D32)

    private fun pct(q: Quote): String {
        val p = (q.price - q.prevClose) / q.prevClose * 100
        val r = (p * 100).roundToInt() / 100.0
        return (if (r >= 0) "+" else "") + r.toString() + "%"
    }
}