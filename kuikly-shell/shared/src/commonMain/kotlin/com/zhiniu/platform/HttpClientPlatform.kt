package com.zhiniu.platform

/**
 * 平台默认网关地址（null = 用 commonMain 的 127.0.0.1:8000）。
 * Android 返回 10.0.2.2：模拟器内 127.0.0.1 指向模拟器自身，
 * 10.0.2.2 是官方固定的宿主机别名，点图标即连上网关无需传参；
 * 真机联调仍用 ?gateway=http://<电脑局域网IP>:8000 覆盖。
 */
expect fun platformDefaultGateway(): String?
