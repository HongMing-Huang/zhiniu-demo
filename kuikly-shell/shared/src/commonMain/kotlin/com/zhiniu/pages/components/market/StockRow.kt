// 知牛 · StockTable + StockRow（首页视觉中心；列按 columns 集合可配置）
// 无外层 Card；行间 1px 分割线；hover surfaceHover 120ms；pressed surfaceSecondary。
package com.zhiniu.pages.components.market

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.data.remote.SectorRow
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.drawSparkLine
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtAmount
import com.zhiniu.pages.components.fmtChangeSigned
import com.zhiniu.pages.components.fmtMarketCap
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.fmtVolHand

/** 可选列（课题：自定义字段选择）；股票 / 最新价 / 涨跌幅为固定列。 */
object StockColumns {
    const val CHANGE = "涨跌额"
    const val MARKET_CAP = "总市值"
    const val VOLUME = "成交量"
    const val SPARK = "走势"
    const val HIGH_LOW = "高低"
    const val AMOUNT = "成交额"
    val ALL = listOf(CHANGE, MARKET_CAP, VOLUME, SPARK, HIGH_LOW, AMOUNT)
    val DEFAULT = setOf(CHANGE, MARKET_CAP, VOLUME, SPARK, AMOUNT)
}

/**
 * 行情表：表头 + 股票行（vfor）。
 * 桌面列：股票 flex(2.4) | 最新价 120 | 涨跌额 110 | 涨跌幅 110 | 总市值 110 | 成交量 110 | 走势 170 | 高/低 150 | 成交额 110。
 * compact（≤760）：股票（含市值/成交量副行）| 最新价 78 | 涨跌幅 68 | 走势 82。
 */
fun ViewContainer<*, *>.StockTable(
    marketQuotes: () -> ObservableList<StockQuote>,
    sparkOf: (StockQuote) -> List<Double>,
    narrow: Boolean,
    compact: Boolean = false,
    columns: Set<String> = StockColumns.DEFAULT,
    onRowClick: (StockQuote) -> Unit,
) {
    StockTableHeader(narrow, compact, columns)
    vfor({ marketQuotes() }) { q ->
        StockRow(q, sparkOf(q), narrow, compact, columns) { onRowClick(q) }
    }
}

/** 表头：42px / fs12 / textTertiary；点击表头排序由调用方通过外部 SortToolbar 控制。 */
fun ViewContainer<*, *>.StockTableHeader(
    narrow: Boolean,
    compact: Boolean = false,
    columns: Set<String> = StockColumns.DEFAULT,
) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(42f) }
        View { attr { flex(2.4f) }; ThLabel("股票", alignLeft = true) }
        View { attr { width(if (compact) 78f else 120f) }; ThLabel("最新价", alignLeft = false) }
        if (!compact && StockColumns.CHANGE in columns) View { attr { width(110f) }; ThLabel("涨跌额", alignLeft = false) }
        View { attr { width(if (compact) 84f else 110f) }; ThLabel("涨跌幅", alignLeft = false) }
        if (!compact && StockColumns.MARKET_CAP in columns) View { attr { width(110f) }; ThLabel("总市值", alignLeft = false) }
        if (!compact && StockColumns.VOLUME in columns) View { attr { width(110f) }; ThLabel("成交量", alignLeft = false) }
        if (!compact && StockColumns.SPARK in columns) View { attr { width(if (compact) 82f else 170f) }; ThLabel("走势", alignLeft = false) }
        if (!narrow && StockColumns.HIGH_LOW in columns) View { attr { width(150f) }; ThLabel("高 / 低", alignLeft = false) }
        if (!narrow && StockColumns.AMOUNT in columns) View { attr { width(110f) }; ThLabel("成交额", alignLeft = false) }
    }
    View {
        attr {
            height(1f)
            backgroundColor(colors.c(colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

private fun ViewContainer<*, *>.ThLabel(label: String, alignLeft: Boolean) {
    val colors = AppTheme.colors
    Text {
        attr {
            fontSize(AppTypography.fs12)
            color(colors.c(colors.textTertiary))
            text(label)
            if (!alignLeft) textAlignRight() else textAlignLeft()
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

/** 涨跌幅色块（欧易式）：红/绿实心圆角块 + 白字，0% 灰块。 */
private fun ViewContainer<*, *>.ChangeBadge(changePercent: Double, compact: Boolean) {
    val colors = AppTheme.colors
    val bg = when {
        changePercent > 0.0001 -> colors.up
        changePercent < -0.0001 -> colors.down
        else -> colors.textTertiary
    }
    View {
        attr {
            width(if (compact) 84f else 110f); allCenter()
        }
        View {
            attr {
                padding(left = 8f, right = 8f, top = 4f, bottom = 4f)
                borderRadius(4f)
                backgroundColor(colors.c(bg))
            }
            Text {
                attr {
                    fontSize(AppTypography.fs12); fontWeightSemiBold()
                    fontFamily(NUM_FONT); lines(1); textOverFlowClip()
                    color(Color.WHITE)
                    text(fmtPct(changePercent))
                }
            }
        }
    }
}

/** 股票行：64px / 仅底部 1px 分割线 / hover surfaceHover 120ms / pressed surfaceSecondary。 */
fun ViewContainer<*, *>.StockRow(
    q: StockQuote,
    spark: List<Double>,
    narrow: Boolean,
    compact: Boolean = false,
    columns: Set<String> = StockColumns.DEFAULT,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    // compact 副行：代码 + 市值（缺失省略）+ 成交量，去掉「市值/量」前缀省宽度
    val compactMeta = if (compact) buildList {
        if (StockColumns.MARKET_CAP in columns && (q.marketCap ?: 0.0) > 0.0) add(fmtMarketCap(q.marketCap))
        if (StockColumns.VOLUME in columns) add(fmtVolHand(q.volume))
    }.joinToString(" · ") else ""
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            height(64f)
            backgroundColor(colors.c(colors.surface))
            highlightBackgroundColor(colors.c(colors.surfaceHover))
            accessibility("${q.name} ${fmtSymbol(q.symbol)}，最新价 ${fmt2(q.price)}，涨跌幅 ${fmtPct(q.changePercent)}")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-row zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        // 股票（名称 + 代码 / compact 时代码后接市值·量）
        View {
            attr { flex(2.4f) }
            Text {
                attr {
                    fontSize(AppTypography.fs14); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                    text(q.name)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            Text {
                attr {
                    marginTop(3f)
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary))
                    lines(1)
                    cssClass("zn-nowrap")
                    text(if (compactMeta.isEmpty()) fmtSymbol(q.symbol) else fmtSymbol(q.symbol) + " · " + compactMeta)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        // 最新价
        // 最新价（compact：价格 + 下方「涨跌额 涨跌幅」同色小字，课题 Task1 要求涨跌额；东财移动端样式）
        if (compact) {
            View {
                attr { width(128f); flexDirectionColumn(); alignItemsFlexEnd() }
                Text {
                    attr {
                        fontSize(AppTypography.fs15); fontWeightSemiBold()
                        fontFamily(NUM_FONT); lines(1); cssClass("zn-nowrap")
                        color(colors.c(colors.textPrimary))
                        text(fmt2(q.price))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                Text {
                    attr {
                        marginTop(2f)
                        fontSize(AppTypography.fs11); fontWeightMedium()
                        fontFamily(NUM_FONT); lines(1); cssClass("zn-nowrap")
                        color(colors.c(if (q.isUp) colors.up else colors.down))
                        text(fmtChangeSigned(q.change) + "  " + fmtPct(q.changePercent))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
            }
        } else {
            NumCell(fmt2(q.price), 120f, { colors.textPrimary }, semibold = true)
            // 涨跌额（桌面列）
            if (StockColumns.CHANGE in columns) NumCell(fmtChangeSigned(q.change), 110f, { if (q.isUp) colors.up else colors.down })
            // 涨跌幅（桌面：文字 + 浅色底）
            NumCell(fmtPct(q.changePercent), 110f, { if (q.isUp) colors.up else colors.down }, semibold = true, chipBg = { if (q.isUp) colors.riseBackground else colors.fallBackground })
        }
        // 总市值 / 成交量（课题基础字段）
        if (!compact && StockColumns.MARKET_CAP in columns) NumCell(fmtMarketCap(q.marketCap), 110f, { colors.textSecondary })
        if (!compact && StockColumns.VOLUME in columns) NumCell(fmtVolHand(q.volume), 110f, { colors.textSecondary })
        // 今日走势（84×28 sparkline；compact 隐藏——色块已是视觉锚点）
        if (!compact && StockColumns.SPARK in columns) {
            View {
                attr { width(170f); allCenter() }
                Canvas({
                    attr { width(84f); height(28f) }
                }) { context, w, h ->
                    drawSparkLine(
                        context, spark,
                        colors.c(if (q.isUp) colors.up else colors.down),
                        w, h,
                    )
                }
            }
        }
        if (!narrow && StockColumns.HIGH_LOW in columns) NumCell(fmt2(q.high) + " / " + fmt2(q.low), 150f, { colors.textSecondary })
        if (!narrow && StockColumns.AMOUNT in columns) NumCell(fmtAmount(q.amount), 110f, { colors.textSecondary })
        // 行底分割线（满足 vfor 单一孩子约束）
        View {
            attr {
                absolutePosition(top = 63f, left = 0f, right = 0f)
                height(1f)
                backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** 榜单行数据（人气榜 / 涨幅榜）：排名 + 名称代码 + 最新价 + 涨跌幅。 */
data class RankRow(
    val rank: Int,
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
) {
    val isUp: Boolean get() = changePercent >= 0
}

/** 行业板块表（东财真实数据）：# 36 | 板块 flex | 涨跌 90/64 | 领涨股 flex 右对齐。行点击进领涨股详情。 */
fun ViewContainer<*, *>.SectorTable(
    rows: () -> ObservableList<SectorRow>,
    compact: Boolean,
    onRowClick: (SectorRow) -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(42f) }
        View { attr { width(36f) }; ThLabel("#", alignLeft = true) }
        View { attr { flex(1f) }; ThLabel("行业板块", alignLeft = true) }
        View { attr { width(if (compact) 64f else 90f) }; ThLabel("涨跌幅", alignLeft = false) }
        View { attr { width(if (compact) 110f else 180f) }; ThLabel("领涨股", alignLeft = false) }
    }
    View {
        attr {
            height(1f)
            backgroundColor(colors.c(colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    vfor({ rows() }) { row ->
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                height(56f)
                backgroundColor(colors.c(colors.surface))
                highlightBackgroundColor(colors.c(colors.surfaceHover))
                accessibility("第 ${row.rank} 名板块 ${row.name}，涨跌幅 ${fmtPct(row.changePercent)}，领涨股 ${row.leadStock}")
                accessibilityRole(AccessibilityRole.BUTTON)
                accessibilityInfo(clickable = true, longClickable = false)
                cssClass("zn-row zn-click")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            event { click { onRowClick(row) } }
            View {
                attr { width(36f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); fontFamily(NUM_FONT); fontWeightSemiBold()
                        color(colors.c(if (row.rank <= 3) colors.textPrimary else colors.textTertiary))
                        text(row.rank.toString())
                    }
                }
            }
            View {
                attr { flex(1f); flexDirectionColumn() }
                Text {
                    attr {
                        fontSize(AppTypography.fs14); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text(row.name)
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                Text {
                    attr {
                        marginTop(3f)
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text("涨 ${row.upCount} / 跌 ${row.downCount}")
                    }
                }
            }
            NumCell(
                fmtPct(row.changePercent),
                if (compact) 64f else 90f,
                { if (row.isUp) colors.up else colors.down },
                semibold = true,
            )
            View {
                attr { width(if (compact) 110f else 180f); flexDirectionColumn(); alignItemsFlexEnd() }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors.c(if (row.leadChangePercent >= 0) colors.up else colors.down))
                        text(if (row.leadStock.isBlank()) "—" else "${row.leadStock} ${fmtPct(row.leadChangePercent)}")
                        textAlignRight(); lines(1); textOverFlowClip()
                    }
                }
            }
            View {
                attr {
                    absolutePosition(top = 55f, left = 0f, right = 0f)
                    height(1f)
                    backgroundColor(colors.c(colors.border))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
    }
}

/** 榜单表：# 36 | 股票 flex | 最新价 120/78 | 涨跌幅 110/68。排名为榜单固有属性，不参与排序按钮。 */
fun ViewContainer<*, *>.RankTable(
    rows: () -> ObservableList<RankRow>,
    compact: Boolean,
    onRowClick: (RankRow) -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(42f) }
        View { attr { width(36f) }; ThLabel("#", alignLeft = true) }
        View { attr { flex(1f) }; ThLabel("股票", alignLeft = true) }
        View { attr { width(if (compact) 78f else 120f) }; ThLabel("最新价", alignLeft = false) }
        View { attr { width(if (compact) 68f else 110f) }; ThLabel("涨跌幅", alignLeft = false) }
    }
    View {
        attr {
            height(1f)
            backgroundColor(colors.c(colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    vfor({ rows() }) { row ->
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                height(56f)
                backgroundColor(colors.c(colors.surface))
                highlightBackgroundColor(colors.c(colors.surfaceHover))
                accessibility("第 ${row.rank} 名 ${row.name}，最新价 ${fmt2(row.price)}，涨跌幅 ${fmtPct(row.changePercent)}")
                accessibilityRole(AccessibilityRole.BUTTON)
                accessibilityInfo(clickable = true, longClickable = false)
                cssClass("zn-row zn-click")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            event { click { onRowClick(row) } }
            View {
                attr { width(36f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); fontFamily(NUM_FONT); fontWeightSemiBold()
                        color(colors.c(if (row.rank <= 3) colors.textPrimary else colors.textTertiary))
                        text(row.rank.toString())
                    }
                }
            }
            View {
                attr { flex(1f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs14); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text(row.name)
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                Text {
                    attr {
                        marginTop(3f)
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text(fmtSymbol(row.symbol))
                    }
                }
            }
            NumCell(if (row.price > 0.0) fmt2(row.price) else "—", if (compact) 78f else 120f, { colors.textPrimary }, semibold = true)
            NumCell(
                if (row.price > 0.0) fmtPct(row.changePercent) else "—",
                if (compact) 68f else 110f,
                { if (row.isUp) colors.up else colors.down },
                semibold = true,
            )
            View {
                attr {
                    absolutePosition(top = 55f, left = 0f, right = 0f)
                    height(1f)
                    backgroundColor(colors.c(colors.border))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.NumCell(
    value: String, width: Float, colorHex: () -> String, semibold: Boolean = false,
    chipBg: () -> String? = { null },
) {
    val colors = AppTheme.colors
    View {
        attr { width(width); flexDirectionRow(); alignItemsCenter() }
        if (chipBg() == null) {
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs14)
                    fontFamily(NUM_FONT)
                    if (semibold) fontWeightSemiBold()
                    color(colors.c(colorHex()))
                    text(value)
                    textAlignRight()
                }
            }
        } else {
            // 欧易式涨跌 chip：浅色底 + 同色文字，扫视效率更高
            View { attr { flex(1f) } }
            View {
                attr {
                    height(24f); padding(left = 8f, right = 8f); allCenter()
                    borderRadius(4f)
                    backgroundColor(colors.c(chipBg() ?: colorHex()))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        fontFamily(NUM_FONT)
                        if (semibold) fontWeightSemiBold()
                        color(colors.c(colorHex()))
                        text(value)
                    }
                }
            }
        }
    }
}
