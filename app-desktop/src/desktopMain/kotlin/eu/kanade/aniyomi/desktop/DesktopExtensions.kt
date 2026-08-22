package eu.kanade.aniyomi.desktop

import eu.kanade.tachiyomi.source.BuiltinSourceRegistry
import eu.kanade.tachiyomi.source.MultiplatformSource
import eu.kanade.tachiyomi.source.MultiplatformSourceFactory
import eu.kanade.tachiyomi.source.SourcePackageManifest
import eu.kanade.tachiyomi.source.SourcePackageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

interface DesktopExtensionManager {
    val extensions: StateFlow<List<DesktopExtension>>

    suspend fun reload()

    suspend fun installJar(sourceJar: Path)

    suspend fun remove(extension: DesktopExtension)
}

data class DesktopExtension(
    val manifest: SourcePackageManifest?,
    val packagePath: Path,
    val status: DesktopExtensionStatus,
    val sources: List<MultiplatformSource> = emptyList(),
    val error: String? = null,
) {
    val id: String = manifest?.id ?: packagePath.fileName.toString()
    val name: String = manifest?.name ?: packagePath.fileName.toString()
    val version: String = manifest?.version ?: "unknown"
    val language: String = manifest?.language ?: "unknown"
    val type: SourcePackageType? = manifest?.type
}

enum class DesktopExtensionStatus {
    Loaded,
    Invalid,
    Failed,
}

class JarDesktopExtensionManager(
    private val extensionsDirectory: Path,
    private val sourceRegistry: BuiltinSourceRegistry,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) : DesktopExtensionManager {
    private val mutableExtensions = MutableStateFlow<List<DesktopExtension>>(emptyList())
    private val classLoaders = mutableListOf<URLClassLoader>()

    override val extensions: StateFlow<List<DesktopExtension>> = mutableExtensions.asStateFlow()

    override suspend fun reload() {
        closeClassLoaders()
        Files.createDirectories(extensionsDirectory)

        val loadedIds = mutableSetOf<String>()
        val loadedSourceIds = mutableSetOf<Long>()
        val discovered = discoverPackages().map { packagePath ->
            loadPackage(packagePath, loadedIds, loadedSourceIds)
        }

        mutableExtensions.value = discovered
        sourceRegistry.replace(discovered.flatMap { it.sources })
    }

    override suspend fun installJar(sourceJar: Path) {
        require(sourceJar.isRegularFile() && sourceJar.extension.equals("jar", ignoreCase = true)) { "Select a desktop extension JAR" }
        Files.createDirectories(extensionsDirectory)
        val target = extensionsDirectory.resolve(sourceJar.fileName.toString())
        Files.copy(sourceJar, target, StandardCopyOption.REPLACE_EXISTING)
        reload()
    }

    override suspend fun remove(extension: DesktopExtension) {
        closeClassLoaders()
        runCatching {
            if (extension.packagePath.isDirectory()) {
                Files.walk(extension.packagePath).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
            } else {
                Files.deleteIfExists(extension.packagePath)
            }
        }
        reload()
    }

    private fun discoverPackages(): List<Path> {
        if (!Files.exists(extensionsDirectory)) return emptyList()
        return Files.list(extensionsDirectory).use { paths ->
            paths
                .filter { path ->
                    (path.isRegularFile() && path.extension.equals("jar", ignoreCase = true)) ||
                        (path.isDirectory() && Files.exists(path.resolve("extension.jar")))
                }
                .sorted(compareBy { it.name.lowercase() })
                .toList()
        }
    }

    private fun loadPackage(
        packagePath: Path,
        loadedIds: MutableSet<String>,
        loadedSourceIds: MutableSet<Long>,
    ): DesktopExtension {
        val jarPath = if (packagePath.isDirectory()) packagePath.resolve("extension.jar") else packagePath
        return runCatching {
            JarFile(jarPath.toFile()).use { jar ->
                validateJarEntries(jar)
                val manifest = readManifest(jar, packagePath)
                validateManifest(manifest)
                check(loadedIds.add(manifest.id)) { "Duplicate extension id: ${manifest.id}" }

                val classLoader = URLClassLoader(arrayOf(jarPath.toUri().toURL()), javaClass.classLoader)
                val sources = try {
                    instantiateSources(classLoader, manifest)
                } catch (error: Throwable) {
                    classLoader.close()
                    throw error
                }
                check(sources.isNotEmpty()) { "Extension did not create any sources" }
                val duplicateSource = sources.firstOrNull { it.id in loadedSourceIds }
                check(duplicateSource == null) { "Duplicate source id: ${duplicateSource?.id}" }
                if (manifest.sources.isNotEmpty()) {
                    val declaredSources = manifest.sources.toSet()
                    val actualSources = sources.map { it.id }.toSet()
                    check(actualSources == declaredSources) {
                        "Manifest sources $declaredSources do not match loaded sources $actualSources"
                    }
                }
                loadedSourceIds += sources.map { it.id }
                classLoaders += classLoader
                DesktopExtension(manifest, packagePath, DesktopExtensionStatus.Loaded, sources)
            }
        }.getOrElse { error ->
            DesktopExtension(null, packagePath, DesktopExtensionStatus.Failed, error = error.message ?: error::class.simpleName)
        }
    }

    private fun readManifest(jar: JarFile, packagePath: Path): SourcePackageManifest {
        val manifestText = jar.getJarEntry(EMBEDDED_MANIFEST)?.let { entry ->
            jar.getInputStream(entry).bufferedReader().use { it.readText() }
        } ?: packagePath.takeIf { it.isDirectory() }
            ?.resolve("extension.json")
            ?.takeIf(Files::exists)
            ?.inputStream()
            ?.bufferedReader()
            ?.use { it.readText() }
        ?: packagePath.parent
            ?.resolve("${packagePath.nameWithoutExtension}.json")
            ?.takeIf(Files::exists)
            ?.inputStream()
            ?.bufferedReader()
            ?.use { it.readText() }
        ?: error("Missing $EMBEDDED_MANIFEST or extension.json")

        return try {
            json.decodeFromString(SourcePackageManifest.serializer(), manifestText)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Invalid extension manifest: ${error.message}", error)
        }
    }

    private fun validateManifest(manifest: SourcePackageManifest) {
        require(manifest.id.isNotBlank()) { "Manifest id is required" }
        require(manifest.name.isNotBlank()) { "Manifest name is required" }
        require(manifest.version.isNotBlank()) { "Manifest version is required" }
        require(manifest.language.isNotBlank()) { "Manifest language is required" }
        require(manifest.entryPoints.isNotEmpty()) { "At least one entry point is required" }
        manifest.entryPoints.forEach { entryPoint ->
            require(entryPoint.isNotBlank()) { "Entry point cannot be blank" }
            require(!entryPoint.contains('/')) { "Entry point must be a JVM class name" }
        }
    }

    private fun validateJarEntries(jar: JarFile) {
        val blockedEntry = jar.entries().asSequence().firstOrNull { entry ->
            val name = entry.name.lowercase()
            BLOCKED_NATIVE_SUFFIXES.any(name::endsWith)
        }
        require(blockedEntry == null) { "Native executable entry is not allowed: ${blockedEntry?.name}" }
    }

    private fun instantiateSources(
        classLoader: ClassLoader,
        manifest: SourcePackageManifest,
    ): List<MultiplatformSource> {
        return manifest.entryPoints.flatMap { entryPoint ->
            val instance = runCatching {
                classLoader.loadClass(entryPoint).getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            }.getOrElse { error ->
                throw IllegalArgumentException("Invalid extension entry point $entryPoint: ${error.message}", error)
            }
            when (instance) {
                is MultiplatformSource -> listOf(instance)
                is MultiplatformSourceFactory -> instance.createSources()
                else -> throw IllegalArgumentException(
                    "Entry point $entryPoint must implement MultiplatformSource or MultiplatformSourceFactory",
                )
            }
        }
    }

    private fun closeClassLoaders() {
        classLoaders.forEach { classLoader -> runCatching { classLoader.close() } }
        classLoaders.clear()
    }

    private companion object {
        const val EMBEDDED_MANIFEST = "META-INF/aniyomi-extension.json"
        val BLOCKED_NATIVE_SUFFIXES = setOf(".dll", ".exe", ".so", ".dylib", ".bat", ".cmd", ".ps1", ".sh")
    }
}
