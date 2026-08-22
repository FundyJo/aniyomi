plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.desktopExtensions.commonJvm)
    implementation(kotlinx.serialization.json)
}

val bundledDependencies by configurations.creating

dependencies {
    bundledDependencies(projects.desktopExtensions.commonJvm)
}

tasks.jar {
    archiveBaseName.set("jellyfin-desktop-extension")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(bundledDependencies.map { zipTree(it) })
    from(configurations.runtimeClasspath.get().filter { it.name.contains("jsoup") }.map(::zipTree))
}
