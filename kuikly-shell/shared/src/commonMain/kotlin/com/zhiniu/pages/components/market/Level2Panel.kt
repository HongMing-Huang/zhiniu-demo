// 知牛 · 五档盘口（Level2Panel：卖五→买五 + 中间最新价）
// 对齐股票软件（同花顺/富途）经典盘口：卖盘绿、买盘红、量右对齐 + 相对量条。
// 数据为确定性模拟（基于 symbol 种子），接入真实行情后由 Repository 替换。
package com.zhiniu.pages.components.market

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtChangeSigned
import com.zhiniu.pages.components.fmtPct

/** 五档盘口：卖五~卖一（上）｜最新价（中）｜买一~买五（下）。 */
fun ViewContainer<*, *>.Level2Panel(q: StockQuote) {
    val colors = AppTheme.colors
    val tick = 0.01
    val sellAnchor = if (q.sell1 > 0.0) q.sell1 else q.price + tick
    val buyAnchor = if (q.buy1 > 0.0) q.buy1 else q.price - tick
    // 量：确定性模拟（symbol 种子），档位数越大挂单越少
    val sellVols = (0 until 5).map { seededVol(q.symbol, 100 + it, it) }
    val buyVols = (0 until 5).map { seededVol(q.symbol, 200 + it, it) }
    val maxVol = (sellVols + buyVols).maxOrNull()?.toFloat() ?: 1f
    View {
        attr {
            flexDirectionColumn()
            backgroundColor(colors.c(colors.surface))
            borderRadius(AppRadius.radius6)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 卖盘（卖五 → 卖一）
        for (i in 0 until 5) {
            L2Row(
                price = sellAnchor + (4 - i) * tick,
                volHand = sellVols[i],
                ratio = sellVols[i].toFloat() / maxVol,
                priceHex = colors.down,
                barHex = colors.down,
            )
        }
        // 中间：最新价
        View {
            attr {
                height(30f); flexDirectionRow(); alignItemsCenter()
                padding(left = 10f, right = 10f)
                backgroundColor(colors.c(colors.surfaceSecondary))
            }
            Text {
                attr {
                    fontSize(AppTypography.fs16); fontWeightSemiBold(); fontFamily(NUM_FONT)
                    color(colors.c(if (q.isUp) colors.up else colors.down))
                    text(fmt2(q.price))
                }
            }
            View { attr { width(10f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12); fontFamily(NUM_FONT)
                    color(colors.c(if (q.isUp) colors.up else colors.down))
                    text(fmtChangeSigned(q.change) + "  " + fmtPct(q.changePercent))
                }
            }
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs11)
                    color(colors.c(colors.textTertiary))
                    text("最新 · " + q.time.ifBlank { "--:--" })
                }
            }
        }
        // 买盘（买一 → 买五）
        for (i in 0 until 5) {
            L2Row(
                price = buyAnchor - i * tick,
                volHand = buyVols[i],
                ratio = buyVols[i].toFloat() / maxVol,
                priceHex = colors.up,
                barHex = colors.up,
            )
        }
    }
}

/** 单档行：价格｜量（右对齐）+ 半透明量条（从行左填充比例）。 */
private fun ViewContainer<*, *>.L2Row(
    price: Double,
    volHand: Long,
    ratio: Float,
    priceHex: String,
    barHex: String,
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(20f); flexDirectionRow(); alignItemsCenter()
            padding(left = 10f, right = 10f)
            overflow(true)
        }
        // 量条（置于底色，左起按比例）
        View {
            attr {
                absolutePosition(top = 4f, left = 0f, bottom = 4f)
                width(96f)
                backgroundColor(colors.ca(barHex, 14))
                opacity((ratio).coerceIn(0.15f, 1f))
            }
        }
        Text {
            attr {
                flex(1f)
                fontSize(AppTypography.fs11); fontFamily(NUM_FONT)
                color(colors.c(priceHex))
                text(fmt2(price))
            }
        }
        Text {
            attr {
                fontSize(AppTypography.fs11); fontFamily(NUM_FONT)
                color(colors.c(colors.textSecondary))
                text(volHand.toString() + "手")
            }
        }
    }
}

/** 确定性模拟挂单量（手）：同 symbol 同档位恒定。 */
internal fun seededVol(symbol: String, salt: Int, idx: Int): Long {
    val h = (symbol.hashCode() * 31 + salt * 17 + idx * 131) and 0x7fffffff
    return ((h % 90L) + 5L) * 100L
}