plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

// 知牛 · 离线兜底数据代码生成（与 build.gradle.kts 同源：mock_market.json → MockMarketData.kt）
val generateMockMarketData = tasks.register("generateMockMarketData") {
    val srcFile = file("src/commonMain/data/mock_market.json")
    val outFile = File(project.buildDir, "generated/mock/kotlin/com/zhiniu/data/mock/MockMarketData.kt")
    outputs.file(outFile)
    inputs.file(srcFile)
    doLast {
        val json = srcFile.readText(Charsets.UTF_8).trim()
        val target = outFile
        target.parentFile.mkdirs()
        target.writeText(
            buildString {
                appendLine("// 本文件由 gradle 任务 generateMockMarketData 从 src/commonMain/data/mock_market.json 生成，请勿手改。")
                appendLine("package com.zhiniu.data.mock")
                appendLine()
                appendLine("/** 离线兜底数据（股票池/指数/宽度/默认值）的 JSON 原文；解析见 MockMarketFile。 */")
                appendLine("internal val MOCK_MARKET_JSON: String = \"\"\"")
                appendLine(json)
                appendLine("\"\"\".trimIndent()")
            },
            Charsets.UTF_8,
        )
    }
}

kotlin {
    ohosArm64 {
        binaries.sharedLib("shared") {
            freeCompilerArgs += "-Xadd-light-debug=enable"
            if (buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE) {
                val clangOpt = "-Os -mllvm -enable-machine-outliner=always -ffunction-sections"
                val clangFlags = "clangOptFlags.ohos_arm64=$clangOpt;clangDebugFlags.ohos_arm64=$clangOpt"
                freeCompilerArgs += "-Xoverride-konan-properties=$clangFlags"
                linkerOpts += "--pack-dyn-relocs=relr"
                linkerOpts += "--gc-sections"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generateMockMarketData.map { it.outputs.files.first().parentFile })
            dependencies {
                implementation("com.tencent.kuikly-open:core:2.25.0-2.0.21-ohos")
                implementation("com.tencent.kuikly-open:core-annotations:2.25.0-2.0.21-ohos")
                // serialization/coroutines：腾讯镜像 KBA 构建的 ohosArm64 变体（与 Kotlin 2.0.21-KBA fork 配套）
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1-KBA-003")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-KBA-002")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

dependencies {
    add("kspOhosArm64", "com.tencent.kuikly-open:core-ksp:2.25.0-2.0.21-ohos")
}
