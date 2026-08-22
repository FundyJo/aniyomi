import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget()
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinx.serialization.json)
                api(kotlinx.coroutines.core)
                api(libs.ktor.client.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlinx.coroutines.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.core.common)
                api(libs.injekt)
                api(libs.rxjava)
                api(libs.jsoup)
                api(libs.okhttp.core)
                implementation(libs.ktor.client.okhttp)
                api(aniyomilibs.nanohttpd)
                api(libs.preferencektx)

                // Workaround for https://youtrack.jetbrains.com/issue/KT-57605
                implementation(kotlinx.coroutines.android)
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "eu.kanade.tachiyomi.source"

    defaultConfig {
        consumerProguardFile("consumer-proguard.pro")
    }
}

val checkSourceApiCommonMainImports by tasks.registering {
    group = "verification"
    description = "Fails when source-api commonMain references platform-only APIs."

    val commonMainDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    inputs.dir(commonMainDir)

    doLast {
        val forbidden = listOf(
            Regex("\\bandroid\\."),
            Regex("\\bandroidx\\."),
            Regex("\\bjava\\."),
            Regex("\\bjavax\\."),
            Regex("\\bio\\.reactivex\\."),
            Regex("\\bokhttp3\\."),
            Regex("\\borg\\.jsoup\\."),
            Regex("\\brx\\."),
        )
        val violations = commonMainDir.asFileTree.matching { include("**/*.kt") }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (forbidden.any { it.containsMatchIn(line) }) {
                        "${file.relativeTo(projectDir)}:${index + 1}: $line"
                    } else {
                        null
                    }
                }
            }
        check(violations.isEmpty()) {
            "source-api commonMain contains platform-only references:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkSourceApiCommonMainImports)
}
