package eu.kanade.aniyomi.desktop

import app.cash.sqldelight.db.SqlDriver
import eu.kanade.tachiyomi.source.BuiltinSourceRegistry
import eu.kanade.tachiyomi.source.GlobalSourceSearch
import eu.kanade.tachiyomi.source.network.KtorNetworkClient
import eu.kanade.tachiyomi.source.network.NetworkClient
import tachiyomi.data.Database
import tachiyomi.data.database.DesktopDatabaseDriverFactory
import tachiyomi.data.database.createAnimeDatabase
import tachiyomi.data.database.createMangaDatabase
import tachiyomi.mi.data.AnimeDatabase

class DesktopDependencyContainer(
    val directories: AppDirectories = DesktopAppDirectories(),
    val fileSystem: PlatformFileSystem = DesktopPlatformFileSystem(),
) {
    private val databaseDriverFactory = DesktopDatabaseDriverFactory(directories.data)

    val mangaDriver: SqlDriver = databaseDriverFactory.createMangaDriver()
    val animeDriver: SqlDriver = databaseDriverFactory.createAnimeDriver()

    val mangaDatabase: Database = createMangaDatabase(mangaDriver)
    val animeDatabase: AnimeDatabase = createAnimeDatabase(animeDriver)

    val networkClient: NetworkClient = KtorNetworkClient()
    val sourceRegistry: BuiltinSourceRegistry = BuiltinSourceRegistry()
    val extensionManager: DesktopExtensionManager = JarDesktopExtensionManager(directories.extensions, sourceRegistry)
    val globalSourceSearch: GlobalSourceSearch = GlobalSourceSearch(sourceRegistry)
    val libraryRepository: DesktopLibraryRepository = DesktopLibraryRepository(mangaDatabase, animeDatabase)

    val secureStorage: SecureStorage = DesktopSecureStorage()
    val externalBrowser: ExternalBrowser = DesktopExternalBrowser()
    val clipboardService: ClipboardService = DesktopClipboardService()
    val notificationService: NotificationService = DesktopNotificationService()
    val downloadEngine: DesktopDownloadEngine = DesktopDownloadEngine(directories.downloads, networkClient, notificationService)
    val filePicker: FilePicker = DesktopFilePicker()
    val platformInfo: PlatformInfo = currentPlatformInfo()

    init {
        fileSystem.ensureDirectory(directories.data)
        fileSystem.ensureDirectory(directories.cache)
        fileSystem.ensureDirectory(directories.downloads)
        fileSystem.ensureDirectory(directories.extensions)
        fileSystem.ensureDirectory(directories.logs)
    }
}
