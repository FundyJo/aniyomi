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
        commonMain {
            dependencies {
                implementation(projects.sourceApi)
                implementation(projects.domain)

                implementation(kotlinx.serialization.json)
                implementation(kotlinx.serialization.json.okio)
                implementation(kotlinx.serialization.protobuf)

                api(libs.sqldelight.coroutines)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            dependencies {
                api(libs.sqldelight.android.driver)
                api(libs.sqldelight.android.paging)
            }
        }
        desktopMain {
            dependencies {
                api(libs.sqldelight.sqlite.driver)
            }
        }
        iosMain {
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
