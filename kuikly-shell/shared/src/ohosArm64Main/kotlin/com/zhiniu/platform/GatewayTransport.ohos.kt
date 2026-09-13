// 知牛 · HarmonyOS actual：GatewayTransport napi 桥预留位。
//
// 编译期：ohosArm64 无 Ktor 引擎，commonMain 已抽 GatewayTransport 接口，本文件保证源集完整可编译。
// 运行期（DevEco 环境就绪前）：所有请求返回失败 → GatewayMarketClient 的 runCatching 兜底
// 走本地确定性快照，页面不白屏（离线降级为产品既有设计）。
// 接入路径（见 docs/platform-readiness.md 方案 A/B）：
//   经 Kuikly KRBridgeModule 调 ETS @ohos.net.http（dataReceive 事件承接 SSE 行），
//   届时把 TODO 处替换为 napi 调用即可，接口签名已对齐。
package com.zhiniu.platform

internal class OhosGatewayTransport : GatewayTransport {
    override suspend fun get(url: String): String =
        throw UnsupportedOperationException("ohos napi 桥未接入：$url（走离线快照）")

    override suspend fun postJson(url: String, json: String): String =
        throw UnsupportedOperationException("ohos napi 桥未接入：$url（走离线快照）")

    override suspend fun postSse(url: String, json: String, onLine: (String) -> Unit): Boolean = false
}

actual fun createPlatformGatewayTransport(): GatewayTransport = OhosGatewayTransport()

// 鸿蒙真机/模拟器访问宿主机开发板：待 DevEco 环境验证后按需调整（模拟器通常也是独立网络命名空间）
actual fun platformDefaultGateway(): String? = null
