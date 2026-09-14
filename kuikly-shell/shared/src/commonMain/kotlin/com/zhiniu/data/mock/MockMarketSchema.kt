/* 知牛 · 离线兜底数据 schema（JSON：src/commonMain/data/mock_market.json，编译期生成 MockMarketData.kt）
 * 单一事实源是 JSON 数据文件；此处只定义解析模型与默认值出口。
 * 注意：解析用 JsonObject 手写遍历——ohos 构建链（build.ohos.gradle.kts）无
 * serialization 编译器插件，@Serializable + decodeFromString 会抛
 * "Serializer not found"（fresh 实例离线兜底整层失效的根因）。
 */
package com.zhiniu.data.mock

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal data class MockMarketFile(
    val stocks: List<MockStock> = emptyList(),
    val indices: List<MockIndex> = emptyList(),
    val breadth: MockBreadth = MockBreadth(),
    val defaults: MockDefaults = MockDefaults(),
)

internal data class MockStock(
    val symbol: String,
    val name: String,
    val pinyin: String = "",
    val price: Double,
    val prevClose: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val amount: Double,
)

internal data class MockIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
)

internal data class MockBreadth(
    val upCount: Int = 0,
    val downCount: Int = 0,
    val amountYi: Int = 0,
    val status: String = "",
)

internal data class MockDefaults(
    val watchlist: List<String> = emptyList(),
    val compareA: String = "",
    val compareB: String = "",
)

private fun JsonObject.str(key: String): String =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

private fun JsonObject.num(key: String): Double =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0

private fun JsonObject.int(key: String): Int = num(key).toInt()

private fun JsonObject.obj(key: String): JsonObject? =
    (this[key] as? JsonObject)

private fun JsonObject.arr(key: String): List<JsonObject> =
    (this[key] as? kotlinx.serialization.json.JsonArray)
        ?.mapNotNull { it as? JsonObject }
        ?: emptyList()

/** 兼容出口：页面/组件以对象语法读取默认值（Watchlist 默认自选、Compare 默认对比对）。 */
internal object MockMarketDefaults {
    val watchlist: List<String> get() = mockMarketFile.defaults.watchlist
    val comparePair: Pair<String, String>
        get() = Pair(mockMarketFile.defaults.compareA, mockMarketFile.defaults.compareB)
}

/** JSON 解析（懒加载；脏数据容错为空文件，页面按空池走真实网关优先的正常链路）。 */
internal val mockMarketFile: MockMarketFile by lazy {
    runCatching {
        val root = Json.parseToJsonElement(MOCK_MARKET_JSON).jsonObject
        MockMarketFile(
            stocks = root.arr("stocks").map { o ->
                MockStock(
                    symbol = o.str("symbol"),
                    name = o.str("name"),
                    pinyin = o.str("pinyin"),
                    price = o.num("price"),
                    prevClose = o.num("prevClose"),
                    open = o.num("open"),
                    high = o.num("high"),
                    low = o.num("low"),
                    volume = o.num("volume").toLong(),
                    amount = o.num("amount"),
                )
            },
            indices = root.arr("indices").map { o ->
                MockIndex(
                    symbol = o.str("symbol"),
                    name = o.str("name"),
                    price = o.num("price"),
                    changePercent = o.num("changePercent"),
                )
            },
            breadth = root.obj("breadth")?.let { o ->
                MockBreadth(
                    upCount = o.int("upCount"),
                    downCount = o.int("downCount"),
                    amountYi = o.int("amountYi"),
                    status = o.str("status"),
                )
            } ?: MockBreadth(),
            defaults = root.obj("defaults")?.let { o ->
                MockDefaults(
                    watchlist = o.arr("watchlist").mapNotNull { s ->
                        (s as? kotlinx.serialization.json.JsonPrimitive)?.content
                    },
                    compareA = o.str("compareA"),
                    compareB = o.str("compareB"),
                )
            } ?: MockDefaults(),
        )
    }.getOrElse { e ->
        // 探针：解析失败在 ohos 曾静默为空池（离线兜底整层失效），失败必须留痕
        println("[zhiniu-mock] parse fail: len=${MOCK_MARKET_JSON.length} err=${e.message}")
        MockMarketFile()
    }
}
