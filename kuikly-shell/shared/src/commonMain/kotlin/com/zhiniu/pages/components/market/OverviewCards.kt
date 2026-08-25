// 知牛 · 市场概览卡片（主要指数 / 市场热门 / 市场宽度）+ 统一搜索弹层
package com.zhiniu.pages.components.market

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtInt
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.common.IconButton

/** 概览卡片外壳：等高三等分、radius 10、border 1、padding 16。 */
fun ViewContainer<*, *>.MarketOverviewCard(
    title: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            flex(1f)
            backgroundColor(colors().c(colors().surface))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            borderRadius(AppRadius.r10)
            cssClass("zn-card")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { padding(top = 14f, left = 16f, right = 16f) }
            Text {
                attr {
                    fontSize(AppTypography.fs15); fontWeightSemiBold()
                    color(colors().c(colors().textPrimary))
                    text(title)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(10f) } }
            content()
        }
    }
}

/** 指数行：名称 + 价格 + 涨跌幅 + sparkline。 */
fun ViewContainer<*, *>.IndexRow(idx: Quote, spark: List<Double>) {
    val colors = { AppTheme.colors }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(33f) }
        Text {
            attr {
                width(76f)
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textSecondary))
                text(idx.name)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors().c(colors().textPrimary))
                text(fmt2(idx.price))
            }
        }
        Text {
            attr {
                width(64f)
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors().c(if (idx.isUp) colors().up else colors().down))
                text(fmtPct(idx.changePercent))
            }
        }
        MiniSparkline(spark, idx.isUp, 52f, 20f)
    }
}

/** 热门股票行（可点击）。 */
fun ViewContainer<*, *>.HotStockRow(st: Quote, onClick: () -> Unit) {
    val colors = { AppTheme.colors }
    View {
        attr {
            flexDirectionRow(); alignItemsCenter(); height(33f)
            cssClass("zn-nav zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                width(110f)
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textPrimary))
                text(st.name)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors().c(colors().textPrimary))
                text(fmt2(st.price))
            }
        }
        Text {
            attr {
                width(64f)
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors().c(if (st.isUp) colors().up else colors().down))
                text(fmtPct(st.changePercent))
            }
        }
    }
}

/** 市场宽度卡内容：上涨/下跌、比例条、成交额、状态徽章。 */
fun ViewContainer<*, *>.MarketBreadthContent(
    upCount: Long,
    downCount: Long,
    upRatio: Double,
    amountLabel: String,
    status: String,
) {
    val colors = { AppTheme.colors }
    // 上涨 / 下跌
    View {
        attr { flexDirectionRow() }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors().c(colors().textSecondary))
                    text("上涨")
                }
            }
            Text {
                attr {
                    marginTop(4f)
                    fontSize(AppTypography.fs18); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors().c(colors().up))
                    text(fmtInt(upCount))
                }
            }
        }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors().c(colors().textSecondary))
                    text("下跌")
                }
            }
            Text {
                attr {
                    marginTop(4f)
                    fontSize(AppTypography.fs18); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors().c(colors().down))
                    text(fmtInt(downCount))
                }
            }
        }
        View {
            attr { flex(1f); alignItemsFlexEnd() }
            StatusBadgePill(status, if (upRatio >= 0.5) colors().up else colors().down)
        }
    }
    // 上涨占比
    View {
        attr { flexDirectionRow(); marginTop(12f); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textTertiary))
                text("上涨占比")
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors().c(colors().textPrimary))
                text(fmt2(upRatio * 100.0) + "%")
            }
        }
    }
    // 红绿比例条
    View {
        attr {
            marginTop(8f)
            height(8f)
            borderRadius(allBorderRadius = 4f)
            flexDirectionRow()
            overflow(true)
            backgroundColor(colors().c(colors().surfaceSecondary))
        }
        val upF = upRatio.coerceIn(0.05, 0.95).toFloat()
        View { attr { flex(upF); height(8f); backgroundColor(colors().ca(colors().up, 90)) } }
        View { attr { flex(1f - upF); height(8f); backgroundColor(colors().ca(colors().down, 90)) } }
    }
    // 成交额
    View {
        attr { flexDirectionRow(); marginTop(12f); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textSecondary))
                text("成交额")
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors().c(colors().textPrimary))
                text(amountLabel)
            }
        }
    }
}

private fun ViewContainer<*, *>.StatusBadgePill(label: String, colorHex: String) {
    val colors = { AppTheme.colors }
    View {
        attr {
            borderRadius(AppRadius.r6)
            padding(left = 8f, right = 8f)
            height(22f)
            allCenter()
            backgroundColor(colors().ca(colorHex, 12))
        }
        Text {
            attr {
                fontSize(AppTypography.fs11); fontWeightSemiBold()
                color(colors().c(colorHex))
                text(label)
            }
        }
    }
}

/**
 * 统一股票搜索弹层（所有入口共用）。
 * 输入股票名/代码实时过滤；点击结果进入详情并自动关闭。
 */
fun ViewContainer<*, *>.StockSearchOverlay(
    visible: () -> Boolean,
    query: () -> String,
    recent: () -> com.tencent.kuikly.core.reactive.collection.ObservableList<Quote>,
    hot: () -> com.tencent.kuikly.core.reactive.collection.ObservableList<Quote>,
    results: () -> com.tencent.kuikly.core.reactive.collection.ObservableList<Quote>,
    left: Float,
    width: Float = 460f,
    onQueryChange: (String) -> Unit,
    onPick: (Quote) -> Unit,
    onClose: () -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            absolutePosition(top = 68f, left = left)
            width(width)
            zIndex(15)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (visible()) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = visible())
        }
        View {
            attr {
                backgroundColor(colors().c(colors().elevated))
                border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
                borderRadius(AppRadius.r10)
                cssClass("zn-pop")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            // 输入行
            View {
                attr {
                    height(52f); flexDirectionRow(); alignItemsCenter()
                    padding(left = 12f, right = 8f)
                }
                View {
                    attr {
                        flex(1f); height(36f); borderRadius(AppRadius.r8)
                        backgroundColor(colors().c(colors().surfaceSecondary))
                        border(Border(1f, BorderStyle.SOLID, colors().c(colors().border)))
                        overflow(true)
                    }
                    Input {
                        attr {
                            height(36f)
                            fontSize(AppTypography.fs13)
                            color(colors().c(colors().textPrimary))
                            placeholder("搜索股票 / 代码")
                            placeholderColor(colors().c(colors().textTertiary))
                            tintColor(colors().c(colors().textPrimary))
                            backgroundColor(Color.TRANSPARENT)
                        }
                        event {
                            textDidChange { params -> onQueryChange(params.text) }
                        }
                    }
                }
                View { attr { width(4f) } }
                IconButton(com.zhiniu.pages.components.IconKind.CLOSE, 15f, 32f) { onClose() }
            }
            View { attr { height(1f); backgroundColor(colors().c(colors().border)) } }
            // 内容
            View {
                attr { padding(top = 8f, bottom = 8f) }
                vif({ query().trim().isEmpty() }) {
                    vif({ !recent().isEmpty() }) {
                        SearchSectionLabel("最近")
                        vfor({ recent() }) { q -> SearchResultRow(q) { onPick(q) } }
                        View { attr { height(4f) } }
                    }
                    SearchSectionLabel("热门")
                    vfor({ hot() }) { q -> SearchResultRow(q) { onPick(q) } }
                }
                velse {
                    vif({ results().isEmpty() }) {
                        View {
                            attr { height(80f); allCenter() }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs13)
                                    color(colors().c(colors().textTertiary))
                                    text("未找到匹配的股票")
                                }
                            }
                        }
                    }
                    vif({ !results().isEmpty() }) {
                        SearchSectionLabel("搜索结果")
                        vfor({ results() }) { q -> SearchResultRow(q) { onPick(q) } }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.SearchSectionLabel(label: String) {
    val colors = { AppTheme.colors }
    Text {
        attr {
            marginLeft(14f); marginTop(8f); marginBottom(4f)
            fontSize(AppTypography.fs11)
            color(colors().c(colors().textTertiary))
            text(label)
        }
    }
}

private fun ViewContainer<*, *>.SearchResultRow(q: Quote, onClick: () -> Unit) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(46f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                width(170f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(colors().c(colors().textPrimary))
                text(q.name)
            }
        }
        Text {
            attr {
                width(104f)
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textTertiary))
                text(fmtSymbol(q.symbol))
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                width(88f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors().c(colors().textPrimary))
                text(fmt2(q.price))
            }
        }
        Text {
            attr {
                width(72f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors().c(if (q.isUp) colors().up else colors().down))
                text(fmtPct(q.changePercent))
            }
        }
        // 行底分隔线（内嵌，满足 vfor 单一孩子约束）
        View {
            attr {
                absolutePosition(top = 45f, left = 14f, right = 14f)
                height(1f)
                backgroundColor(colors().c(colors().border))
            }
        }
    }
}
