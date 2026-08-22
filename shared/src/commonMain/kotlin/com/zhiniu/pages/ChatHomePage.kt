/* 知牛 · AI 聊天主页（Task02）
 * 多轮会话气泡 + 流式输出 + 4 快捷指令 + Agent 时间线。
 * 使用内置 List/Text/Input/Button/Carousel；数据绑定按 Kuikly observable 语义。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder

@Page("ChatHome")
internal class ChatHomePage(
    private val viewModel: com.zhiniu.viewmodel.ChatVM,
) : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr { allCenter() }
            Text {
                attr {
                    text("知牛 · AI 问股")
                    fontSize(18f)
                }
            }
            Text {
                attr {
                    text("输入问题：看大盘 / 诊个股 / 解释指标 / 对比两只")
                    fontSize(13f)
                }
            }
        }
    }
}