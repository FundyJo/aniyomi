import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "tachiyomi.data"

    defaultConfig {
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
        val commonMain by getting {
            dependencies {
                implementation(projects.sourceApi)
                implementation(projects.domain)

                implementation(kotlinx.serialization.json)
                implementation(kotlinx.serialization.json.okio)
                implementation(kotlinx.serialization.protobuf)

                api(libs.sqldelight.coroutines)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                api(libs.sqldelight.android.driver)
                api(libs.sqldelight.android.paging)
            }
        }

        val desktopMain by getting {
            dependencies {
                api(libs.sqldelight.sqlite.driver)
            }
        }

        val iosMain by getting {
            dependencies {
                api(libs.sqldelight.native.driver)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("tachiyomi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/commonMain/sqldelight"))
            srcDirs.from(project.file("./src/commonMain/sqldelight"))
        }
        create("AnimeDatabase") {
            packageName.set("tachiyomi.mi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/commonMain/sqldelightanime"))
            srcDirs.from(project.file("./src/commonMain/sqldelightanime"))
        }
    }
}

val checkDataCommonMainImports by tasks.registering {
    group = "verification"
    description = "Fails when data commonMain Kotlin sources reference platform-only APIs."

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
            "data commonMain contains platform-only Kotlin references:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkDataCommonMainImports)
}
