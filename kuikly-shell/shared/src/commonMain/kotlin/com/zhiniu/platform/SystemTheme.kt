// 知牛 · 系统外观偏好（expect）
// H5(jsMain) actual：matchMedia('(prefers-color-scheme: dark)') + change 监听；
// 其他端：由各平台壳注入（暂无 actual 时按浅色处理）。
package com.zhiniu.platform

/** 系统是否偏好深色。 */
expect fun systemPrefersDark(): Boolean

/**
 * 监听系统深浅色切换（仅 SYSTEM 模式下使用）。
 * 平台不支持时回调永不触发。
 */
expect fun watchSystemTheme(callback: (dark: Boolean) -> Unit)

/**
 * 同步给宿主（H5：<html data-theme="...">，供 host CSS 切换 hover 色等）。
 * 非 Web 端 no-op。
 */
expect fun applyHostTheme(dark: Boolean)
