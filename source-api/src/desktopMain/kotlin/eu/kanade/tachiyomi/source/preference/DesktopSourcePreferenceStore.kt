package eu.kanade.tachiyomi.source.preference

import com.sun.jna.platform.win32.Crypt32Util
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.Properties
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object DesktopSourcePreferenceStores {
    fun forSource(sourceId: Long): SourcePreferenceStore {
        return PropertiesSourcePreferenceStore(defaultDirectory().resolve("source_$sourceId.properties"))
    }

    fun secretStoreForSource(sourceId: Long): SourceSecretStore {
        return DpapiSourceSecretStore(defaultDirectory().resolve("source_$sourceId.secrets"))
    }

    internal fun defaultDirectory(): Path {
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

interface SourceSecretStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String): Boolean
    fun delete(key: String): Boolean
}

class DpapiSourceSecretStore(
    private val path: Path,
) : SourceSecretStore {
    private val base64 = Base64.getEncoder()
    private val decoder = Base64.getDecoder()
    private val isWindows = System.getProperty("os.name").contains("win", ignoreCase = true)

    override fun getString(key: String): String? {
        if (!isWindows) return null
        val protectedValue = read()[key]?.toString() ?: return null
        return runCatching {
            Crypt32Util.cryptUnprotectData(decoder.decode(protectedValue)).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    override fun putString(key: String, value: String): Boolean {
        if (!isWindows) return false
        return runCatching {
            val protectedValue = base64.encodeToString(Crypt32Util.cryptProtectData(value.toByteArray(Charsets.UTF_8)))
            write { setProperty(key, protectedValue) }
            true
        }.getOrDefault(false)
    }

    override fun delete(key: String): Boolean {
        return runCatching {
            write { remove(key) }
            true
        }.getOrDefault(false)
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
