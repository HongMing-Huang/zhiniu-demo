package com.zhiniu.data.remote

import com.zhiniu.domain.model.Candle
import com.zhiniu.domain.model.FundFlowSnapshot
import com.zhiniu.domain.model.MarketIndex
import com.zhiniu.domain.model.OrderBookLevel
import com.zhiniu.domain.model.SseParser
import com.zhiniu.domain.model.StockFundamentals
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.platform.GatewayTransport
import com.zhiniu.platform.createPlatformGatewayTransport
import com.zhiniu.platform.runOffMainThread
import com.zhiniu.platform.sseStreamingSupported
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class MarketNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val publishedAt: String,
    val source: String,
    val provider: String,
    val isStale: Boolean,
)

data class AgentStage(val label: String, val source: String, val status: String)

/** 通用问答结果（/agent/chat）：isLlm=false 为后端显式规则降级。 */
data class AgentChatResult(
    val content: String,
    val isLlm: Boolean,
    val provider: String,
)

/** 东财行业板块行（/quote/sectors）。rank 由页面按涨跌幅排序后赋值。 */
data class SectorRow(
    val code: String,
    val name: String,
    val changePercent: Double,
    val upCount: Int,
    val downCount: Int,
    val leadStock: String,
    val leadSymbol: String,
    val leadChangePercent: Double,
    val rank: Int = 0,
) {
    val isUp: Boolean get() = changePercent >= 0
}

data class SectorResult(val sectors: List<SectorRow>, val source: String, val isStale: Boolean)

/** 研究流阶段事件（/agent/research/stream 类型化 SSE：stage_started / stage_completed）。 */
data class ResearchStageEvent(val type: String, val stage: String, val label: String, val source: String)

/** 人气榜条目（东财排名 + 新浪实时增强；行情缺失时 price 为 0）。 */
data class PopularStock(
    val rank: Int,
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
)

data class PopularityResult(val stocks: List<PopularStock>, val source: String, val isStale: Boolean)

data class CandleSeriesResult(
    val bars: List<Candle>,
    val source: String,
    val provider: String,
    val isStale: Boolean,
)

data class AgentResearchResult(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val direction: String,
    val rangeChangePercent: Double,
    val rsi14: Double,
    val ma20: Double,
    val newsCount: Int,
    val newsSource: String,
    val pe: Double?,
    val pb: Double?,
    val marketCap: Double?,
    val reportDate: String,
    val financialSource: String,
    val riskFlags: List<String>,
    val stages: List<AgentStage>,
    val summary: String,
    val stance: String,
    val confidence: Double,
    val synthesisMode: String,
    val synthesisProvider: String,
    // TradingAgents 辩论结构化输出（课题：趋势 / 压力位 / 支撑位 / 投资建议）
    val rating: String = "",
    val trend: String = "",
    val pressure: Double? = null,
    val support: Double? = null,
    val bullPoints: List<String> = emptyList(),
    val bearPoints: List<String> = emptyList(),
    val riskNotes: List<String> = emptyList(),
    val traderPlan: String = "",
    val traderEntry: Double? = null,
    val traderStop: Double? = null,
    // 结构化洞察（服务端确定性推导：风险五档 + 买卖观察区间 + 迷你K线数据）
    val riskLevel: String = "",
    val riskLevelLabel: String = "",
    val riskRationale: String = "",
    val adviceAvailable: Boolean = false,
    val adviceBuyRange: String = "",
    val adviceSellRange: String = "",
    val adviceRationale: String = "",
    val adviceBasis: List<String> = emptyList(),
    val klineCloses: List<Double> = emptyList(),
)

data class GatewayStatus(
    val online: Boolean = false,
    val marketLabel: String = "连接中",
    val agentLabel: String = "检查中",
    val agentReady: Boolean = false,
)

/** H5/原生共用的行情网关客户端；失败由页面保留本地快照，不让网络抖动破坏主流程。 */
object GatewayMarketClient {
    const val DEFAULT_BASE_URL = "http://127.0.0.1:8000"

    /** 网关地址：平台默认（Android 模拟器 10.0.2.2）→ H5 可通过 URL 参数 gateway= 覆盖（真机联调指向电脑 IP）。 */
    var baseUrl: String = com.zhiniu.platform.platformDefaultGateway() ?: DEFAULT_BASE_URL
        set(value) {
            val trimmed = value.trim().trimEnd('/')
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) field = trimmed
        }
    /** H5/原生共用的行情网关客户端；失败由页面保留本地快照，不让网络抖动破坏主流程。
     * 传输经 GatewayTransport 平台抽象（Ktor js/OkHttp/Darwin；ohos 为 napi 桥预留位）。 */
    private val transport: GatewayTransport by lazy { createPlatformGatewayTransport() }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun quotes(symbols: List<String>): List<StockQuote> {
        if (symbols.isEmpty()) return emptyList()
        val text = runOffMainThread { transport.get("$baseUrl/quote/realtime?codes=${symbols.joinToString(",")}") }
        return parseQuotes(text, symbols)
    }

    suspend fun candles(symbol: String, scale: Int = 240, count: Int = 240): List<Candle> {
        val text = runOffMainThread { transport.get("$baseUrl/quote/kline?symbol=$symbol&scale=$scale&datalen=$count") }
        return parseCandles(text)
    }

    suspend fun candleSeries(symbol: String, scale: Int = 240, count: Int = 240): CandleSeriesResult {
        val text = runOffMainThread { transport.get("$baseUrl/quote/kline?symbol=$symbol&scale=$scale&datalen=$count") }
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: JsonObject(emptyMap())
        return CandleSeriesResult(
            bars = parseCandles(text),
            source = root.string("source"),
            provider = root.string("provider"),
            isStale = root.boolean("isStale"),
        )
    }

    suspend fun fundamentals(symbol: String): StockFundamentals? {
        val text = runOffMainThread { transport.get("$baseUrl/quote/fundamentals?symbol=$symbol") }
        return parseFundamentals(text)
    }

    suspend fun news(symbol: String): List<MarketNewsItem> {
        val text = runOffMainThread { transport.get("$baseUrl/news/list?symbol=$symbol") }
        return parseNews(text)
    }

    /** 东财人气榜（真实排名）；后端离线时返回 source=mock + isStale。 */
    suspend fun popularity(count: Int = 20): PopularityResult {
        val text = runOffMainThread { transport.get("$baseUrl/quote/popularity?count=$count") }
        return parsePopularity(text)
    }

    /** 新浪三大指数实时行情（/quote/indices）；网关不可用返回 null（页面回退本地快照）。 */
    suspend fun indices(): List<MarketIndex>? {
        val text = runCatching {
            runOffMainThread { transport.get("$baseUrl/quote/indices") }
        }.getOrNull() ?: return null
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val items = root["indices"]?.jsonArray?.mapNotNull { element ->
            val o = element.jsonObject
            val name = o.string("name")
            if (name.isBlank()) return@mapNotNull null
            val price = o.double("price")
            val prevClose = o.double("prevClose")
            val changePct = if (prevClose > 0.0) (price - prevClose) / prevClose * 100.0 else 0.0
            MarketIndex(
                symbol = o.string("symbol"),
                name = name,
                price = price,
                changePercent = kotlin.math.round(changePct * 100) / 100.0,
            )
        } ?: emptyList()
        return items.ifEmpty { null }
    }

    /** 东财行业板块（按当日涨跌幅排序）；网关不可用返回 null（页面显示空态提示，不伪造板块）。 */
    suspend fun sectors(): SectorResult? {
        val text = runCatching {
            runOffMainThread { transport.get("$baseUrl/quote/sectors") }
        }.getOrNull() ?: return null
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val sectors = root["sectors"]?.jsonArray?.mapNotNull { element ->
            val o = element.jsonObject
            val name = o.string("name")
            if (name.isBlank()) return@mapNotNull null
            SectorRow(
                code = o.string("code"),
                name = name,
                changePercent = o.double("changePercent"),
                upCount = o.double("upCount").toInt(),
                downCount = o.double("downCount").toInt(),
                leadStock = o.string("leadStock"),
                leadSymbol = o.string("leadSymbol"),
                leadChangePercent = o.double("leadChangePercent"),
            )
        } ?: emptyList()
        if (sectors.isEmpty()) return null
        return SectorResult(sectors, root.string("source"), root.boolean("isStale"))
    }

    suspend fun research(symbol: String, keyword: String): AgentResearchResult? {
        val text = runOffMainThread { transport.postJson("$baseUrl/agent/research", researchPayload(symbol, keyword)) }
        return parseResearch(text)
    }

    /**
     * 类型化研究流：逐帧回调阶段事件（行情/技术面/财务/资讯/风险/多头/空头/研究经理/交易员/风控/归纳），
     * 在 result 帧解析完整报告返回；run_error 或流中断返回 null，由调用方回退到 research()。
     */
    suspend fun researchStream(
        symbol: String,
        keyword: String,
        onStage: (ResearchStageEvent) -> Unit,
    ): AgentResearchResult? {
        if (!sseStreamingSupported) return null // iOS Debug 断言下关闭流式，调用方回退一次性接口
        var result: AgentResearchResult? = null
        var pendingEvent: String? = null  // SSE event: 行先于 data: 行到达时挂起（lambda 闭包捕获）
        transport.postSse("$baseUrl/agent/research/stream", researchPayload(symbol, keyword)) { line ->
            val frame = SseParser.parseLine(line) ?: return@postSse
            if (frame.event != null) {
                pendingEvent = frame.event
                return@postSse
            }
            val data = frame.data ?: return@postSse
            val payload = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return@postSse
            val type = payload.string("type").ifBlank { pendingEvent.orEmpty() }
            pendingEvent = null
            when (type) {
                "stage_started", "stage_completed" -> onStage(
                    ResearchStageEvent(type, payload.string("stage"), payload.string("label"), payload.string("source"))
                )
                "result" -> result = payload["result"]?.jsonObject?.let { parseResearchObject(it) }
                "run_error" -> return@postSse
                "run_finished" -> return@postSse
            }
        }
        return result
    }

    private fun researchPayload(symbol: String, keyword: String): String = buildJsonObject {
        put("symbol", symbol)
        put("keyword", keyword.take(80))
    }.toString()

    /**
     * 通用问答（快捷指令/概念解释等非个股研究问题）。
     * 返回 null 表示网关不可用（调用方回退本地 Mock 回复，保证离线可演示）；
     * isLlm=false 为后端显式规则降级（内容可信但非模型输出）。
     */
    suspend fun agentChat(
        question: String,
        history: List<Pair<String, String>> = emptyList(),
        symbol: String = "",
    ): AgentChatResult? {
        val payload = buildJsonObject {
            put("question", question.take(300))
            if (history.isNotEmpty()) {
                put("history", buildJsonArray {
                    history.takeLast(8).forEach { (role, content) ->
                        add(buildJsonObject { put("role", role); put("content", content.take(500)) })
                    }
                })
            }
            if (symbol.isNotBlank()) put("symbol", symbol)
        }
        val text = runCatching {
            runOffMainThread { transport.postJson("$baseUrl/agent/chat", payload.toString()) }
        }.getOrNull() ?: return null
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val content = root.string("content")
        if (content.isBlank()) return null
        return AgentChatResult(
            content = content,
            isLlm = root.string("mode") == "llm",
            provider = root.string("provider"),
        )
    }

    suspend fun health(): GatewayStatus {
        val text = runOffMainThread { transport.get("$baseUrl/healthz") }
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return GatewayStatus()
        val services = root["services"]?.jsonObject ?: JsonObject(emptyMap())
        val market = services["market"]?.jsonObject ?: JsonObject(emptyMap())
        val agent = services["agent"]?.jsonObject ?: JsonObject(emptyMap())
        val ready = agent.string("status") == "ready"
        return GatewayStatus(
            online = root.string("status") == "ok",
            marketLabel = market.string("provider").ifBlank { "行情网关" },
            agentLabel = if (ready) "模型已配置" else "规则降级",
            agentReady = ready,
        )
    }

    internal fun parseQuotes(text: String, order: List<String>): List<StockQuote> {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return emptyList()
        return order.mapNotNull { symbol -> root[symbol]?.jsonObject?.toQuote(symbol) }
    }

    internal fun parseCandles(text: String): List<Candle> {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return emptyList()
        return root["data"]?.jsonArray?.mapNotNull { item ->
            val o = item.jsonObject
            Candle(
                day = o.string("day"),
                open = o.double("open"), high = o.double("high"),
                low = o.double("low"), close = o.double("close"),
                volume = (o.double("volume") / 100.0).toLong().coerceAtLeast(0L),
            ).takeIf { it.open > 0.0 && it.high >= maxOf(it.open, it.close) && it.low <= minOf(it.open, it.close) }
        } ?: emptyList()
    }

    internal fun parseNews(text: String): List<MarketNewsItem> {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return emptyList()
        return root["items"]?.jsonArray?.mapNotNull { element ->
            val item = element.jsonObject
            val title = item.string("title")
            if (title.isBlank()) return@mapNotNull null
            MarketNewsItem(
                id = item.string("id"),
                title = title,
                summary = item.string("content"),
                publishedAt = item.string("publishedAt").ifBlank { item.string("time") },
                source = item.string("source"),
                provider = item.string("provider"),
                isStale = item.string("isStale") == "true",
            )
        } ?: emptyList()
    }

    internal fun parsePopularity(text: String): PopularityResult {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return PopularityResult(emptyList(), "", true)
        val stocks = root["stocks"]?.jsonArray?.mapNotNull { element ->
            val item = element.jsonObject
            val symbol = item.string("symbol")
            if (symbol.length != 8) return@mapNotNull null
            PopularStock(
                rank = item.long("rank").toInt(),
                symbol = symbol,
                name = item.string("name").ifBlank { symbol.drop(2) },
                price = item.double("price"),
                changePercent = item.double("changePercent"),
            )
        } ?: emptyList()
        return PopularityResult(stocks, root.string("source"), root.boolean("isStale"))
    }

    internal fun parseResearch(text: String): AgentResearchResult? {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        return parseResearchObject(root)
    }

    internal fun parseResearchObject(root: JsonObject): AgentResearchResult? {
        if (root.string("status") != "complete") return null
        val evidence = root["evidence"]?.jsonObject ?: return null
        val quote = evidence["quote"]?.jsonObject ?: JsonObject(emptyMap())
        val technical = evidence["technical"]?.jsonObject ?: JsonObject(emptyMap())
        val financials = evidence["financials"]?.jsonObject ?: JsonObject(emptyMap())
        val news = evidence["news"]?.jsonObject ?: JsonObject(emptyMap())
        val levels = evidence["levels"]?.jsonObject ?: JsonObject(emptyMap())
        val synthesis = root["synthesis"]?.jsonObject ?: JsonObject(emptyMap())
        val trader = synthesis["trader"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())
        val insight = root["insight"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())
        val riskLevel = insight["riskLevel"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())
        val advice = insight["advice"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())
        val prev = quote.double("prevClose")
        val price = quote.double("price")
        return AgentResearchResult(
            symbol = root.string("symbol"),
            name = quote.string("name"),
            price = price,
            changePercent = if (prev > 0.0) (price - prev) / prev * 100.0 else 0.0,
            direction = technical.string("direction"),
            rangeChangePercent = technical.double("changePct"),
            rsi14 = technical.double("rsi14"),
            ma20 = technical.double("ma20"),
            newsCount = news["total"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0,
            newsSource = news.string("source"),
            pe = financials.nullableDouble("pe"),
            pb = financials.nullableDouble("pb"),
            marketCap = financials.nullableDouble("marketCap") ?: quote.nullableDouble("marketCap"),
            reportDate = financials.string("reportDate"),
            financialSource = financials.string("source"),
            riskFlags = evidence["riskFlags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            stages = root["stages"]?.jsonArray?.map { stage ->
                val item = stage.jsonObject
                AgentStage(item.string("label"), item.string("source"), item.string("status"))
            } ?: emptyList(),
            summary = synthesis.string("summary"),
            stance = synthesis.string("stance"),
            confidence = synthesis.double("confidence"),
            synthesisMode = synthesis.string("mode"),
            synthesisProvider = synthesis.string("provider"),
            rating = synthesis.string("rating"),
            trend = synthesis.string("trend"),
            pressure = synthesis.nullableDouble("pressure") ?: levels.nullableDouble("pressure"),
            support = synthesis.nullableDouble("support") ?: levels.nullableDouble("support"),
            bullPoints = synthesis.stringList("bullPoints"),
            bearPoints = synthesis.stringList("bearPoints"),
            riskNotes = synthesis.stringList("riskNotes"),
            traderPlan = trader.string("plan"),
            traderEntry = trader.nullableDouble("entry"),
            traderStop = trader.nullableDouble("stop"),
            riskLevel = riskLevel.string("level"),
            riskLevelLabel = riskLevel.string("label"),
            riskRationale = riskLevel.string("rationale"),
            adviceAvailable = advice.boolean("available"),
            adviceBuyRange = advice.string("buyRange"),
            adviceSellRange = advice.string("sellRange"),
            adviceRationale = advice.string("rationale"),
            adviceBasis = advice.stringList("basis"),
            klineCloses = insight["klineCloses"]?.jsonArray?.map { it.jsonPrimitive.doubleOrNull ?: 0.0 } ?: emptyList(),
        )
    }

    private fun JsonObject.toQuote(symbol: String): StockQuote? {
        val price = double("price")
        val prevClose = double("prevClose")
        if (price <= 0.0 || prevClose <= 0.0) return null
        return StockQuote(
            symbol = symbol, name = string("name"),
            open = double("open"), prevClose = prevClose, price = price,
            high = double("high"), low = double("low"),
            buy1 = double("buy1"), sell1 = double("sell1"),
            bids = orderBook("bids"), asks = orderBook("asks"),
            volume = long("volume"), amount = double("amount"),
            marketCap = nullableDouble("marketCap"),
            turnoverRate = nullableDouble("turnoverRate"),
            volumeRatio = nullableDouble("volumeRatio"),
            date = string("date"), time = string("time"),
            source = string("source").ifBlank { "知牛离线快照" },
            provider = string("provider").ifBlank { "offline-snapshot" },
            isStale = boolean("isStale"),
        )
    }

    internal fun parseFundamentals(text: String): StockFundamentals? {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        if (!root.boolean("available")) return null
        val flow = root["moneyFlow"]?.jsonObject ?: JsonObject(emptyMap())
        return StockFundamentals(
            symbol = root.string("symbol"),
            name = root.string("name"),
            industry = root.string("industry"),
            region = root.string("region"),
            concepts = root["concepts"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            pe = root.nullableDouble("pe"),
            pb = root.nullableDouble("pb"),
            turnoverRate = root.nullableDouble("turnoverRate"),
            amplitude = root.nullableDouble("amplitude"),
            marketCap = root.nullableDouble("marketCap"),
            floatMarketCap = root.nullableDouble("floatMarketCap"),
            reportDate = root.string("reportDate"),
            reportType = root.string("reportType"),
            revenue = root.nullableDouble("revenue"),
            netProfit = root.nullableDouble("netProfit"),
            roe = root.nullableDouble("roe"),
            grossMargin = root.nullableDouble("grossMargin"),
            revenueYoY = root.nullableDouble("revenueYoY"),
            netProfitYoY = root.nullableDouble("netProfitYoY"),
            moneyFlow = FundFlowSnapshot(
                asOf = flow.string("asOf"),
                mainNetInflow = flow.nullableDouble("mainNetInflow"),
                smallNetInflow = flow.nullableDouble("smallNetInflow"),
                mediumNetInflow = flow.nullableDouble("mediumNetInflow"),
                largeNetInflow = flow.nullableDouble("largeNetInflow"),
                superLargeNetInflow = flow.nullableDouble("superLargeNetInflow"),
            ),
            source = root.string("source"),
            provider = root.string("provider"),
            isStale = root.boolean("isStale"),
            available = true,
        )
    }

    private fun JsonObject.orderBook(key: String): List<OrderBookLevel> =
        this[key]?.jsonArray?.mapNotNull { row ->
            val values = row.jsonArray
            val price = values.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val shares = values.getOrNull(1)?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L
            if (price > 0.0) OrderBookLevel(price, shares) else null
        } ?: emptyList()

    private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.content ?: ""
    private fun JsonObject.stringList(key: String): List<String> =
        this[key]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull()?.takeIf(String::isNotBlank) }
            ?: emptyList()
    private fun JsonObject.double(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
    private fun JsonObject.nullableDouble(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.content == "true"
    private fun JsonObject.long(key: String): Long = this[key]?.jsonPrimitive?.longOrNull
        ?: this[key]?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L
}
