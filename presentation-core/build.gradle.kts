plugins {
    id("mihon.library")
    id("mihon.library.compose")
    kotlin("android")
}

android {
    namespace = "tachiyomi.presentation.core"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    api(projects.core.common)
    api(projects.i18n)

    // Compose
    implementation(composeLibs.activity)
    implementation(composeLibs.foundation)
    implementation(composeLibs.material3.core)
    implementation(composeLibs.material.icons)
    implementation(composeLibs.animation)
    implementation(composeLibs.animation.graphics)
    debugImplementation(composeLibs.ui.tooling)
    implementation(composeLibs.ui.tooling.preview)
    implementation(composeLibs.ui.util)

    implementation(androidx.paging.runtime)
    implementation(androidx.paging.compose)
    implementation(kotlinx.immutables)
}
