/* 知牛 · 内存自选（演示用：跨页面共享，非响应式；页面以自身 observable 驱动重绘） */
package com.zhiniu.data.local

object Watchlist {
    private val symbols = mutableSetOf("sh600519", "sz300750")

    fun contains(symbol: String): Boolean = symbol in symbols

    fun toggle(symbol: String) {
        if (!symbols.add(symbol)) symbols.remove(symbol)
    }
}
