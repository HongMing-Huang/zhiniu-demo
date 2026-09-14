import com.tencent.kuikly.gradle.config.KuiklyConfig
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("maven-publish")
    id("com.tencent.kuikly-open.kuikly")
}

val KEY_PAGE_NAME = "pageName"

// 知牛 · 离线兜底数据代码生成：mock_market.json（数据文件，单一事实源）→ MockMarketData.kt
// Kuikly 无跨端同步读文本资产的 API，故编译期把 JSON 内嵌为常量；改数据只需改 JSON 后重新构建。
val generateMockMarketData = tasks.register("generateMockMarketData") {
    val srcFile = file("src/commonMain/data/mock_market.json")
    val outFile = layout.buildDirectory.file("generated/mock/kotlin/com/zhiniu/data/mock/MockMarketData.kt")
    outputs.file(outFile)
    inputs.file(srcFile)
    doLast {
        val json = srcFile.readText(Charsets.UTF_8).trim()
        val target = outFile.get().asFile
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
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
    }

    js(IR) {
        nodejs() // 用于 :shared:jsNodeTest 跑 commonMain/commonTest 纯逻辑测试（无需 Android SDK）
        browser {
            webpackTask {
                outputFileName = "nativevue2.js"
            }
            commonWebpackConfig {
                output?.library = null
                devtool = "source-map"
            }
        }
        binaries.executable()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            freeCompilerArgs = freeCompilerArgs + getCommonCompilerArgs()
            isStatic = true
            license = "MIT"
        }
        extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
    }

    sourceSets {
        val commonMain by getting {
            // 离线兜底数据代码生成：src/commonMain/data/mock_market.json（单一事实源）→ MockMarketData.kt
            kotlin.srcDir(generateMockMarketData.map { it.outputs.files.first().parentFile })
            dependencies {
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
                // 知牛 shared 既有业务依赖（见 shared/ 源码：remote ViewModel data domain）
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.12")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
                implementation("io.ktor:ktor-client-okhttp:2.3.12") // createPlatformHttpClient() Android actual 引擎
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12") // createPlatformHttpClient() iOS actual 引擎
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

group = "com.zhiniu"
version = System.getenv("kuiklyBizVersion") ?: "1.0.0"

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("mavenUserName") ?: ""
                password = System.getenv("mavenPassword") ?: ""
            }
            rootProject.properties["mavenUrl"]?.toString()?.let { url = uri(it) }
        }
    }
}

ksp {
    arg(KEY_PAGE_NAME, getPageName())
}

dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:${Version.getKuiklyVersion()}") {
        add("kspAndroid", this)
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
        add("kspJs", this)
    }
}

android {
    namespace = "com.zhiniu.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
    sourceSets {
        named("main") {
            assets.srcDirs("src/commonMain/assets")
        }
    }
}

fun getPageName(): String {
    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
}

fun getCommonCompilerArgs(): List<String> {
    return listOf("-Xallocator=std")
}

fun getLinkerArgs(): List<String> {
    return listOf()
}

configure<KuiklyConfig> {
    js {
        outputName("nativevue2")
    }
}
