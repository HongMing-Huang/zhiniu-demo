package com.zhiniu.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import com.zhiniu.KuiklyRenderActivity
import org.json.JSONObject

object KRRouterAdapter : IKRRouterAdapter {

    /** 底部 Tab 级页面（互相切换：无转场动画、不堆栈——正常 App 的 Tab 行为）。 */
    private val TAB_PAGES = setOf("MarketList", "Watchlist", "AiResearch", "Profile")

    override fun openPage(context: Context, pageName: String, pageData: JSONObject) {
        val isTabSwitch = TAB_PAGES.contains(pageName)
        val current = context as? Activity
        val currentIsTab = current is KuiklyRenderActivity && TAB_PAGES.contains(current.pageName)
        KuiklyRenderActivity.start(context, pageName, pageData)
        if (isTabSwitch) {
            // Tab 切换：关掉系统默认 Activity 转场（用户反馈的「怪动画」根因）
            (current as? KuiklyRenderActivity)?.overridePendingTransition(0, 0)
            // 当前也是 Tab 页则结束自己，避免返回键在多个 Tab 间倒退
            if (currentIsTab) current?.finish()
        }
    }

    override fun closePage(context: Context) {
        (context as? Activity)?.finish()
    }
}
