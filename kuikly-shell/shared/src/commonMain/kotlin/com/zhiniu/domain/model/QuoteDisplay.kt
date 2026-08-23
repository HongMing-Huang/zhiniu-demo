/* 知牛 · 行情展示纯逻辑（KMP 安全，不依赖 java.util / String.format）
 *
 * 说明：commonMain 跨多端（Android/WASM/iOS/鸿蒙/Mock）编译，禁止 java.util.*。
 * 本层提供 价格/涨跌/量/额 的安全格式化 + 五档盘口行（中文档位 一~五）。
 * 用途：T2-4 OHLC 量/额两字段、T2-2.1 五档盘口（KuiklyTableView 消费）、UI 顶部价格展示。
 */
package com.zhiniu.domain.model

/** 五档盘口行（喂给 KuiklyTableView 的行模型）。 */
data class DepthRow(val level: String, val side: String, val price: Double, val volume: Long)

/** 展示纯逻辑助手。 */
object QuoteDisplay {

    val CHINESE_LEVEL = listOf("一", "二", "三", "四", "五")

    /** 价格 → "1292.83" / "11.30"（四舍五入到分，补零到 2 位小数）。 */
    fun price(v: Double): String {
        val neg = v < 0
        val scaled = kotlin.math.abs(v) * 100.0
        val r = kotlin.math.round(scaled).toLong()
        val int = r / 100
        val frac = (r % 100).let { if (it < 10) "0$it" else "$it" }
        return "${if (neg) "-" else ""}$int.$frac"
    }

    /** 涨跌幅 → "+5.20%" / "-2.30%" / "0.00%"。 */
    fun pct(v: Double): String {
        val sign = when { v > 0 -> "+"; v < 0 -> "-"; else -> "" }
        return sign + price(kotlin.math.abs(v)) + "%"
    }

    /** 成交量（单位：手）→ "3347手" / "3.35万手" / "2.10亿手"。 */
    fun volume(hand: Long): String = when {
        hand < 0 -> "0"
        hand >= 100_000_000L -> price(hand / 1e8) + "亿手"
        hand >= 10_000L -> price(hand / 1e4) + "万手"
        else -> "${hand}手"
    }

    /** 成交额（输入：万元）→ "42.78亿" / "4278.30万" / "1200元"。 */
    fun amountFromWan(wanYuan: Double): String = when {
        wanYuan >= 10_000.0 -> price(wanYuan / 10_000.0) + "亿"
        wanYuan >= 1.0 -> price(wanYuan) + "万"
        else -> price(wanYuan * 10_000.0) + "元"
    }

    /** 五档盘口 → 行模型（买/卖各 ≤5 行，档位中文一~五）。 */
    fun depthRows(bids: List<BidAsk>, asks: List<BidAsk>): List<DepthRow> {
        val buy = bids.take(5).mapIndexed { i, x -> DepthRow(CHINESE_LEVEL[i], "买", x.price, x.volume) }
        val sell = asks.take(5).mapIndexed { i, x -> DepthRow(CHINESE_LEVEL[i], "卖", x.price, x.volume) }
        return buy + sell
    }
}