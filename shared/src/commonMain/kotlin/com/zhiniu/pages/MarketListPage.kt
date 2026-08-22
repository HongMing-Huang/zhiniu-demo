/* 知牛 · 行情列表页（Task01，完整实现）
 * 布局：顶部指数条 → Tab(自选/全部/涨幅/跌幅) → 股票行列表(名称代码/最新价/涨跌幅/AI标签)。
 * 组件：Kuikly 内置 View/Text/ScrollView/List/Tab/Dialog；数据 observableList + vfor/vif。
 * ⚠️ 指令(vfor/vif)与颜色/尺寸具象签名以 Kuikly SDK 官方模板为准；此处按 kuiklyDSL.mdc 语义书写。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text
import com.zhiniu.domain.model.AppError
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Palette
import com.zhiniu.pages.components.appErrorCard
import com.zhiniu.pages.components.colorOf
import com.zhiniu.pages.components.priceColor
import com.zhiniu.pages.components.pricePct
import com.zhiniu.pages.components.priceValue
import com.zhiniu.pages.components.skeletonRow
import com.zhiniu.viewmodel.MarketMode
import com.zhiniu.viewmodel.MarketListVM

@Page("MarketList")
internal class MarketListPage(
    private val vm: MarketListVM,
    /** 壳工程注入的路由实现：openPage(page, args)。 */
    private val navigator: (String, Map<String, String>) -> Unit = { _, _ -> },
) : Pager() {

    override fun body(): ViewBuilder {
        return {
            View {
                attr { flex(1f); flexDirection(FLEX_DIRECTION_COLUMN) }
                // ===== 顶部指数条 =====
                ScrollView {
                    attr {
                        flexGrow(0f); height(44f); flexDirection(FLEX_DIRECTION_ROW)
                        vfor(vm.indices) { item ->
                            Text { attr { text(item.name) } }
                            Text { attr { text(priceValue(item)) } }
                            Text { attr { text(pricePct(item)) } }
                        }
                    }
                }

                // ===== Tab：自选/全部/涨幅/跌幅（内置 Tab，以 SDK 模板为准）=====
                Tab {
                    attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
                    Text { attr { text("自选"); color(colorOf(if (vm.mode.value == MarketMode.WATCHLIST) Palette.UP else Palette.SUB)); onClick { vm.setMode(MarketMode.WATCHLIST) } } }
                    Text { attr { text("全部"); color(colorOf(if (vm.mode.value == MarketMode.ALL) Palette.UP else Palette.SUB)); onClick { vm.setMode(MarketMode.ALL) } } }
                    Text { attr { text("涨幅"); color(colorOf(if (vm.mode.value == MarketMode.GAINERS) Palette.UP else Palette.SUB)); onClick { vm.setMode(MarketMode.GAINERS) } } }
                    Text { attr { text("跌幅"); color(colorOf(if (vm.mode.value == MarketMode.LOSERS) Palette.UP else Palette.SUB)); onClick { vm.setMode(MarketMode.LOSERS) } } }
                }

                // ===== 加载 / 错误 / 列表 =====
                vif(vm.loading) { skeletonRow() }
                vif(vm.errorMsg != null) {
                    appErrorCard(AppError.Unknown(vm.errorMsg.value)) { vm.retry() }
                }
                List {
                    attr {
                        flex(1f)
                        vfor(vm.quotes) { q ->
                            quoteRow(q)
                        }
                    }
                }
            }
        }
    }

    /** 股票行：名称代码 / 最新价 / 涨跌幅 / AI 标签；单击进入详情。 */
    private fun quoteRow(q: Quote): ViewBuilder = {
        View {
            attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(1f) }
            Text { attr { text("${q.name} ${q.symbol}") } }
            Text { attr { text(priceValue(q)); color(colorOf(priceColor(q))) } }
            Text { attr { text(pricePct(q)); color(colorOf(priceColor(q))) } }
            Text {
                attr {
                    text("AI")
                    color(colorOf(Palette.UP))
                    onClick { navigator("StockDetail", mapOf("symbol" to q.symbol, "name" to q.name)) }
                }
            }
        }
    }
}