/* 知牛 · ProfilePage（我的）：用户卡 + 资产统计 + 外观偏好 + 服务状态 + 关于/免责声明
 * 对标移动端行情 App「我的」Tab（KuiklyStock 同款 4 Tab 布局）；
 * 主题切换 / 服务状态由 ThemePopover 浮层迁入本页（浮层撤下，职责归页）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.data.local.AlertStore
import com.zhiniu.data.local.Watchlist
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.base.openComparePage
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.ThemeMode
import com.zhiniu.pages.components.THEME_MODE_SP_KEY
import com.zhiniu.pages.components.UPDOWN_SP_KEY
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.common.AppInput
import com.zhiniu.pages.components.common.SecondaryButton

private val APP_VERSION = "v1.0.0"
private const val HISTORY_SP_KEY = "zhiniu.history.items.v1"

@Page("Profile", supportInLocal = true)
internal class ProfilePage : AppBasePage() {

    internal var watchCount by observable(0)
    internal var alertCount by observable(0)
    // 服务器地址设置（真机联调：填电脑局域网 IP；模拟器默认 10.0.2.2:8000 无需改动）
    internal var gatewayDraft by observable("")
    internal var gatewayTip by observable("")
    internal var aboutExpanded by observable(false)
    internal var disclaimerExpanded by observable(false)
    internal val historyItems by observableList<com.zhiniu.data.local.HistoryItem>()

    override fun created() {
        super.created()
        watchCount = Watchlist.symbols().size
        alertCount = AlertStore.alerts().size
        val sp = acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
            com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
        )
        // 恢复涨跌配色偏好（SP 唯一事实源 → AppTheme 全局生效）
        AppTheme.swapUpDon = sp.getString(UPDOWN_SP_KEY) == "1"
        // 恢复外观偏好（浅色/深色/跟随系统；此前仅内存态，重启丢失）
        when (sp.getString(THEME_MODE_SP_KEY)) {
            ThemeMode.LIGHT.name -> AppTheme.applyMode(ThemeMode.LIGHT)
            ThemeMode.DARK.name -> AppTheme.applyMode(ThemeMode.DARK)
            ThemeMode.SYSTEM.name -> AppTheme.applyMode(ThemeMode.SYSTEM)
        }
        // 持久化钩子已在 AppBasePage.created 注册（与 AI 指令 set_appearance 共用）
        // 浏览历史恢复 + 用本地快照补价格（拿不到价格的只显示名称）
        com.zhiniu.data.local.ViewHistory.deserialize(sp.getString(HISTORY_SP_KEY))
        historyItems.diffUpdate(com.zhiniu.data.local.ViewHistory.withPrices { repo.quoteOf(it) })
    }

    /** 保存服务器地址：http(s) 校验 → 全局生效 + SP 持久化（重启后仍生效）。 */
    internal fun saveGateway() {
        val url = gatewayDraft.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            gatewayTip = "地址需以 http:// 或 https:// 开头"
            return
        }
        GatewayMarketClient.baseUrl = url
        acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
            com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
        ).setString(GatewayMarketClient.GATEWAY_SP_KEY, url)
        gatewayTip = "已保存并连接 $url"
        refreshServiceStatus()
    }

    /** 恢复平台默认网关（模拟器 10.0.2.2:8000 / H5 同源 127.0.0.1:8000）。 */
    internal fun resetGateway() {
        gatewayDraft = GatewayMarketClient.DEFAULT_BASE_URL
        GatewayMarketClient.baseUrl = GatewayMarketClient.DEFAULT_BASE_URL
        acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
            com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
        ).setString(GatewayMarketClient.GATEWAY_SP_KEY, "")
        gatewayTip = "已恢复默认 " + GatewayMarketClient.DEFAULT_BASE_URL
        refreshServiceStatus()
    }

    /** 保存后重探服务状态（绿点即时反馈）。 */
    private fun refreshServiceStatus() {
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.health() }
                .onSuccess { status ->
                    gatewayOnline = status.online
                    agentReady = status.agentReady
                }
        }
    }

    internal fun toggleSwapUpDon() {
        AppTheme.swapUpDon = !AppTheme.swapUpDon
        val sp = acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
            com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
        )
        sp.setString(UPDOWN_SP_KEY, if (AppTheme.swapUpDon) "1" else "0")
    }

    internal fun clearHistory() {
        com.zhiniu.data.local.ViewHistory.clear()
        historyItems.diffUpdate(emptyList())
        val sp = acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
            com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
        )
        sp.setString(HISTORY_SP_KEY, "")
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@ProfilePage, "我的")
        List {
            attr {
                flex(1f)
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            profileContent(this@ProfilePage)
            View { attr { height(24f) } }
        }
        renderBottomTab(this@ProfilePage, "我的")
    }
}

// ============== 内容区 ==============
private fun ViewContainer<*, *>.profileContent(host: ProfilePage) {
    val colors = AppTheme.colors
    val pad: Float = if (host.isCompact()) 16f else 32f
    View {
        attr { padding(left = pad, right = pad) }

        // ---- 用户卡 ----
        View { attr { height(20f) } }
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                padding(all = 16f)
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surface))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            // 头像：官方品牌 Logo 标（圆底容器）
            View {
                attr {
                    width(52f); height(52f); borderRadius(26f); allCenter()
                    backgroundColor(colors.ca(colors.up, 8))
                }
                Image {
                    attr {
                        size(30f, 30f)
                        src(ImageUri.commonAssets("brand/logo-mark.png"))
                    }
                }
            }
            View { attr { width(14f) } }
            View {
                attr { flex(1f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs16); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary)); text("知牛研究员")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                View { attr { height(3f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textSecondary)); text("本地账户 · 自选与预警仅存本机")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
            }
            ThemeQuickToggle()
        }

        // ---- 资产统计三格 ----
        View { attr { height(12f) } }
        View {
            attr { flexDirectionRow(); alignItemsStretch() }
            StatCell("自选股票", "${host.watchCount}", "只", colors) {
                host.navWatchlist()
            }
            View { attr { width(10f) } }
            StatCell("价格预警", "${host.alertCount}", "条", colors) {
                host.navWatchlist()
            }
            View { attr { width(10f) } }
            StatCell("AI 研究", "多视角", "", colors) {
                host.navAiResearch()
            }
        }

        // ---- 浏览历史（横向胶囊流，点击直达详情；空态不占位） ----
        vif({ host.historyItems.isNotEmpty() }) {
            View { attr { height(20f) } }
            View {
                attr { flexDirectionRow(); alignItemsCenter(); marginBottom(8f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs12); fontWeightMedium()
                        color(colors.c(colors.textSecondary)); text("浏览历史")
                    }
                }
                View { attr { flex(1f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary)); text("清空")
                        accessibility("清空浏览历史")
                        accessibilityRole(com.tencent.kuikly.core.base.attr.AccessibilityRole.BUTTON)
                        accessibilityInfo(clickable = true, longClickable = false)
                        cssClass("zn-click")
                    }
                    event { click { host.clearHistory() } }
                }
            }
            View {
                attr { flexDirectionRow(); flexWrapWrap() }
                host.historyItems.forEach { item ->
                    View {
                        attr {
                            height(30f); paddingLeft(10f); paddingRight(10f)
                            marginBottom(6f); marginRight(8f)
                            flexDirectionRow(); alignItemsCenter()
                            borderRadius(15f)
                            backgroundColor(colors.c(colors.surface))
                            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                            cssClass("zn-click")
                            accessibility("查看 ${item.name}")
                            accessibilityRole(com.tencent.kuikly.core.base.attr.AccessibilityRole.BUTTON)
                            accessibilityInfo(clickable = true, longClickable = false)
                            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                        event { click { host.openStock(item.symbol) } }
                        Text {
                            attr {
                                fontSize(AppTypography.fs12); lines(1)
                                color(colors.c(colors.textPrimary)); text(item.name)
                            }
                        }
                        item.price?.let { p ->
                            View { attr { width(5f) } }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs11)
                                    fontFamily(com.zhiniu.pages.components.NUM_FONT)
                                    color(colors.c(colors.textTertiary))
                                    text(com.zhiniu.pages.components.fmt2(p))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- 偏好设置 ----
        View { attr { height(20f) } }
        GroupLabel("偏好设置")
        View {
            attr {
                flexDirectionColumn()
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surface))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            // 外观：三段式
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(left = 16f, right = 16f, top = 14f, bottom = 6f)
                }
                Icon(IconKind.THEME, 16f)
                View { attr { width(10f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs14); fontWeightMedium()
                        color(colors.c(colors.textPrimary)); text("外观")
                    }
                }
            }
            View {
                attr {
                    margin(left = 16f, right = 16f, top = 4f, bottom = 6f)
                    height(36f); padding(all = 3f)
                    flexDirectionRow()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    borderRadius(AppRadius.radius6)
                }
                ThemeSegment(ThemeMode.SYSTEM)
                ThemeSegment(ThemeMode.LIGHT)
                ThemeSegment(ThemeMode.DARK)
            }
            // 涨跌配色：红涨绿跌（A股默认）↔ 绿涨红跌（海外习惯），双预览色块即时生效
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    margin(left = 16f, right = 16f, bottom = 14f)
                }
                Icon(IconKind.CHART, 16f)
                View { attr { width(10f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs14); fontWeightMedium()
                        color(colors.c(colors.textPrimary)); text("涨跌配色")
                    }
                }
                View { attr { flex(1f) } }
                UpDownSwapToggle { host.toggleSwapUpDon() }
            }
        }

        // ---- 资产与服务 ----
        View { attr { height(20f) } }
        GroupLabel("资产与服务")
        View {
            attr {
                flexDirectionColumn()
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surface))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            SettingRow(IconKind.STAR, "我的自选", "${host.watchCount} 只", colors) { host.navWatchlist() }
            RowDivider()
            SettingRow(IconKind.BELL, "价格预警", "${host.alertCount} 条 · 触发记录在自选页", colors) { host.navWatchlist() }
            RowDivider()
            SettingRow(IconKind.CHART, "双股对比", "AI 对比两只股票强弱", colors) {
                val quotes = host.repo.stockQuotes()
                host.openComparePage(
                    quotes.getOrNull(0)?.symbol ?: com.zhiniu.data.mock.MockMarketDefaults.comparePair.first,
                    quotes.getOrNull(1)?.symbol ?: com.zhiniu.data.mock.MockMarketDefaults.comparePair.second,
                )
            }
        }

        // ---- 服务状态（原 ThemePopover 内容迁入，真实 /healthz） ----
        View { attr { height(20f) } }
        GroupLabel("服务状态")
        View {
            attr {
                flexDirectionColumn()
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surface))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            ServiceStatus(IconKind.DATA, "行情与资讯", host.gatewayOnline, if (host.gatewayOnline) "实时网关已连接" else "网关未连接 · 本地快照", colors)
            RowDivider()
            ServiceStatus(IconKind.AI, "研究 Agent", host.agentReady, if (host.agentReady) "LLM 已配置 · 多 Agent 辩论" else "规则降级 · 未伪装模型", colors)
        }

        // ---- 服务器地址（真机联调：填电脑局域网 IP；模拟器默认 10.0.2.2:8000 无需改动） ----
        View { attr { height(20f) } }
        GroupLabel("服务器地址")
        View {
            attr {
                flexDirectionColumn()
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surface))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                padding(all = 12f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            Text {
                attr {
                    fontSize(AppTypography.fs11)
                    color(colors.c(colors.textTertiary)); marginBottom(8f)
                    text("当前：${com.zhiniu.data.remote.GatewayMarketClient.baseUrl}")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            AppInput(
                placeholder = "http://192.168.1.100:8000（真机填电脑 IP）",
                text = host.gatewayDraft, height = 36f,
                onTextChange = { host.gatewayDraft = it },
            )
            View { attr { height(8f) } }
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                SecondaryButton("保存并连接", height = 28f) { host.saveGateway() }
                View { attr { width(8f) } }
                SecondaryButton("恢复默认", height = 28f) { host.resetGateway() }
                View { attr { flex(1f) } }
                vif({ host.gatewayTip.isNotBlank() }) {
                    Text {
                        attr {
                            fontSize(AppTypography.fs11)
                            color(colors.c(colors.textTertiary)); text(host.gatewayTip)
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                }
            }
        }

        // ---- 关于 / 免责声明 ----
        View { attr { height(20f) } }
        GroupLabel("关于")
        View {
            attr {
                flexDirectionColumn()
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surface))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            // 关于知牛（可展开）
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(left = 16f, right = 16f)
                    height(48f)
                    cssClass("zn-click")
                    accessibility("展开关于知牛")
                    accessibilityRole(AccessibilityRole.BUTTON)
                    accessibilityInfo(clickable = true, longClickable = false)
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
                }
                event { click { host.aboutExpanded = !host.aboutExpanded } }
                Icon(IconKind.INFO, 16f)
                View { attr { width(10f) } }
                Text {
                    attr {
                        flex(1f)
                        fontSize(AppTypography.fs14); fontWeightMedium()
                        color(colors.c(colors.textPrimary)); text("关于知牛")
                    }
                }
                Icon(if (host.aboutExpanded) IconKind.CHEVRON_UP else IconKind.CHEVRON_DOWN, 12f)
            }
            vif({ host.aboutExpanded }) {
                View {
                    attr { padding(left = 16f, right = 16f, bottom = 14f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); lineHeight(19f)
                            color(colors.c(colors.textSecondary))
                            text(
                                "知牛 · ZhiNiu —— 基于 Kuikly（腾讯 KMP 跨端框架）的 AI 股票应用。" +
                                    "一套 Kotlin 代码运行于 H5 / Android / iOS / 鸿蒙四端。\n" +
                                    "多 Agent 研究管线：行情 → 技术面 → 财务 → 资讯 → 风险 → 多空辩论 → 风控归纳。" +
                                    "数据源：新浪行情 / 东方财富资讯；AI：OpenAI 兼容网关（DeepSeek / GLM 等）。\n" +
                                    "致谢：Tencent-TDS/KuiklyUI、tdesign-icons、JetBrains Mono。",
                            )
                        }
                    }
                }
            }
            RowDivider()
            // 免责声明（可展开）
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(left = 16f, right = 16f)
                    height(48f)
                    cssClass("zn-click")
                    accessibility("展开免责声明")
                    accessibilityRole(AccessibilityRole.BUTTON)
                    accessibilityInfo(clickable = true, longClickable = false)
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
                }
                event { click { host.disclaimerExpanded = !host.disclaimerExpanded } }
                Icon(IconKind.ERROR, 16f)
                View { attr { width(10f) } }
                Text {
                    attr {
                        flex(1f)
                        fontSize(AppTypography.fs14); fontWeightMedium()
                        color(colors.c(colors.textPrimary)); text("免责声明")
                    }
                }
                Icon(if (host.disclaimerExpanded) IconKind.CHEVRON_UP else IconKind.CHEVRON_DOWN, 12f)
            }
            vif({ host.disclaimerExpanded }) {
                View {
                    attr { padding(left = 16f, right = 16f, bottom = 14f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); lineHeight(19f)
                            color(colors.c(colors.textSecondary))
                            text("本应用仅供技术学习与 Kuikly 框架演示，所有行情与 AI 输出不构成任何投资建议。股市有风险，投资需谨慎。")
                        }
                    }
                }
            }
        }

        // ---- 页脚 ----
        View { attr { height(18f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11); lineHeight(17f)
                color(colors.c(colors.textTertiary))
                text("知牛 $APP_VERSION · Kuikly · 课题 Task 1 + Task 2 演示")
            }
        }
    }
}

// ============== 组件 ==============

private fun ViewContainer<*, *>.GroupLabel(label: String) {
    Text {
        attr {
            marginBottom(8f)
            fontSize(AppTypography.fs12)
            color(AppTheme.colors.c(AppTheme.colors.textTertiary))
            text(label)
        }
    }
}

private fun ViewContainer<*, *>.RowDivider() {
    View {
        attr {
            marginLeft(42f); height(1f)
            backgroundColor(AppTheme.colors.c(AppTheme.colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

/** 头像右侧深浅色快捷切换（一步直达，同 KuiklyStock 外观入口）。 */
private fun ViewContainer<*, *>.ThemeQuickToggle() {
    val colors = AppTheme.colors
    val next: ThemeMode = if (AppTheme.isDark) ThemeMode.LIGHT else ThemeMode.DARK
    View {
        attr {
            width(38f); height(38f); borderRadius(19f); allCenter()
            backgroundColor(colors.c(colors.surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            accessibility("切换到${next.label}")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 10))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { AppTheme.applyModePersisted(next) } }
        Icon(if (AppTheme.isDark) IconKind.THEME else IconKind.THEME, 17f)
    }
}

/** 统计格：数字大字 + 单位 + 标签，点击跳转。 */
private fun ViewContainer<*, *>.StatCell(
    label: String,
    value: String,
    unit: String,
    colors: com.zhiniu.pages.components.Palette,
    onClick: () -> Unit,
) {
    View {
        attr {
            flex(1f)
            padding(top = 12f, bottom = 12f)
            flexDirectionColumn(); alignItemsCenter()
            borderRadius(AppRadius.radius8)
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            cssClass("zn-click")
            accessibility("$label $value$unit，点击查看")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        View {
            attr { flexDirectionRow(); alignItemsFlexEnd() }
            Text {
                attr {
                    fontSize(AppTypography.fs20); fontWeightSemiBold()
                    fontFamily(com.zhiniu.pages.components.NUM_FONT)
                    color(colors.c(colors.textPrimary)); text(value)
                }
            }
            if (unit.isNotEmpty()) {
                Text {
                    attr {
                        marginLeft(2f); marginBottom(2f)
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary)); text(unit)
                    }
                }
            }
        }
        View { attr { height(3f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textSecondary)); text(label)
            }
        }
    }
}

/** 设置行：图标 + 标题 + 右侧说明 + chevron。 */
private fun ViewContainer<*, *>.SettingRow(
    icon: IconKind,
    title: String,
    desc: String,
    colors: com.zhiniu.pages.components.Palette,
    onClick: () -> Unit,
) {
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            padding(left = 16f, right = 12f)
            height(48f)
            cssClass("zn-click")
            accessibility("$title，$desc")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
        }
        event { click { onClick() } }
        Icon(icon, 16f)
        View { attr { width(10f) } }
        Text {
            attr {
                flex(1f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(colors.c(colors.textPrimary)); text(title)
            }
        }
        Text {
            attr {
                fontSize(AppTypography.fs12); lines(1)
                color(colors.c(colors.textTertiary)); text(desc)
            }
        }
        View { attr { width(4f) } }
        Icon(IconKind.CHEVRON_RIGHT, 12f)
    }
}

/** 服务状态行：真实 /healthz 结果，绿/琥珀状态点。 */
private fun ViewContainer<*, *>.ServiceStatus(
    icon: IconKind,
    label: String,
    ok: Boolean,
    desc: String,
    colors: com.zhiniu.pages.components.Palette,
) {
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            padding(left = 16f, right = 16f)
            height(48f)
            accessibility("$label：$desc")
            accessibilityRole(AccessibilityRole.TEXT)
        }
        Icon(icon, 16f)
        View { attr { width(10f) } }
        Text {
            attr {
                flex(1f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(colors.c(colors.textPrimary)); text(label)
            }
        }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary)); text(desc)
            }
        }
        View { attr { width(8f) } }
        View {
            attr {
                width(7f); height(7f); borderRadius(4f)
                backgroundColor(colors.c(if (ok) colors.down else colors.ma5))
            }
        }
    }
}

/** 涨跌配色切换：双段胶囊（红涨绿跌 / 绿涨红跌），当前态高亮；色块预览即时跟随 AppTheme。 */
private fun ViewContainer<*, *>.UpDownSwapToggle(onToggle: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(32f); padding(all = 3f)
            flexDirectionRow()
            backgroundColor(colors.c(colors.surfaceSecondary))
            borderRadius(AppRadius.radius6)
        }
        listOf(false to "红涨绿跌", true to "绿涨红跌").forEach { (swapped, label) ->
            val active = AppTheme.swapUpDon == swapped
            View {
                attr {
                    height(26f); paddingLeft(8f); paddingRight(8f)
                    flexDirectionRow(); alignItemsCenter(); allCenter()
                    borderRadius(AppRadius.radius5)
                    backgroundColor(if (active) colors.c(colors.surface) else com.tencent.kuikly.core.base.Color.TRANSPARENT)
                    if (active) border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                    accessibility("涨跌配色：$label")
                    accessibilityRole(AccessibilityRole.BUTTON)
                    accessibilityInfo(clickable = true, longClickable = false)
                    cssClass("zn-click")
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 7))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event { click { if (!active) onToggle() } }
                // 预览色点：涨色 + 跌色（该档位下的真实取色）
                val upHex = if (swapped) "#16B364" else "#F04F5F"
                val downHex = if (swapped) "#F04F5F" else "#16B364"
                View { attr { width(8f); height(8f); borderRadius(4f); backgroundColor(Color2(upHex)) } }
                View { attr { width(4f) } }
                View { attr { width(8f); height(8f); borderRadius(4f); backgroundColor(Color2(downHex)) } }
                View { attr { width(5f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        fontWeightMedium()
                        color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                        text(label)
                    }
                }
            }
        }
    }
}

private fun Color2(hex: String): com.tencent.kuikly.core.base.Color =
    com.tencent.kuikly.core.base.Color((0xFF shl 24) or hex.removePrefix("#").toLong(16).toInt())

/** 外观三段式（跟随系统 / 浅色 / 深色），选中白底描边。 */
private fun ViewContainer<*, *>.ThemeSegment(mode: ThemeMode) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); height(30f); allCenter()
            borderRadius(AppRadius.radius5)
            backgroundColor(
                if (AppTheme.mode == mode) colors.c(colors.surface)
                else com.tencent.kuikly.core.base.Color.TRANSPARENT
            )
            if (AppTheme.mode == mode) border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            accessibility("外观：${mode.label}")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 7))
        }
        event { click { AppTheme.applyModePersisted(mode) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                fontWeightMedium()
                color(colors.c(if (AppTheme.mode == mode) colors.textPrimary else colors.textSecondary))
                text(mode.label)
            }
        }
    }
}
