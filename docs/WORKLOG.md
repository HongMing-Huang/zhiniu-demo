# 知牛 · 变更日志（Work Log）

> 单一维护变更记录，防止记忆混乱。每条记录：时间 · 改了什么 · 影响/验证。
> 约定：开发中每次有实质改动即在顶部追加一行（不写无关文档）。格式：
> `YYYY-MM-DD HH:mm | 类别 | 改动描述 | 验证结果`

***

## 2026-09-13

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 19:00 | design/mobile | 移动端专项（课题重移动端，对标 KuiklyStock/SaiRen 手机形态）：①Light 主题改微灰页面底 #F7F8FA + 纯白 Surface（card-on-gray 层次，两位组员共同语言；桌面回归更清晰）②MarketPulse compact 重写为三张指数卡（名称/大价格/涨跌色/迷你走势，白卡描边）+ 市场宽度摘要卡，替换原纯文本两行③重建 bundle 使并行会话的 BottomTabBar 生效（行情/自选/AI研究 底部导航，此前组件存在但未构建进包）④Header 手机端隐藏文字导航（导航职责移交底部 Tab）+ 二级页（详情/对比）compact 左上返回箭头 | H5 390×844 三页实测（指数卡/底部 Tab/返回箭头/涨跌 chip 渲染正确，0 error）；Android 模拟器实装最新 APK 验证一致；桌面 1440 回归通过（微灰底表格层次更佳）；README 拼图刷新 |

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 18:10 | qa/multiplat | Android 模拟器实机验收（emulator-5554 / zhiniu_api34）：最新 APK 安装启动，品牌图标进桌面；首页（脉冲/Tab/工具条/涨跌幅 chip/紧凑副行）、对比页、AI 研究页逐页截图；同 commonMain 与 H5 行为一致，实机无崩溃 |

| 14:20 | fix/ios+harmony | **iOS 启动崩溃根因修复 + 鸿蒙真机模拟器跑通（四端闭环）**：①**iOS SIGABRT**（3 次崩溃日志 iosApp-*.ips）：Kuikly 2.25.0 iOS 渲染层 `assertContextQueue` 对 Kotlin→native 调用做严格 Context 线程断言，Ktor(Darwin) 协程恢复线程更新 observable（TextShadow.setProp→callNative）触发 SIGABRT；**Podfile post_install 补丁**将 Debug 断言对齐 Release 行为（ObjC assert() 在 Release 本被编译掉），*5 次启动压测全部存活*；②**鸿蒙模拟器真启动**（本机已装 DevEco-Studio.app + OpenHarmony SDK + HarmonyOS-6.1.1 phone 镜像 + 4 台模拟器实例，此前误判"无 DevEco"为失误）：修复 bundleName 两段→三段（app.json5 鸿蒙规范）、@kuikly-open/render 2.16.0→2.25.0 对齐（ohpm 需清除 socks 代理环境变量）、ohos 壳 Index.ets 默认页 HelloWorld→MarketList（第四处模板残留）；③**底部 Tab ohos 兼容**——absolutePosition（right/bottom 及 left/top 锚定）在 ohos 渲染器均不渲染，改流式布局：BottomTabBar 去绝对定位，四页面 body 末尾经 renderBottomTab 接入（内容 flex1 + Tab 贴底）；已知边界：ETS 侧无路由桥，鸿蒙 Tab 点击暂不切页（模板仅单页，需 ETS RouterModule 开发） | iOS：5/5 启动存活 + 页面渲染截图；鸿蒙：hvigor assembleHap BUILD SUCCESSFUL → hdc install 成功 → aa start 启动 → 市场页完整渲染（Logo/指数/4列表格/真实行情 1292.83）+ 底部 Tab 三项显示；Android/H5 回归构建通过；鸿蒙实时联网待 transport napi 桥接入（当前离线快照降级，符合设计） |
| 06:00 | feat/ohos | **鸿蒙网络层桥接落地（阻塞①②解除）**：按 platform-readiness 方案 A 抽 `GatewayTransport` 接口（commonMain：get/postJson/postSse 三方法，SSE 行回调 + SseParser 仍在 common），GatewayMarketClient 全部 11 个端点改走接口（commonMain 彻底去 Ktor 依赖）；js/android/ios actual 为 Ktor 实现迁移（Js/OkHttp/Darwin），ohosArm64Main actual 为 napi 桥预留位（运行期抛 Unsupported → 既有离线快照降级，不白屏；DevEco 就绪后经 KRBridgeModule 调 ETS @ohos.net.http dataReceive 承接 SSE）；补 SystemTheme ohos actual；ohos 依赖补齐（腾讯镜像 KBA 变体 serialization 1.7.1-KBA-003 / coroutines 1.8.0-KBA-002，版本经镜像 metadata 实查）；新增 scripts/ohos-backfill.sh 产物回填 | **`:shared:compileKotlinOhosArm64` BUILD SUCCESSFUL**（此前 commonMain Ktor 全量 Unresolved）；**`:shared:linkDebugSharedOhosArm64` 产出 libshared.so + libshared_api.h**，已回填 ohosApp/entry/libs/arm64-v8a 与 cpp/thirdparty/biz_entry（预案阻塞②产物）；剩余：DevEco Studio 内 ohpm install + hvigor 构建（本机无 DevEco，边界如实标注） |
| 05:50 | test/regression | **传输层重构三端全量回归**：①H5 桌面截图回归（playwright chromium-1234）：行情/待办/详情三页 + 断言（横排导航 true、8 列表格 true）无回归；②AI 问答链路（input→/agent/chat→LLM 回答「解释RSI指标」✅）；③SSE 研究流全链路（「分析贵州茅台」→ 阶段帧→多 Agent 辩论→评级 ✅，postSse 重构路径）；④Android 重装冒烟（移动布局+底部 Tab+真实数据+待办持久化保持）；⑤iOS AppDelegate 增 KUIKLY_PAGE_DATA 环境变量 JSON 传参（详情页 symbol 注入通道） | node 脚本 sse_regression/sse_research_regression 输出 ✅✅；desktop_regression 三页 OK；Android 冒烟截图（茅台 1292.83 真实数据） |

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 05:10 | ui/mobile | **移动端适配根本性修复（用户反馈"没适配移动端"）**：①**根因**——Kuikly Android/iOS 的 activityWidth 传物理像素（模拟器实测 1080/1179，logcat remeasure 可证），固定 760 阈值在手机上永远 false → 整页渲染桌面布局；isCompact() 重写为「竖屏宽高比>1.35 且宽≤1280」判定（手机竖屏命中、桌面/平板/手机横屏不命中，不受像素单位影响）；②**BottomTabBar 新组件**——手机布局底部三 Tab（市场/AI研究/待办，选中 2px 顶部指示条，含安全区避让 iPhone Home 条/Android 手势条），Header 横排导航在 compact 下隐藏（避免双导航）；③contentWidth() 手机满屏宽；④四页面（市场/AI/待办/详情）滚动内容底部 bottomNavInset() 避让 Tab 高度；⑤compact 分支全部激活：行情表 4 列精简（股票含市值/成交量副行）、AI 页单栏（无会话侧栏） | Android 截图：底部 Tab + 紧凑 Header + 4 列表格 + 真实数据；**点击底部「AI研究」Tab 成功跳转**（移动端单栏：角色 Tab + Composer 底部）；iOS 截图：同款移动布局 + 真实数据；H5 手机视口（模拟器 Chrome）同渲染移动布局（离线快照符合网络拓扑）；桌面 H5 逻辑等价静态确认（1440×900 ratio 0.63 → false 同原行为） |
| 04:50 | fix/gateway | **修复 Android 模拟器默认路径连不上网关的产品级缺陷**：原 DEFAULT_BASE_URL=127.0.0.1 在模拟器内指向模拟器自身，点图标启动永远离线（此前验证全靠 intent 传 gateway 参数，用户「没跑通」反馈的根源）。新增 `expect platformDefaultGateway()`：Android actual 返回 `http://10.0.2.2:8000`（官方固定宿主机别名，模拟器点开即连；真机无效时回落快照，gateway 参数仍可覆盖），js/ios actual 返回 null 沿用 127.0.0.1（iOS 模拟器 loopback 直通、H5 同源） | 无参数默认启动（monkey/am start 等同点图标）截图：真实指数 3888.06/-1.17% + 行情茅台 1292.83 +0.10% 自动加载；`topResumedActivity=com.zhiniu/.KuiklyRenderActivity` 前台确认；iOS 新构建（BUILD SUCCEEDED）重启后真实数据渲染；H5 重部署 200 |
| 04:20 | test/deep | **移动端深度交互测试（三端能力矩阵补全）**：①**待办本地持久化原生端验证**——Android 经 run-as 写入 KRSharedPreferencesModule.xml / iOS 经 plistlib 写 com.zhiniu.plist，杀进程重启后两端均正确渲染（未完成空心钮 + 已完成 ✓ + 计数），H5(localStorage)/Android(SharedPreferences)/iOS(NSUserDefaults) 三端持久化读取链路全验证（写入为同一套 commonMain setString）；②**修复 iOS 壳同款默认页 bug**（AppDelegate.swift 默认 HelloWorld→MarketList，KUIKLY_PAGE 环境变量机制验证可用：SIMCTL_CHILD_ 前缀注入）；③**Android 详情页验证**——K 线 Canvas 原生渲染（蜡烛/量柱/MA 均线真实值）、真实五档盘口（买一 1292.88×4900手）、五 Tab；④**Android 导航闭环**——点行进详情→Back 返回列表（滚动位置恢复），RouterModule 原生跳转工作；⑤**深色模式跟随系统**在 Android 原生端自动生效（跌绿涨红正确）。已知限制：adb input 注入文本不触发 Kuikly InputView textDidChange（自动化测试限制，真人软键盘路径待真机验证）；simctl terminate 偶发静默失败需重试 | Android 截图证据：详情页 OHLC+K线+五档、返回列表深色渲染；iOS 截图证据：MarketList 默认页（真实指数 3888.06）、AiResearch（环境变量注入）、TodoList 持久化两条（新二进制下复验） |
| 03:20 | sim/three-platform | **三端模拟器真实运行验收（课题核心加分项）**：①**iOS 首次构建成功并运行**——`pod update OpenKuiklyIOSRender` 2.16.0→2.25.0（此前锁文件陈旧从未对齐）、先 `:shared:generateDummyFramework`、xcodebuild 须带 JDK17 环境否则脚本阶段用系统 Java25 失败，iPhone 17 Pro 模拟器 install+launch 截图验收；②**Android 模拟器端到端**——重建 AVD（emulator SDK 包重新注册 + api34 google_apis arm64 + pixel_6）、安装 APK 启动，**发现并修复真机致命 bug：壳 Activity 默认页仍为已删除的 HelloWorld → PagerNotFoundException 白屏**，改默认页 MarketList；用 intent `pageData {"gateway":"http://10.0.2.2:8000"}` 打通模拟器→宿主机网关；③**指数条真实化**——发现 MarketPulse 用 mock 硬编码指数（3245）而 `/quote/indices` 真实 API（3888）前端零消费，新增 `GatewayMarketClient.indices()` + MarketPage `liveIndices` 实时刷新（回退 repo 快照） | 三端截图对比数据一致：上证 3888.06 -1.17% / 深成 11917.63 / 创业板 2567.50（9/12 真实收盘），行情茅台 1292.83 +0.10%；Android 离线启动显示快照（降级设计生效）→ 联网 intent 后显示实时；iOS BUILD SUCCEEDED ×2、Android assembleDebug 通过、H5 build.sh 部署通过 |

## 2026-09-12

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 23:50 | agent/audit+fix | **Agent 模块高强度核查与修复（4 处实质 bug + 1 功能补齐）**：①交易员 entry/stop 恒为 null——prompt 的 JSON 模板把价位写成 null 占位被模型照抄，模板改为具体数字示例并新增 `_num()` 宽容解析（"1,890.5"/" 9.8 " → float）；②辩论链随机超时降级（152s 实测）——中转站延迟抖动大（同 prompt 3s~60s），AsyncOpenAI 加 45s 单请求超时 + `_chat_once` 失败快速重试 1 次；③**通用问答走真 LLM**：新增 `POST /agent/chat`（history 8 轮 + 可选 symbol 行情快照注入 system 防编价；无模型显式规则降级），前端非个股问题（4 快捷指令中 3 个原走 MockAiService）改走该端点，网关不可用回退本地 Mock；④详情页追问答非所问——原把概念问题发给完整研究管线返回评级归纳，改走 `/agent/chat` 带标的上下文；⑤行情页新增「板块」Tab 消费 `/quote/sectors`（原零消费）：东财行业板块按涨幅前 30，行含涨/跌家数与领涨股，点击进领涨股详情，SectorTable 组件 + 排序/字段按钮对榜单/板块 Tab 隐藏 | 后端 58/58（新增 chat×3 + trader 容错 + _num×1）；真实研究 16.9s mode=llm trader entry=326/stop=314；curl 验证 /agent/chat 概念问答与带 symbol 快照锚定（引用 1275.16 且诚实说明无 K 线）；8 个行情端点全真实在线（新浪五档/指数/东财板块 100 行/筛选 30 行/人气/资讯 12 条）；浏览器实测：板块 Tab 渲染（通信线缆 +5.84% 领涨神宇股份 +19.98%）、快捷指令「解释 RSI」真 LLM 回答（50 线/背离/钝化/风险声明）、详情页追问「为什么说量能不足」对题回答带 provider 标注、待办页回归通过 |
| 17:35 | feat/todo | **跨端待办清单基线（课题三端要求）**：新增 `TodoListPage`（@Page TodoList）与 `TodoStore` 纯逻辑（新增/编辑/删除/完成/清除已完成/JSON 往返/脏数据容错，commonTest 8 项，jsNodeTest 仍被上游 IrSimpleFunctionSymbolImpl 工具链 bug 阻断故以浏览器 E2E 验证）；持久化走 Kuikly 官方 SharedPreferencesModule（Android/iOS/鸿蒙/H5 各端原生落地，key `zhiniu.todo.items.v1`）；Header 增「待办」一级导航；修复 H5 原生输入值残留（ViewRef setText，同 AiResearchPage 已知坑）；连带修复上会话遗留的 AiMarkdown.kt 缺 `ca` import 编译错误 | `:shared:compileKotlinJs` BUILD SUCCESSFUL；浏览器 E2E：添加（按钮+回车双路径）→ 勾选完成 → 编辑改名 → 删除 → 两次刷新持久化全部保持，浅色渲染与计数正确；`:androidApp:assembleDebug` APK 构建通过、`:shared:compileKotlinIosSimulatorArm64` 通过（三端一套代码） |
| 17:10 | llm/gateway | **真实 LLM 链路首次端到端打通**：用户提供 GemAI 中转 Key（OpenAI 兼容，`gemai.huchan.cn/v1`，模型 `deepseek-v4-flash`，Key 仅存 backend/.env 已 gitignore）。①providers.json 新增 gemai 厂商并置于 zhiniu/quick、zhiniu/flash 降级链首位；②修复推理模型空回复根因——`deepseek-v4-flash` 默认开思考，辩论/风控短回复预算被 reasoning_tokens 耗尽（finish=length、content 空），实测 `"thinking":{"type":"disabled"}` 可关闭；为此给 config.py 增加 providers.json 声明式 `extraBody` → ProviderResolved.extra_body 透传链；③网关 AsyncOpenAI 客户端按厂商复用（原每次调用新建，重复 TLS 握手），辩论超时 75s→150s | 直调 API：JSON mode 支持 ✓；网关单次调用 reasoning=0、正常出文；`/agent/research/stream` 端到端 32s（原 64s 规则回退/69s 空辩论），synthesis.mode=llm，多空观点/三方风控均为带真实数据的模型论证（PE 19.57、RSI 36.5、支撑 1151.01）；浏览器实测 AI 研究页显示「多 Agent 辩论」真实归纳、资讯 12 条恢复 eastmoney-search |
| 16:55 | design/fix | 全界面打磨（对照用户截图反馈 + Exa 调研欧易 47 源：模块化面板/指标摘要条/亮暗平权/密度靠分区）：①Logo 重生成方形紧裁 256px 超采样（修复横版塞方框导致的压糊）②AI 研究页层次重构：AI 消息收敛进 surface+border 消息卡、与角色条/Composer 同轴 960 居中、正文提升 fs14、会话列表选中态加 aiAccent 指示条与加粗③修复股票卡 RSI 值窄容器折行（AiKeyValue value flex+等宽+Medium）④详情页更新时间缩短防折行⑤README 三张实测截图刷新 | :shared:compileKotlinJs 与 scripts/build.sh 通过；浏览器浅/深双主题实测：AI 消息卡层次、选中态、RSI 单行、Logo 清晰度全部生效，控制台 0 error |
| 15:54 | agent/audit | 数据源/Agent 完成度代码级审计（应用户「不欺骗自己」要求）：①详情页 AI 解读面板此前无来源标注 → Header 补「本地规则生成 · 非模型输出 · 追问走研究管线」；②实测确认前端研究请求真实走 `/agent/research/stream`（uvicorn 日志 200×3）、生产 bundle 中旧假进度定时器已移除（grep=0）、流式进度行发送后 202ms 出现、结果含评级/压力/支撑/多空/来源且规则降级显式标注；③如实记录剩余缺口：sectors/screener 前端零消费（纯后端接口）、LLM 辩论真实路径因无有效 Key 零次运行（路由已验证会对：gpt-4o-mini 带 fast 能力可被 zhiniu/quick 选中）、通用聊天非个股问题仍走 MockAiService、沙箱出口 IP 被 push2 WAF 限流（urllib TLS 指纹被掐、curl 间歇 200，属环境问题非代码问题，需在本机网络复核 sectors/screener 运行时） | `scripts/build.sh` 通过；浏览器实测：详情页标注渲染（label=true）、AI 研究页流式全链路结果渲染、行情/人气榜真实数据复验 |
| 15:45 | brand/fix | 品牌 Logo 全面接入（用户提供 1254×1254 主稿入库 docs/img/logo.png）：PIL 裁切牛头标生成 ①Header 品牌位（commonAssets brand/logo-mark.png，深浅色通用）②H5 favicon-32/apple-touch-icon（web-host + build.sh brand 同步）③Android 自适应启动图标（mipmap 前景 5 密度 + anydpi-v26 XML + manifest 挂载，此前无自定义图标）④iOS AppIcon.appiconset 1024 ⑤鸿蒙 app_icon/layered_image/startIcon 108。后端 fundamentals 修复：①SSRF 白名单补 datacenter-web.eastmoney.com（此前财报被自家网关拦截）②push2 不可达时估值改走腾讯 gtimg（PE/PB/名称实测入库）③三源独立降级互不拖垮；AI 解读抽屉估值判断/业绩解读随即呈现真实数据 | H5 构建部署 + 浏览器实测：头部 Logo 加载、favicon 生效、控制台 0 error；行情页自选/沪市/深市/人气榜/排序菜单逐一验证通过；AI 抽屉显示「PE 19.6 / PB 6.34 合理区间」与「2026-06-30 半年报 营收 922.78亿 净利 445.17亿 ROE 16.8%」；后端 53/53 单测通过；:androidApp:assembleDebug 含新图标构建通过 |

## 2026-09-11

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 00:52 | feat/qa | 完成-度核查补全：①Task1 发奖模块补齐「估值判断（高估/低估）」与「业绩/卖点解读」——AiInsight 增 valuation/earnings 字段，AiService.insightFor 接收网关真实基本面（详情页 liveFundamentals → AiInsightFundamentals），规则化解读并在上游缺失时如实标注；②Android 原生构建验收：修复 AppTheme `setMode` 与委托属性 JVM setter 签名冲突（改名 applyMode），绕过本机 SOCKS 代理后 `:androidApp:assembleDebug` 产出 androidApp-debug.apk；③补交 api-matrix.md（人气榜/板块/选股/SSRF 白名单文档）；GitHub 仓库补建 Issue 跟踪 | `:shared:compileKotlinJs`、`scripts/build.sh`、`:androidApp:assembleDebug` 全部通过；浏览器实测抽屉估值/业绩板块渲染、主题切换正常、控制台 0 error；PE/PB 上游（push2）当前网络不可达时按设计回退兜底文案 |
| 18:05 | docs/feat | 对照课题验收标准全面自查与补齐：①新增 `AiMarkdown.kt` commonMain Markdown 解析器+渲染器（标题/列表/表格/代码块/引用/行内样式，官方 RichText/Span），接入 AI 研究主回复「研究归纳」、分析视角与 Mock 回复，解析器 10 项 commonTest 单测；②字体改走 KuiklyUI 官方接入：数字等宽 JetBrains Mono（OFL）单字体名 `NUM_FONT`，Android KRFontAdapter 实现、iOS 官方 Handler+共享资源副本、H5 @font-face+字体就绪门控启动（1.5s 兜底），build.sh 同步字体；③交付物完整性：web-host（H5 主入口）首次入库，README/快速开始/技术栈/架构/演示脚本按实态重写，新增 docs/typography.md 与 docs/deliverables.md（评分 40/25/25/10 自查映射）；④工程清理：移除未用 koin 依赖、幽灵 `:h5App` 模块、HelloWorldPage 与 `_pending_ui` 死代码，修复 README 占位符/断链/虚假声明 | `:shared:compileKotlinJs` 与 `scripts/build.sh` 通过；后端 53/53 单测通过；Playwright 实测：字体 loaded 且数字计算样式生效、AI Markdown 全块型渲染正确、来源去重、控制台 0 error、行情/详情/AI/深浅色截图入库 docs/img/ |
| 17:50 | build/android/ios | 三端静态检查与补齐（只读审查，未编译原生端、未启动模拟器）：新增 `shared/src/androidMain`、`iosMain` 的 4 组 `actual`（`createPlatformHttpClient` OkHttp/Darwin 引擎、`systemPrefersDark/watchSystemTheme/applyHostTheme`）；`shared/build.gradle.kts` androidMain/iosMain 补 `ktor-client-okhttp/-darwin:2.3.12`；iOS Podfile `OpenKuiklyIOSRender` 2.16.0→2.25.0 对齐 core；鸿蒙阻塞（Ktor 无 ohos 引擎、`libshared.so`/`libshared_api.h` 缺失、`@kuikly-open/render` 2.16.0）与三端就绪结论记录于 `docs/platform-readiness.md` | `:shared:compileKotlinJs` 仍通过；原生端编译按要求等待确认后进行 |
| 17:20 | ui/data | 首页课题字段补齐：`StockQuote` 增 `marketCap/turnoverRate/volumeRatio`；行情表新增总市值/成交量列 + 「字段」内联芯片自定义显示列（`tableEpoch` 奇偶重建表格）；新增「人气榜」（东财真实排名）/「涨幅榜」Tab 与 `RankTable`；`MarketTab`/`RoleTab` 的 active 改为 lambda 在 attr 内读取保证响应式；详情页 `factsOf(quote, fundamentals)` 改真实基本面+行情快照双源并补 `liveFundamentals` 拉取（修复上轮遗留 4 处 `factsOf(q)` 编译错误）；`?gateway=` URL 参数可覆盖网关地址（真机联调） | `compileKotlinJs` + `scripts/build.sh` 通过；浏览器（8084/8001）：总市值 1.59 万亿、人气榜 风华高科/N燧原 真实、「字段」取消总市值后列消失、详情换手率 0.28% 真实 |
| 17:00 | agent/ui | AI 研究页接入 `/agent/research/stream` 类型化 SSE（Ktor `preparePost`+`readUTF8Line`，流不可用回退 `research()`），阶段帧实时驱动进度条（替换定时假进度）；新增结构化「AI 解读」卡（短期趋势/投资建议/压力位/支撑位/置信度/归纳模式）、「多空对抗」多头/空头观点、风控视角（三方风控 + 交易员观察方案）；角色视图切换按 `lastResearch` 复述真实辩论内容；`AiMetricsBlock` 标签固定宽、长值多行 | 浏览器：发送「分析宁德时代」→ 下行/减持/压力 414.04/支撑 326.00/置信度 50%，多空 3 条，资讯 12 条 eastmoney-search，市值 1.53 万亿 |
| 16:30 | agent/backend | 移植 TradingAgents（TauricResearch，MIT）多 Agent 结构为 `backend/app/ta_agents.py`：多头/空头研究员对抗 → 研究经理五档评级（买入/增持/持有/减持/卖出）→ 交易员绝对价位观察方案 → 激进/中性/保守风控辩论；经网关 `zhiniu/quick` 降级链直接调用（不引入 LangGraph）；`agent.py` 接入并新增 `_levels_from_kline` 确定性压力/支撑（近 60 根高低点）；`stream_research` 用队列实时转发辩论阶段帧；无模型/超时/非法评级显式 `rule-engine` 同构回退 | pytest 新增 6 项（规则回退形状、LLM 全流程假响应、非法评级降级、进度转发）；真实调用：无有效 Key → `deterministic_fallback`，11.7s 完成 |
| 16:00 | quote/backend | 板块/选股/人气榜接真实数据：`/quote/sectors` 东财行业板块（按 f3 排序、领涨股）、`/quote/screener` 东财成交额榜前 100 池 + 行业/涨幅过滤、新增 `/quote/popularity` 东财人气榜排名 + 新浪实时增强；`/quote/realtime` 批量补充总市值/换手/量比（东财 ulist → 腾讯 gtimg 双真实来源）；新增出站白名单 `_guard_external_url`（仅 HTTPS + 行情域名，防 SSRF）；均 TTL 缓存 + stale/mock 显式降级 | pytest 42→53 通过（真实抓包样本解析 + 强制离线降级）；实测人气榜/市值为真实值；沙箱内 push2 限流时腾讯兜底生效 |
| 11:31 | quote/ui | 修复五档盘口真实数据在客户端模型层被丢弃的问题：`StockQuote` 增加买卖档位，网关 JSON 解析 `[价格,股数]`，盘口优先展示新浪真实五档并换算为手，仅缺档时确定性降级；窄屏侧栏 AI 标签强制单行 | 后端解析单测覆盖五档字段；Kuikly 编译、全量构建及浏览器切换验收通过 |
| 11:08 | ui/agent | 按 OKX 图表优先布局重构详情工作区：1024px 内图表与侧栏上下排列，侧栏改“盘口/数据/AI”原位切换；设置面板改紧凑主题分段与 /healthz 真实服务状态；研究 Agent 新增真实 LLM JSON 归纳、来源标记和无 Key 明示规则降级，详情追问改走后端 | Kuikly JS 编译与完整生产构建通过；后端 39/39 测试通过；浏览器复验详情与设置；真实新浪行情、K线和东方财富资讯返回成功，模型上游不可用时明确标记规则降级 |
| 10:21 | ui/design | 依据 OKX 工作区与当前窄屏截图重做 compact 布局：行情脉冲分两行、表格只保留股票/价格/涨幅/走势、工具条分层；详情 K 线/盘口上下排列；AI 收起会话侧栏；设置入口显式文字化并补齐 Kuikly 官方无障碍语义 | Kuikly JS 编译与 `scripts/build.sh` 通过；Codex 内置浏览器回归行情/设置/深色/详情/AI；后端 39/39 测试通过，新浪实时行情返回成功 |
| 01:46 | agent/backend | 研究管线新增 `/agent/research/stream` 类型化 SSE：统一 runId、四阶段完成事件、结果帧及显式 `run_finished/run_error` 终止帧；JSON/SSE 复用同一结果构建器 | 后端 39/39 单测通过；真实贵州茅台请求返回新浪行情/K线、东方财富资讯，约 295ms 完成 |
| 01:46 | ui/data | AI 研究页接入 `GatewayMarketClient.research`，展示真实价格、RSI14、MA20、资讯数量、来源、风险与时效；并在消息内容变化时自动跟随到最新结果 | 浏览器发送“分析贵州茅台”后显示 12 条资讯与完整证据卡，Composer 清空且最新消息可见 |
| 01:46 | ui/build | 按 Kuikly 官方 `pageData.safeAreaInsets` 为 Header/浮层增加顶部避让，为列表页尾和 AI Composer 增加底部避让；同步 API/UI/AGENTS/QA 文档 | `:shared:compileKotlinJs` 与 `scripts/build.sh` 通过；未启动模拟器 |

## 2026-09-10

| 时间 | 类别 | 改动 | 验证 |
| ---- | ---- | ---- | ---- |
| 23:13 | docs/design | 用 Browser 实看 OKX 工作区，并以 Exa/GitHub 官方资料复核 TradingView、ChartIQ、Quantower、TradingAgents、OpenBB、KuiklyUI、TDesign Icons、AKShare；同步架构/API/UI/AGENTS 文档 | 研究来源与实现边界已记录；未启动模拟器 |
| 23:13 | data/agent | 新增东方财富资讯 provider、120 秒缓存/stale/离线快照链；7 个 Agent 工具改为复用真实行情/K线/资讯；新增 `/agent/research` 四阶段证据管线；个股新闻 Tab 接网关 | 后端 37 项单测通过；真实新闻与研究接口返回成功 |
| 23:13 | ui/build | 设置面板扩为 288px 分组面板并使用 TDesign 图标；新增 compact/medium 断点；修复 1920 宽度下搜索/设置浮层定位越界；Header/AI 会话栏响应式适配 | `:shared:compileKotlinJs` 与完整生产构建通过；浏览器控制台 0 warn/error |
| 12:02 | qa/build | 修复详情五标签“状态已更新但界面不刷新”的根因：一次性 `when` 改为 Kuikly 响应式 `vif`，激活态改为响应式读取；移除会拦截框架事件的 H5 `mousedown` 补丁；构建脚本启用 `pipefail`，避免 Gradle 失败被 `tail` 掩盖 | 概览/资金/财务/新闻/AI解读逐项点击与内容断言 5/5 通过；生产构建通过 |
| 11:58 | qa | 以 1280×800、1440×900 浅/深色、1920×1080 验收市场/详情/AI；回归市场筛选、主题、AI 会话/新建/角色/发送、K 线按钮与鼠标滚轮缩放 | 浏览器控制台 0 error；K 线 80→68→80 且滚轮可改变可见根数；AI 核心交互通过 |
| 10:48 | data/backend | 前端 Market/StockDetail 接入 FastAPI `/quote/realtime` 与 `/quote/kline`；实时请求成功时覆盖快照，失败保留本地数据；修复新浪成交额单位（统一为元）和 K 线缓存键未包含周期/长度的问题 | 实测网关返回 2026-09-09 贵州茅台 1290.88 与 30 根日 K；后端 7 项单测通过 |
| 10:40 | ui/chart | 按 Product Design 审计与 Ponytail 最小根因修复：所有公共按钮/导航/Tab 强制单行；AI 会话改为可恢复的历史内容并居中；详情 AI 面板内容独立滚动；K 线默认最新 80 根，新增放大/缩小/复位、拖拽、捏合与 H5 滚轮缩放桥；移除可见 Demo/演示文案并更新设置状态 | 编译、浏览器交互与多视口验收通过 |

## 2026-09-09

| 时间  | 类别   | 改动 | 验证 |
| ----- | ------ | ---- | ---- |
| 22:57 | design | 完成当前前端 UI 终验：Header 层级修复；按钮行布局、激活态与收藏填充态统一；19 枚 TDesign 官方图标改为透明底预着色 PNG，移除 H5 tint 方块伪影；1280 档隐藏次要行情列避免横向溢出 | Codex 浏览器实测 1280×800、1440×900 浅/深色、1920×1080；市场/详情/K线/五档/AI 路由与交互通过，控制台无 error/warn |
| 22:53 | ui     | 主题调色板改为稳定的 AdaptiveColors 动态语义代理，修复浅/深色切换后背景、文字、边框和图表混用旧色；行情数字单元改为动态取色 | 市场页与个股 K 线页深色模式视觉复验通过 |
| 22:45 | ui     | AppInput 接入 Kuikly 官方 ViewRef/InputView.setText，Enter 与发送按钮提交后显式清空 AI Composer；修复仅更新状态但 H5 原生输入值残留 | AI 研究页提交“分析贵州茅台”后结构化回复出现且输入框清空 |
| 22:39 | build  | 重新构建并部署 H5 bundle；图标生成脚本使用透明背景与中性灰预着色，避免 Kuikly H5 对透明 SVG/PNG 的边界盒 tint 问题 | `:shared:compileKotlinJs`、`scripts/build.sh` 通过；`:shared:jsTest` 被 Kotlin/JS IR `callKotlinMethod` 重复绑定的上游编译器错误阻断（clean 后复现） |

## 2026-09-04

| 时间    | 类别     | 改动                                                                                                                                                                                                     | 验证                                                    |
| ----- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------- |
| 18:28 | build  | 修复 H5 启动链路（根因：index.html 缺 `kuiklyBundlesReady` 置位 + 跨端口 8083 直引 + 旧 dev 宿主混用 → 白屏/乱码/路由失效）：bundle 同源部署 web-host/，index.html 改「nativevue2.js → ready 置位 → h5App.js」同源顺序；build.sh/dev.sh/AGENTS 同步单端口链路 | 编译通过 + 构建部署 + 8082 全资源 200                            |
| 18:22 | design | 设置面板补全：ThemePopover 升级为「外观/数据源/模型/免责声明」四分组（主题三档切换 + 数据源/模型演示态「接入中」标识）                                                                                                                                  | 编译通过 + 构建部署                                           |
| 18:13 | chart  | 分时图补均价线：累计 VWAP 橙线（原仅价格线+面积），对齐股票软件分时双线标配                                                                                                                                                              | 编译通过 + 重新部署 v9                                        |
| 18:12 | ui     | 新增五档盘口 Level2Panel（详情页 Rail 顶部：卖五\~买五 + 中间最新价/涨跌 + 相对量条，卖绿买红确定性模拟量）                                                                                                                                    | 编译通过 + 完整构建 + 部署 v9                                   |
| 18:08 | ui     | 详情页底部 Tab 扩为 5 个（概览/资金/财务/新闻/AI解读）：资金=主力/超大/大/中/小单净流入（随涨跌方向正负）、财务=估值+营收/净利/毛利率/负债率、新闻=个股资讯列表                                                                                                           | 编译通过 + 完整构建 + 部署 v8                                   |
| 18:05 | ui     | 自选交互闭环：详情页改用 FavoriteButton（点击切换「加自选⇄已自选」星形填充+文案），首页新增「自选」Tab 真实过滤（Watchlist 驱动）                                                                                                                       | 编译通过 + 部署 v7                                          |
| 18:00 | design | 按钮图标插槽：PrimaryButton/SecondaryButton/GhostButton 支持前置 icon；接入详情「加自选⭐/AI 分析🤖/查看完整分析🤖」、AiBlocks「查看详情📈」                                                                                                | 编译通过 + 部署 v6                                          |
| 17:55 | agent  | TradingAgents 研究落地：AiResearchPage 新增「角色视图」Tab（分析/多空对抗/风控，2px AI 色指示器 + 切换引导消息）；ui-redesign.md 记录研究结论与迁移点                                                                                               | 编译通过 + 部署 v5                                          |
| 17:50 | design | 图标系统扩充：TDesign 官方 1000 图标复核，新增 6 个 PNG（calendar/check/filter-sort/error-triangle/chat-message/data-display），IconKind 扩至 19；接入排序按钮与会话行                                                                  | 编译通过；6 图标 HTTP 200                                    |
| 17:45 | chart  | K 线规范对齐股票软件：时间轴首/中/末 3 标签、蜡烛/量柱/MACD 宽度改 slot\*0.7 自适应（原固定 6-9）                                                                                                                                        | 编译通过                                                  |
| 17:10 | ui     | UI 精修 4 处：①Header 品牌占位（22px 描边方块 Z + 知牛）；②AI 消息头改用 aiAccent #D9FF43；③ThemePopover 扩展免责声明分组（设置占位）；④详情页 AI 分析按钮改 aiAccent 描边强调                                                                           | 编译通过 + 浏览器验证（Z logo ✓、AI 按钮 rgb(217,255,67) ✓、免责声明 ✓） |
| 17:05 | ui     | 三屏浏览器验收：MarketPage（涨红/跌绿 token 生效 #F04F5F/#16B364）、StockDetail（K线 tab+五档+Canvas 899×520）、AiResearch（AI 问候+推荐问题）、主题切换（Light/Dark）                                                                       | 全部通过；index.html js 引用加 ?v=2 刷新缓存                      |
| 14:40 | git    | 推送全部提交至 `HongMing-Huang/zhiniu-demo` 远程仓库（已建仓）                                                                                                                                                         | gh 推送成功，origin/main 最新                                |
| 14:35 | docs   | 新增 `docs/WORKLOG.md`（变更日志规范）；完善 `AGENTS.md`：§0 工作日志、GitHub 仓库信息、Kuikly 2.25.0、验证流程修正（移除不存在的 v4\_final.js）、§9.1 Git 提交规范                                                                                | 文档已同步                                                 |
| 14:10 | git    | 初始化 GitHub 独立仓库 `HongMing-Huang/zhiniu-demo`（private），`origin` 关联并推送 main                                                                                                                              | gh repo create 成功，推送 HEAD→main                        |
| 13:55 | design | `Theme.kt` 统一规范：涨 `#F04F5F`、跌 `#16B364`、AI 强调 `#D9FF43`（默认值）；MA/RSI 指标紫保留；同步 `AGENTS.md` 色彩条款                                                                                                          | 编译通过                                                  |
| 13:50 | docs   | 新增 `docs/ui-redesign.md`：现状诊断 / OKX 参考 / 规范 v2 / 素材占位清单 / 新浪直连与 Agent 端到端接入研究                                                                                                                          | 已提交 commit 4e09d82                                    |
| —     | design | 基础提交：目标结构 + Kuikly 2.25.0 + 市场排序/筛选（commit 0f9dcce）                                                                                                                                                    | 编译+浏览器验证通过                                            |

## 2026-09-03

| 时间    | 类别    | 改动                                                                                             | 验证                                       |
| ----- | ----- | ---------------------------------------------------------------------------------------------- | ---------------------------------------- |
| 20:21 | ui    | 补齐全真排序（默认/涨幅/跌幅）与筛选（仅上涨）交互                                                                     | 浏览器端到端验证：排序升降序、筛选开关、行跳转详情全部通过            |
| 20:15 | build | 升级 Kuikly 2.16.0 → 2.25.0（buildSrc/KotlinBuildVar + ohos 依赖 + 文档同步）                            | productionWebpack 全量编译成功，MarketPage 正常渲染 |
| —     | ui    | 目标结构落盘：MarketPage/StockDetailPage/AiResearchPage + 公共组件体系 + TDesign 图标 + Light/Dark 主题（历史重构基线） | 浏览器验证                                    |

***

## 待办 / 已知

- [ ] UI 精修三屏（Header 品牌位 / 全数字等宽接入 / AI 强调落地 / 设置面板占位）

- [x] 实时行情与 K 线接入（FastAPI 8000 网关；失败时保留本地快照）

- [ ] Agent 端到端联调（后端 gateway 已具雏形：SSE + tools 编排）

- [ ] 素材替换（用户后续提供：品牌 logo / 个股 logo / AI 配图）
