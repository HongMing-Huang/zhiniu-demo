/* 知牛 · 离线兜底数据 schema（JSON：src/commonMain/data/mock_market.json，编译期生成 MockMarketData.kt）
 * 单一事实源是 JSON 数据文件；此处只定义解析模型与默认值出口。
 */
package com.zhiniu.data.mock

import kotlinx.serialization.Serializable

@Serializable
internal data class MockMarketFile(
    val stocks: List<MockStock> = emptyList(),
    val indices: List<MockIndex> = emptyList(),
    val breadth: MockBreadth = MockBreadth(),
    val defaults: MockDefaults = MockDefaults(),
)

@Serializable
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

@Serializable
internal data class MockIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
)

@Serializable
internal data class MockBreadth(
    val upCount: Int = 0,
    val downCount: Int = 0,
    val amountYi: Int = 0,
    val status: String = "",
)

@Serializable
internal data class MockDefaults(
    val watchlist: List<String> = emptyList(),
    val compareA: String = "",
    val compareB: String = "",
)

/** JSON 解析（懒加载；脏数据容错为空文件，页面按空池走真实网关优先的正常链路）。 */
internal val mockMarketFile: MockMarketFile by lazy {
    runCatching {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<MockMarketFile>(MOCK_MARKET_JSON)
    }.getOrDefault(MockMarketFile())
}

/** 离线兜底默认值出口：默认自选 / 默认对比对（Watchlist、ComparePage、ProfilePage 共用，不再硬编码标的）。 */
object MockMarketDefaults {
    val watchlist: List<String> get() = mockMarketFile.defaults.watchlist
    val comparePair: Pair<String, String>
        get() = mockMarketFile.defaults.compareA to mockMarketFile.defaults.compareB
}
