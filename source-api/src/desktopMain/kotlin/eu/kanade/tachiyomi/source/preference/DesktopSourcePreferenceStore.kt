package eu.kanade.tachiyomi.source.preference

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object DesktopSourcePreferenceStores {
    fun forSource(sourceId: Long): SourcePreferenceStore {
        return PropertiesSourcePreferenceStore(defaultDirectory().resolve("source_$sourceId.properties"))
    }

    private fun defaultDirectory(): Path {
        val userHome = Path.of(System.getProperty("user.home"))
        val osName = System.getProperty("os.name").lowercase()
        val dataRoot = when {
            osName.contains("win") -> System.getenv("APPDATA")?.let(Path::of)
                ?: userHome.resolve("AppData").resolve("Roaming")
            osName.contains("mac") -> userHome.resolve("Library").resolve("Application Support")
            else -> System.getenv("XDG_DATA_HOME")?.let(Path::of) ?: userHome.resolve(".local").resolve("share")
        }
        return dataRoot.resolve("Aniyomi").resolve("source-preferences")
    }
}

class PropertiesSourcePreferenceStore(
    private val path: Path,
) : SourcePreferenceStore {
    override fun getString(key: String, default: String): String = read()[key]?.toString() ?: default

    override fun putString(key: String, value: String) {
        write { setProperty(key, value) }
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = read()[key]?.toString()?.toBooleanStrictOrNull() ?: default

    override fun putBoolean(key: String, value: Boolean) {
        write { setProperty(key, value.toString()) }
    }

    private fun read(): Properties {
        val properties = Properties()
        if (Files.exists(path)) {
            path.inputStream().use(properties::load)
        }
        return properties
    }

    private fun write(block: Properties.() -> Unit) {
        Files.createDirectories(path.parent)
        val properties = read().apply(block)
        path.outputStream().use { output -> properties.store(output, null) }
    }
}
