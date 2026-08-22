package eu.kanade.aniyomi.desktop

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

interface AppDirectories {
    val data: Path
    val cache: Path
    val downloads: Path
    val logs: Path
}

interface PlatformFileSystem {
    fun ensureDirectory(path: Path): Path
}

interface SecureStorage {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
    fun delete(key: String): Boolean
}

interface ExternalBrowser {
    fun open(uri: URI): Boolean
}

interface ClipboardService {
    fun setText(text: String)
}

interface NotificationService {
    fun notify(title: String, message: String)
}

interface FilePicker {
    fun pickDirectory(title: String): Path?
    fun pickFile(title: String): Path?
    fun saveFile(title: String, defaultFileName: String): Path?
}

data class PlatformInfo(
    val name: String,
    val version: String,
    val architecture: String,
)

class DesktopAppDirectories : AppDirectories {
    private val userHome = Path.of(System.getProperty("user.home"))
    private val osName = System.getProperty("os.name").lowercase()

    override val data: Path = when {
        osName.contains("win") -> System.getenv("APPDATA")?.let(Path::of)
            ?: userHome.resolve("AppData").resolve("Roaming")
        osName.contains("mac") -> userHome.resolve("Library").resolve("Application Support")
        else -> System.getenv("XDG_DATA_HOME")?.let(Path::of) ?: userHome.resolve(".local").resolve("share")
    }.resolve("Aniyomi")

    override val cache: Path = when {
        osName.contains("win") -> System.getenv("LOCALAPPDATA")?.let(Path::of)
            ?: userHome.resolve("AppData").resolve("Local")
        osName.contains("mac") -> userHome.resolve("Library").resolve("Caches")
        else -> System.getenv("XDG_CACHE_HOME")?.let(Path::of) ?: userHome.resolve(".cache")
    }.resolve("Aniyomi")

    override val downloads: Path = userHome.resolve("Downloads").resolve("Aniyomi")

    override val logs: Path = cache.resolve("logs")
}

class DesktopPlatformFileSystem : PlatformFileSystem {
    override fun ensureDirectory(path: Path): Path = Files.createDirectories(path)
}

class DesktopSecureStorage : SecureStorage {
    override fun read(key: String): String? = null

    override fun write(key: String, value: String): Boolean = false

    override fun delete(key: String): Boolean = true
}

class DesktopExternalBrowser : ExternalBrowser {
    override fun open(uri: URI): Boolean {
        return runCatching {
            if (!Desktop.isDesktopSupported()) return false
            Desktop.getDesktop().browse(uri)
            true
        }.getOrDefault(false)
    }
}

class DesktopClipboardService : ClipboardService {
    override fun setText(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

class DesktopNotificationService : NotificationService {
    override fun notify(title: String, message: String) {
        if (!SystemTray.isSupported()) return
        runCatching {
            val image = Toolkit.getDefaultToolkit().createImage(ByteArray(0))
            val trayIcon = TrayIcon(image, "Aniyomi")
            val tray = SystemTray.getSystemTray()
            tray.add(trayIcon)
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO)
            tray.remove(trayIcon)
        }
    }
}

class DesktopFilePicker : FilePicker {
    override fun pickDirectory(title: String): Path? {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        return openDialog(title, FileDialog.LOAD).also {
            System.setProperty("apple.awt.fileDialogForDirectories", "false")
        }
    }

    override fun pickFile(title: String): Path? = openDialog(title, FileDialog.LOAD)

    override fun saveFile(title: String, defaultFileName: String): Path? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.file = defaultFileName
        dialog.isVisible = true
        return dialog.directory?.let { directory -> dialog.file?.let { file -> Path.of(directory, file) } }
    }

    private fun openDialog(title: String, mode: Int): Path? {
        val dialog = FileDialog(null as Frame?, title, mode)
        dialog.isVisible = true
        return dialog.directory?.let { directory -> dialog.file?.let { file -> Path.of(directory, file) } }
    }
}

fun currentPlatformInfo(): PlatformInfo = PlatformInfo(
    name = System.getProperty("os.name"),
    version = System.getProperty("os.version"),
    architecture = System.getProperty("os.arch"),
)
