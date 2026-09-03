/* 知牛 · Mock/Real 数据源切换点：UI 只通过 MarketStore 取数，不感知实现。
 * 接真实行情/AI 时替换 repository 与 aiService 实现（见 docs/ARCHITECTURE.md）。
 */
package com.zhiniu.data.mock

import com.zhiniu.domain.repository.AiService
import com.zhiniu.domain.repository.MarketRepository

object MarketStore {
    /** 行情仓储：当前为 Mock 确定性实现。 */
    val repository: MarketRepository = MockMarketRepository()

    /** AI 服务：当前为 Mock 确定性实现。 */
    val aiService: AiService = MockAiService(repository)
}
