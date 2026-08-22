/* 知牛 · 行情列表页（Task01）
 *
 * ⚠️ 本页仅保留 Kuikly 官方文档已验证的最小 Page 形态（Pager/@Page/body():ViewBuilder）。
 * 完整 UI 布局、Index 指数条 / Tab / 股票行 / 下拉刷新 / AI 标签等见 docs/ui-design.md，
 * 其组合组件用 Kuikly 内置组件（View/Text/ScrollView/Tab/Dialog），
 * 动态列表绑定需在官方模板环境按 kuiklyDSL.mdc 的 observable/vfor 完成（避免臆造 API）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text

@Page("MarketList")
internal class MarketListPage(
    private val viewModel: com.zhiniu.viewmodel.MarketListVM,
) : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr { allCenter() }
            Text {
                attr {
                    text("行情")
                    fontSize(18f)
                }
            }
        }
    }
}