import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    alias(composeLibs.plugins.jetbrains.compose)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(projects.core.platform)
                implementation(projects.data)
                implementation(projects.domain)
                implementation(projects.sourceApi)

                implementation(kotlinx.coroutines.core)
                implementation(libs.jna)
                implementation(libs.jna.platform)
                implementation(libs.bundles.coil)
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "eu.kanade.aniyomi.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Aniyomi"
            packageVersion = "0.1.0"
            vendor = "Aniyomi"
            description = "Aniyomi desktop application"
        }
    }
}
