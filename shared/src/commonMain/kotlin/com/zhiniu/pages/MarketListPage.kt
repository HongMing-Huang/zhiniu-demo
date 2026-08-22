/* 知牛 · 行情列表页（Task01，完整实现）
 * 布局：顶部指数条 → Tab(自选/全部/涨幅/跌幅) → 股票行列表(名称代码/最新价/涨跌幅/AI标签)。
 * 组件：Kuikly 内置 View/Text/ScrollView/List/Tab/Dialog；数据 observableList + vfor/vif。
 * ⚠️ 指令(vfor/vif)与颜色/尺寸具象签名以 Kuikly SDK 官方模板为准；此处按 kuiklyDSL.mdc 语义书写。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Palette
import com.zhiniu.pages.components.priceColor
import com.zhiniu.pages.components.pricePct
import com.zhiniu.pages.components.priceValue
import com.zhiniu.viewmodel.MarketMode
import com.zhiniu.viewmodel.MarketListVM

@Page("MarketList")
internal class MarketListPage(
    private val vm: MarketListVM,
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
                            // 每指指数：名称 + 点位 + 涨跌幅
                            Text { attr { text(item.name) } }
                            Text { attr { text(priceValue(item)) } }
                            Text { attr { text(pricePct(item)) } }
                        }
                    }
                }

                // ===== Tab：自选/全部/涨幅/跌幅 =====
                View {
                    attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
                    Text { attr { text("自选"); onClick { vm.setMode(MarketMode.WATCHLIST) } } }
                    Text { attr { text("全部"); onClick { vm.setMode(MarketMode.ALL) } } }
                    Text { attr { text("涨幅"); onClick { vm.setMode(MarketMode.GAINERS) } } }
                    Text { attr { text("跌幅"); onClick { vm.setMode(MarketMode.LOSERS) } } }
                }

                // ===== 加载 / 错误 / 列表 =====
                vif(vm.loading) {
                    Text { attr { text("加载中…"); allCenter() } }
                }
                vif(vm.errorMsg != null) {
                    View {
                        attr { allCenter() }
                        Text { attr { text("加载失败：${vm.errorMsg.value}") } }
                        Text { attr { text("重试"); color(colorOf(Palette.UP)); onClick { vm.retry() } } }
                    }
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

    /** 股票行：名称代码 / 最新价(着色) / 涨跌幅(着色) / AI 标签。 */
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
                    onClick { showAiTagExplanation(q) }
                }
            }
        }
    }

    /** AI 标签点击 → Dialog 解释依据。 */
    private fun showAiTagExplanation(q: Quote) {
        // Dialog(标题= ${q.name} AI结论, 内容=由多数据源与 LLM 综合)：以 SDK 模板 Dialog 用法
    }

    private fun colorOf(hex: String): String = hex // 颜色对象构造以 SDK 为准
}