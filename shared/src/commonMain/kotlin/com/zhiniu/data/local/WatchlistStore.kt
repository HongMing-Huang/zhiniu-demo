/* 知牛 · 本地自选存储（接口 + 内存实现；后续可替换为 SQLDelight，见 ADR-005）*/
package com.zhiniu.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface WatchlistStore {
    fun observe(): Flow<Set<String>>
    suspend fun add(symbol: String)
    suspend fun remove(symbol: String)
}

class MemoryWatchlistStore : WatchlistStore {
    private val _items = MutableStateFlow(
        setOf("sh600519", "sz000001", "sh600036", "sz300750", "sh601318")
    )
    override fun observe(): Flow<Set<String>> = _items
    override suspend fun add(symbol: String) { _items.value = _items.value + symbol }
    override suspend fun remove(symbol: String) { _items.value = _items.value - symbol }
}