/* 知牛 · DI（Koin）· AppModule */
package com.zhiniu.di

import com.zhiniu.data.local.MemoryWatchlistStore
import com.zhiniu.data.local.WatchlistStore
import com.zhiniu.data.remote.LlmGatewayClient
import com.zhiniu.data.remote.SinaKlineApi
import com.zhiniu.data.remote.SinaQuoteApi
import com.zhiniu.domain.repository.ChatRepository
import com.zhiniu.domain.repository.LlmChatRepository
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.domain.repository.SinaMarketRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * 应用级依赖。engine 由各端壳注入（android 用 CI.Android、ios 用 Darwin、h5 用 JS），
 * gatewayBaseUrl 指向自建 LLM 网关（见 backend/）。
 */
fun appModule(
    engine: HttpClientEngine,
    gatewayBaseUrl: String = "http://localhost:8000",
    scopeProvider: () -> CoroutineScope = { CoroutineScope(kotlinx.coroutines.SupervisorJob()) },
) = module {
    single { HttpClient(engine) }

    single<WatchlistStore> { MemoryWatchlistStore() }
    single { SinaQuoteApi(get()) }
    single { SinaKlineApi(get()) }
    single { LlmGatewayClient(get(), gatewayBaseUrl) }

    single<MarketRepository> { SinaMarketRepository(get(), get(), get()) }
    single<ChatRepository> { LlmChatRepository(get()) }
}