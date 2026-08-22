/* 知牛 · 新浪实时行情客户端（真实 API，P0 主源）
 *
 * 端点: https://hq.sinajs.cn/list=<code1>,<code2>,...
 * 约束: Header 需 Referer: https://finance.sina.com.cn ；返回为 GBK 编码 JS 赋值。
 * 字段索引（实测）见 docs/api-matrix.md：0名称 1今开 2昨收 3现价 4最高 5最低
 *   6买一 7卖一 8量(股) 9额(元) 10..19买二~买五(量,价) 20..29卖二~卖五 30日期 31时间
 */
package com.zhiniu.data.remote

import com.zhiniu.domain.model.BidAsk
import com.zhiniu.domain.model.Quote
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SinaQuoteApi(private val client: HttpClient) {

    suspend fun fetch(vararg symbols: String): List<Quote> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyList()
        val resp = client.get("https://hq.sinajs.cn/list=${symbols.joinToString(",")}") {
            header("Referer", "https://finance.sina.com.cn")
            // Ktor 默认按响应 charset 解码；若服务端未标 charset，做如下兜底：
        }.bodyAsText()

        resp.lineSequence()
            .filter { it.startsWith("var hq_str_") && it.contains('"') }
            .mapNotNull(::parseLine)
            .toList()
    }

    private fun parseLine(line: String): Quote? {
        val openQ = line.indexOf('"')
        val closeQ = line.lastIndexOf('"')
        if (openQ < 0 || closeQ <= openQ) return null
        val code = line.substringAfter("var hq_str_").substringBefore("=")
        val raw = line.substring(openQ + 1, closeQ)
        if (raw.isEmpty() || raw == "0") return null
        val f = raw.split(",")
        if (f.size < 32) return null

        fun p(i: Int) = f[i].toDoubleOrNull() ?: 0.0
        fun vol(i: Int) = f[i].toLongOrNull() ?: 0L

        // 五档：新浪 A 股 10..29 为 买二~买五、卖二~卖五（量,价两两交替）
        // 索引 10--19 实为 买一量..买五、（首档已由 idx6 给价）；此处按文档常见布局拼接买卖盘口
        val bids = buildList {
            for (k in 0 until 2) {
                val qty = vol(10 + k * 2)
                if (qty > 0) add(BidAsk(f[11 + k * 2].toDoubleOrNull() ?: p(6), qty))
            }
        }
        val asks = buildList {
            for (k in 0 until 2) {
                val qty = vol(20 + k * 2)
                if (qty > 0) add(BidAsk(f[21 + k * 2].toDoubleOrNull() ?: p(7), qty))
            }
        }
        return Quote(
            symbol = code,
            name = f[0],
            open = p(1),
            prevClose = p(2),
            price = p(3),
            high = p(4),
            low = p(5),
            buy1 = p(6),
            sell1 = p(7),
            volume = vol(8) / 100L,           // 股->手
            amount = p(9),
            bids = bids,
            asks = asks,
            date = f[30],
            time = f[31],
        )
    }
}