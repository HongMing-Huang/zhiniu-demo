/* 知牛 · 新浪 K 线客户端（真实 API，P0）
 * 端点: money.finance.sina.com.cn/.../CN_MarketData.getKLineData?symbol=sh600519&scale=240&ma=no&datalen=180
 * 返回: JSON 数组 [ {day,open,high,low,close,volume}, ... ]
 */
package com.zhiniu.data.remote

import com.zhiniu.domain.model.KLineBar
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

class SinaKlineApi(private val client: HttpClient) {

    suspend fun fetch(symbol: String, scale: Int = 240, datalen: Int = 180): List<KLineBar> =
        withContext(Dispatchers.Default) {
            val url = "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/" +
                "CN_MarketData.getKLineData?symbol=$symbol&scale=$scale&ma=no&datalen=$datalen"
            val text = client.get(url) {
                header("Referer", "https://finance.sina.com.cn")
            }.bodyAsText()
            parse(text)
        }

    private fun parse(text: String): List<KLineBar> {
        val arr = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonArray
            ?: return emptyList()
        return arr.mapNotNull(::toBar)
    }

    private fun toBar(el: JsonElement): KLineBar? {
        val obj = el as? kotlinx.serialization.json.JsonObject ?: return null
        fun d(key: String): Double = (obj[key]?.let {
            if (it is JsonNull) null else it.toString().toDoubleOrNull()
        }) ?: 0.0
        val day = obj["day"]?.toString()?.trim('"') ?: ""
        return KLineBar(day, d("open"), d("high"), d("low"), d("close"), d("volume").toLong())
    }
}