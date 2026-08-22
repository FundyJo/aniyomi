plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(projects.sourceApi)
    api(libs.jsoup)
    implementation(kotlinx.coroutines.core)
}
