/* 知牛 · 自选（内存态 + 页面层 SharedPreferences 持久化双写）。
 * 跨页面共享，非响应式；页面以自身 observable 驱动重绘。
 * 持久化：页面在增删后调用 serialize() 写 SharedPreferencesModule，
 * 启动/进页时 restore() 恢复（三端 Kuikly 官方 SharedPreferencesModule 落地）。
 */
package com.zhiniu.data.local

object Watchlist {
    private val symbols = linkedSetOf("sh600519", "sz300750")

    fun symbols(): List<String> = symbols.toList()

    fun contains(symbol: String): Boolean = symbol in symbols

    fun toggle(symbol: String) {
        if (!symbols.add(symbol)) symbols.remove(symbol)
    }

    fun add(symbol: String) {
        symbols.add(symbol)
    }

    fun remove(symbol: String) {
        symbols.remove(symbol)
    }

    fun restore(list: List<String>) {
        symbols.clear()
        symbols.addAll(list.filter { it.isNotBlank() })
    }

    fun serialize(): String = symbols.joinToString(",")
}
