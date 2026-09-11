// 知牛 · 系统外观偏好（iOS actual）
package com.zhiniu.platform

import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

/** 读取主屏 traitCollection 的外观样式。 */
actual fun systemPrefersDark(): Boolean =
    UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

/** iOS 外观变化经 traitCollectionDidChange 由宿主 ViewController 转发；此处不注册全局监听。 */
actual fun watchSystemTheme(callback: (dark: Boolean) -> Unit) {
}

/** 原生端主题由 Kuikly 视图自身着色，无 host CSS 需同步。 */
actual fun applyHostTheme(dark: Boolean) {
}
