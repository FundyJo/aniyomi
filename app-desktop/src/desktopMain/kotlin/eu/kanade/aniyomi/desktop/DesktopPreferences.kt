package eu.kanade.aniyomi.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

enum class DesktopThemeMode {
    System,
    Light,
    Dark,
}

class DesktopPreferences(
    private val path: Path,
) {
    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<DesktopThemeMode> = _themeMode

    fun setThemeMode(mode: DesktopThemeMode) {
        write { setProperty(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private fun readThemeMode(): DesktopThemeMode {
        return read().getProperty(KEY_THEME_MODE)?.let { value ->
            DesktopThemeMode.entries.firstOrNull { it.name == value }
        } ?: DesktopThemeMode.System
    }

    private fun read(): Properties {
        val properties = Properties()
        if (path.exists()) {
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

private const val KEY_THEME_MODE = "appearance.themeMode"
