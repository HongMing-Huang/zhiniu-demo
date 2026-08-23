// 知牛 ZhiNiu · PR-01 脚手架最小首页
//
// ⚠️ 本文件严格对齐 Kuikly 官方文档最小页示例（Pager / @Page / body(): ViewBuilder），
//    仅使用已由官方文档确认的 DSL 形态，未臆造未证实的 API。
//    官方示例（2026-08 调研）：
//      @Page("HelloWorld")
//      internal class HelloWorldPage : Pager() {
//          override fun body(): ViewBuilder {
//              return { attr { allCenter() }
//                       Text { attr { text("Hello Kuikly"); fontSize(14f) } } }
//          }
//      }
//    Pager / ViewBuilder / Text 等类的完整包路径以 Kuikly SDK（IDE 生成模板）为准。
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text

/**
 * 启动首页骨架页。
 * 仅用于验证 Kuikly 工程连通性（Hello 级别）；真实行情/详情/聊天页在后续 PR 填充。
 */
@Page("Home")
internal class HomePage : Pager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }
            Text {
                attr {
                    text("知牛 ZhiNiu · Kuikly 脚手架运行成功")
                    fontSize(16f)
                }
            }
        }
    }
}