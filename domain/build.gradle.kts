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
        commonMain {
            dependencies {
                api(kotlinx.serialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            kotlin.srcDir("src/main/java")

            dependencies {
                implementation(projects.sourceApi)
                implementation(projects.core.common)

                implementation(platform(kotlinx.coroutines.bom))
                implementation(kotlinx.bundles.coroutines)
                implementation(kotlinx.bundles.serialization)

                implementation(libs.unifile)

                api(libs.sqldelight.android.paging)

                compileOnly(libs.compose.stablemarker)
            }
        }
        androidUnitTest {
            kotlin.srcDir("src/test/java")

            dependencies {
                implementation(libs.bundles.test)
                implementation(kotlinx.coroutines.test)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

android {
    namespace = "tachiyomi.domain"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}
