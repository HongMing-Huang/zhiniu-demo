/* 知牛 · TodoListPage（跨端待办清单 · 课题三端基线）
 * 新增 / 编辑 / 删除 / 完成 / 清除已完成 + 本地持久化。
 * 持久化走 Kuikly 官方 SharedPreferencesModule（Android SharedPreferences /
 * iOS NSUserDefaults / 鸿蒙与 H5 由各端渲染宿主实现），业务代码 100% commonMain。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.openAiResearchPage
import com.zhiniu.base.openMarketPage
import com.zhiniu.domain.model.TodoItem
import com.zhiniu.domain.model.TodoStore
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.common.AppInput
import com.zhiniu.pages.components.common.GhostButton
import com.zhiniu.pages.components.common.PrimaryButton
import com.zhiniu.pages.components.common.SecondaryButton

/** 存储键：与端内其他 KV 数据隔离，前缀知牛品牌。 */
private const val TODO_STORE_KEY = "zhiniu.todo.items.v1"

@Page("TodoList", supportInLocal = true)
internal class TodoListPage : AppBasePage() {

    internal val items by observableList<TodoItem>()
    internal var draft by observable("")
    internal var editingId by observable(0L)
    internal var editDraft by observable("")
    internal var loadFailed by observable(false)

    // H5 原生输入值与 observable 状态可能不同步，提交后需经 ViewRef 显式 setText 清空
    internal var addInputRef: ViewRef<InputView>? = null
    internal var editInputRef: ViewRef<InputView>? = null

    private lateinit var sp: SharedPreferencesModule

    override fun created() {
        super.created()
        sp = acquireModule(SharedPreferencesModule.MODULE_NAME)
        items.addAll(TodoStore.deserialize(sp.getString(TODO_STORE_KEY)))
    }

    internal fun addItem() {
        val next = TodoStore.add(items.toList(), draft)
        if (next.size == items.size) return // 无效输入（空串/纯空格），保持 draft 不清空便于修正
        items.diffUpdate(next)
        draft = ""
        addInputRef?.view?.setText("")
        persist()
    }

    internal fun toggleItem(id: Long) {
        items.diffUpdate(TodoStore.toggle(items.toList(), id))
        persist()
    }

    internal fun removeItem(id: Long) {
        if (id == editingId) cancelEdit()
        items.diffUpdate(TodoStore.remove(items.toList(), id))
        persist()
    }

    internal fun beginEdit(item: TodoItem) {
        editingId = item.id
        editDraft = item.title
    }

    internal fun saveEdit() {
        val next = TodoStore.rename(items.toList(), editingId, editDraft)
        items.diffUpdate(next)
        editingId = 0L
        editDraft = ""
        editInputRef?.view?.setText("")
        persist()
    }

    internal fun cancelEdit() {
        editingId = 0L
        editDraft = ""
        editInputRef?.view?.setText("")
    }

    internal fun clearDone() {
        items.diffUpdate(TodoStore.clearDone(items.toList()))
        persist()
    }

    internal fun doneCount(): Int = items.count { it.done }

    private fun persist() {
        sp.setString(TODO_STORE_KEY, TodoStore.serialize(items.toList()))
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@TodoListPage, "待办")
        todoContent(this@TodoListPage)
    }
}

// ============== 内容区 ==============
private fun ViewContainer<*, *>.todoContent(host: TodoListPage) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); flexDirectionColumn(); alignItemsCenter()
            paddingLeft(if (host.isCompact()) 16f else 32f)
            paddingRight(if (host.isCompact()) 16f else 32f)
            paddingBottom(host.bottomNavInset() + 48f)
            backgroundColor(colors.c(colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { width(host.contentWidth()); flexDirectionColumn() }
            // 标题区
            View { attr { height(28f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs24); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary)); text("待办清单")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(6f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs13)
                    color(colors.c(colors.textSecondary))
                    text("跨端基线演示：新增 / 编辑 / 删除 / 本地持久化，一套代码 Android · iOS · 鸿蒙运行")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            // 新增行
            View { attr { height(20f) } }
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                AppInput(
                    placeholder = "添加待办，回车或点「添加」",
                    text = host.draft,
                    onTextChange = { host.draft = it },
                    onReturn = { host.addItem() },
                    onRef = { host.addInputRef = it },
                )
                View { attr { width(8f) } }
                PrimaryButton("添加") { host.addItem() }
            }
            // 清单卡片
            View { attr { height(16f) } }
                View {
                    attr {
                        flexDirectionColumn()
                        borderRadius(AppRadius.radius8)
                        border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                        backgroundColor(colors.c(colors.surface))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                vif({ host.items.isEmpty() }) { todoEmpty() }
                vfor({ host.items }) { item ->
                    // vfor 的 lambda receiver 非 ViewContainer，vif/velse 需套一层 View 才能解析
                    View {
                        attr { flexDirectionColumn() }
                        vif({ host.editingId == item.id }) { todoEditRow(host, item) }
                        vif({ host.editingId != item.id }) { todoRow(host, item) }
                    }
                }
            }
            // 底部统计 + 清除已完成
            View { attr { height(12f) } }
            View {
                attr { flexDirectionRow(); alignItemsCenter(); paddingLeft(4f); paddingRight(4f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors.c(colors.textSecondary))
                        text("共 ${host.items.size} 项 · 未完成 ${host.items.size - host.doneCount()} 项")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                View { attr { flex(1f) } }
                vif({ host.doneCount() > 0 }) {
                    GhostButton("清除已完成", height = 30f) { host.clearDone() }
                }
            }
        }
    }
}

/** 空态：与清单卡片同布局的轻提示，避免加载前后布局跳位。 */
private fun ViewContainer<*, *>.todoEmpty() {
    val colors = AppTheme.colors
    View {
        attr {
            height(96f); alignItemsCenter(); justifyContentCenter()
            accessibility("暂无待办")
        }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                color(colors.c(colors.textSecondary)); text("暂无待办，先添加一条")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** 常态行：勾选圆钮 + 标题（点击整区切换完成）+ 编辑/删除。 */
private fun ViewContainer<*, *>.todoRow(host: TodoListPage, item: TodoItem) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            paddingLeft(16f)
            borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 完成切换区（勾选钮 + 标题）
        View {
            attr {
                flex(1f); flexDirectionRow(); alignItemsCenter()
                paddingTop(14f); paddingBottom(14f)
                cssClass("zn-click")
                highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
                accessibility(if (item.done) "标记 ${item.title} 为未完成" else "完成 ${item.title}")
                accessibilityRole(AccessibilityRole.BUTTON)
            }
            event { click { host.toggleItem(item.id) } }
            doneBadge(item.done)
            Text {
                attr {
                    flex(1f); marginLeft(12f); marginRight(12f)
                    fontSize(AppTypography.fs15)
                    color(colors.c(if (item.done) colors.textSecondary else colors.textPrimary))
                    opacity(if (item.done) 0.72f else 1f)
                    text(item.title); lines(2); textOverFlowClip()
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        View {
            attr { flexDirectionRow(); alignItemsCenter(); paddingRight(10f) }
            GhostButton("编辑", height = 30f) { host.beginEdit(item) }
            GhostButton("删除", height = 30f) { host.removeItem(item.id) }
        }
    }
}

/** 编辑态行：输入框 + 保存/取消。 */
private fun ViewContainer<*, *>.todoEditRow(host: TodoListPage, item: TodoItem) {
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            paddingLeft(16f); paddingTop(10f); paddingBottom(10f); paddingRight(10f)
            borderBottom(Border(1f, BorderStyle.SOLID, AppTheme.colors.c(AppTheme.colors.border)))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        AppInput(
            placeholder = "修改待办内容",
            text = host.editDraft,
            onTextChange = { host.editDraft = it },
            onReturn = { host.saveEdit() },
            onRef = { host.editInputRef = it },
        )
        View { attr { width(8f) } }
        PrimaryButton("保存") { host.saveEdit() }
        View { attr { width(6f) } }
        SecondaryButton("取消") { host.cancelEdit() }
    }
}

/** 完成态圆钮：完成 = 主色实心 + ✓；未完成 = 1.5px 描边空心。 */
private fun ViewContainer<*, *>.doneBadge(done: Boolean) {
    val colors = AppTheme.colors
    View {
        attr {
            size(20f, 20f)
            borderRadius(allBorderRadius = 10f)
            border(Border(1.5f, BorderStyle.SOLID, colors.c(colors.textPrimary)))
            backgroundColor(colors.c(if (done) colors.textPrimary else colors.surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        vif({ done }) {
            Text {
                attr {
                    size(20f, 20f); textAlignCenter(); lineHeight(20f)
                    fontSize(AppTypography.fs13); fontWeight600()
                    color(colors.c(colors.surface)); text("✓")
                }
            }
        }
    }
}
