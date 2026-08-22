import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

android {
    namespace = "tachiyomi.domain"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    androidTarget()
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.sourceApi)
                api(kotlinx.coroutines.core)
                api(kotlinx.serialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlinx.coroutines.test)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.sourceApi)
                implementation(projects.core.common)

                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
                implementation(kotlinx.bundles.coroutines)
                implementation(kotlinx.bundles.serialization)

                implementation(libs.unifile)

                api(libs.sqldelight.android.paging)

                compileOnly(libs.compose.stablemarker)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(kotlinx.coroutines.test)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

val checkDomainCommonMainImports by tasks.registering {
    group = "verification"
    description = "Fails when domain commonMain references platform-only APIs."

    val commonMainDir = layout.projectDirectory.dir("src/commonMain/kotlin")
    inputs.dir(commonMainDir)

    doLast {
        val forbidden = listOf(
            Regex("\\bandroid\\."),
            Regex("\\bandroidx\\."),
            Regex("\\bjava\\."),
            Regex("\\bjavax\\."),
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
            "domain commonMain contains platform-only references:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkDomainCommonMainImports)
}
