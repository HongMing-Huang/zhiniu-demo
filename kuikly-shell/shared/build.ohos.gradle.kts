plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
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
