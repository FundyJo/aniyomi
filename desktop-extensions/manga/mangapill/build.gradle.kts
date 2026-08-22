plugins {
    kotlin("jvm")
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.desktopExtensions.commonJvm)
}

val bundledDependencies by configurations.creating

dependencies {
    bundledDependencies(projects.desktopExtensions.commonJvm)
}

tasks.jar {
    archiveBaseName.set("mangapill-desktop-extension")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(bundledDependencies.map { zipTree(it) })
    from(configurations.runtimeClasspath.get().filter { it.name.contains("jsoup") }.map(::zipTree))
}
