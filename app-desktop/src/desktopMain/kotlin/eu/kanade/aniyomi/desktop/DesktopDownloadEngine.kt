package eu.kanade.aniyomi.desktop

import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourcePageImage
import eu.kanade.tachiyomi.source.VideoSource
import eu.kanade.tachiyomi.source.network.HttpHeaders
import eu.kanade.tachiyomi.source.network.NetworkClient
import eu.kanade.tachiyomi.source.network.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream

enum class DesktopDownloadKind {
    MangaChapter,
    AnimeEpisode,
}

enum class DesktopDownloadStatus {
    Queued,
    Downloading,
    Paused,
    Completed,
    Failed,
    Cancelled,
}

data class DesktopDownloadItem(
    val id: String,
    val kind: DesktopDownloadKind,
    val sourceId: Long,
    val mediaTitle: String,
    val itemTitle: String,
    val targetPath: Path,
    val status: DesktopDownloadStatus = DesktopDownloadStatus.Queued,
    val completedUnits: Int = 0,
    val totalUnits: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null,
) {
    val fraction: Float?
        get() = when {
            totalUnits > 0 -> completedUnits.toFloat() / totalUnits.toFloat()
            totalBytes != null && totalBytes > 0L -> downloadedBytes.toFloat() / totalBytes.toFloat()
            else -> null
        }
}

data class LocalChapterDownload(
    val directory: Path,
    val pages: List<Path>,
)

data class LocalEpisodeDownload(
    val file: Path,
)

class DesktopDownloadEngine(
    private val downloadRoot: Path,
    private val networkClient: NetworkClient,
    private val notificationService: NotificationService,
    concurrency: Int = 2,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(concurrency.coerceIn(1, 5))
    private val specs = mutableMapOf<String, DownloadSpec>()
    private val jobs = mutableMapOf<String, Job>()
    private val queueFile = downloadRoot.resolve("desktop-download-queue.properties")
    private val _items = MutableStateFlow(loadQueue())

    val items: StateFlow<List<DesktopDownloadItem>> = _items

    fun enqueueMangaChapter(
        sourceId: Long,
        manga: SourceMedia,
        chapter: SourceEpisode,
        pages: List<SourcePageImage>,
        headers: HttpHeaders,
    ): String {
        val id = stableId("manga", sourceId, manga.url, chapter.url)
        val target = downloadRoot.resolve("Manga").resolve(manga.title.safePathSegment()).resolve(chapter.name.safePathSegment())
        val urls = pages.sortedBy(SourcePageImage::index).mapNotNull { page -> page.imageUrl ?: page.url.takeIf(String::isNotBlank) }
        specs[id] = DownloadSpec.MangaPages(urls, headers)
        upsert(
            DesktopDownloadItem(
                id = id,
                kind = DesktopDownloadKind.MangaChapter,
                sourceId = sourceId,
                mediaTitle = manga.title,
                itemTitle = chapter.name,
                targetPath = target,
                totalUnits = urls.size,
            ),
        )
        start(id)
        return id
    }

    fun enqueueAnimeEpisode(
        sourceId: Long,
        anime: SourceMedia,
        episode: SourceEpisode,
        video: VideoSource,
    ): String {
        val id = stableId("anime", sourceId, anime.url, episode.url)
        val target = downloadRoot.resolve("Anime").resolve(anime.title.safePathSegment()).resolve("${episode.name.safePathSegment()}${video.extension()}")
        specs[id] = DownloadSpec.AnimeFile(video.url, video.headers)
        upsert(
            DesktopDownloadItem(
                id = id,
                kind = DesktopDownloadKind.AnimeEpisode,
                sourceId = sourceId,
                mediaTitle = anime.title,
                itemTitle = episode.name,
                targetPath = target,
            ),
        )
        start(id)
        return id
    }

    fun pause(id: String) {
        jobs.remove(id)?.cancel()
        update(id) { it.copy(status = DesktopDownloadStatus.Paused, error = null) }
    }

    fun resume(id: String) {
        val spec = specs[id]
        if (spec == null) {
            update(id) { it.copy(status = DesktopDownloadStatus.Failed, error = "Open the source entry and enqueue again to refresh download URLs.") }
            return
        }
        update(id) { it.copy(status = DesktopDownloadStatus.Queued, error = null) }
        start(id)
    }

    fun retry(id: String) {
        resume(id)
    }

    fun cancel(id: String) {
        jobs.remove(id)?.cancel()
        update(id) { it.copy(status = DesktopDownloadStatus.Cancelled, error = null) }
    }

    fun remove(id: String) {
        jobs.remove(id)?.cancel()
        specs.remove(id)
        _items.update { items -> items.filterNot { it.id == id } }
        persist()
    }

    fun deleteFiles(id: String) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        runCatching {
            when {
                Files.isDirectory(item.targetPath) -> Files.walk(item.targetPath).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
                }
                else -> Files.deleteIfExists(item.targetPath)
            }
        }
        remove(id)
    }

    fun findChapter(sourceId: Long, manga: SourceMedia, chapter: SourceEpisode): LocalChapterDownload? {
        val id = stableId("manga", sourceId, manga.url, chapter.url)
        val item = _items.value.firstOrNull { it.id == id && it.status == DesktopDownloadStatus.Completed } ?: return null
        if (!Files.isDirectory(item.targetPath)) return null
        val pages = Files.list(item.targetPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .sorted(Comparator.comparing(Path::getFileName))
                .toList()
        }
        return pages.takeIf { it.isNotEmpty() }?.let { LocalChapterDownload(item.targetPath, it) }
    }

    fun findEpisode(sourceId: Long, anime: SourceMedia, episode: SourceEpisode): LocalEpisodeDownload? {
        val id = stableId("anime", sourceId, anime.url, episode.url)
        val item = _items.value.firstOrNull { it.id == id && it.status == DesktopDownloadStatus.Completed } ?: return null
        return item.targetPath.takeIf { Files.isRegularFile(it) }?.let(::LocalEpisodeDownload)
    }

    private fun start(id: String) {
        if (jobs[id]?.isActive == true) return
        jobs[id] = scope.launch {
            semaphore.withPermit {
                when (val spec = specs[id]) {
                    is DownloadSpec.MangaPages -> downloadManga(id, spec)
                    is DownloadSpec.AnimeFile -> downloadAnime(id, spec)
                    null -> update(id) { it.copy(status = DesktopDownloadStatus.Paused, error = "Download URLs are not available after restart.") }
                }
            }
        }
    }

    private suspend fun downloadManga(id: String, spec: DownloadSpec.MangaPages) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        runCatching {
            Files.createDirectories(item.targetPath)
            update(id) { it.copy(status = DesktopDownloadStatus.Downloading, error = null) }
            spec.urls.forEachIndexed { index, url ->
                if (_items.value.firstOrNull { it.id == id }?.status == DesktopDownloadStatus.Paused) return
                val target = item.targetPath.resolve("%03d%s".format(index + 1, url.extensionFromUrl(".jpg")))
                if (!target.exists()) {
                    downloadToFile(url, spec.headers, target, id, null)
                }
                update(id) { current -> current.copy(completedUnits = index + 1) }
            }
            update(id) { it.copy(status = DesktopDownloadStatus.Completed, completedUnits = spec.urls.size, totalUnits = spec.urls.size, error = null) }
            notificationService.notify("Download complete", "${item.mediaTitle} • ${item.itemTitle}")
        }.onFailure { error -> fail(id, error) }
    }

    private suspend fun downloadAnime(id: String, spec: DownloadSpec.AnimeFile) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        if (spec.url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)) {
            fail(id, IllegalArgumentException("HLS episode downloads are not implemented yet."))
            return
        }
        runCatching {
            Files.createDirectories(item.targetPath.parent)
            update(id) { it.copy(status = DesktopDownloadStatus.Downloading, error = null) }
            downloadToFile(spec.url, spec.headers, item.targetPath, id, item.totalBytes)
            update(id) { it.copy(status = DesktopDownloadStatus.Completed, completedUnits = 1, totalUnits = 1, error = null) }
            notificationService.notify("Download complete", "${item.mediaTitle} • ${item.itemTitle}")
        }.onFailure { error -> fail(id, error) }
    }

    private suspend fun downloadToFile(url: String, headers: HttpHeaders, target: Path, id: String, knownTotal: Long?) {
        val response = networkClient.execute(NetworkRequest(url = url, headers = headers))
        if (response.status !in 200..299) error("HTTP ${response.status}")
        val contentLength = response.headers.get("Content-Length")?.toLongOrNull() ?: knownTotal
        val temp = target.resolveSibling("${target.name}.part")
        var itemBytes = 0L
        temp.outputStream().use { output ->
            response.body.stream().collect { chunk ->
                output.write(chunk)
                itemBytes += chunk.size
                update(id) { it.copy(downloadedBytes = it.downloadedBytes + chunk.size, totalBytes = contentLength ?: it.totalBytes) }
            }
        }
        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    private fun fail(id: String, error: Throwable) {
        update(id) { it.copy(status = DesktopDownloadStatus.Failed, error = error.message ?: error::class.simpleName.orEmpty()) }
        _items.value.firstOrNull { it.id == id }?.let { item ->
            notificationService.notify("Download failed", "${item.mediaTitle} • ${item.itemTitle}")
        }
    }

    private fun upsert(item: DesktopDownloadItem) {
        _items.update { items -> items.filterNot { it.id == item.id } + item }
        persist()
    }

    private fun update(id: String, block: (DesktopDownloadItem) -> DesktopDownloadItem) {
        _items.update { items -> items.map { item -> if (item.id == id) block(item) else item } }
        persist()
    }

    private fun loadQueue(): List<DesktopDownloadItem> {
        if (!queueFile.exists()) return emptyList()
        val properties = Properties().apply { queueFile.inputStream().use(::load) }
        return properties.getProperty("ids").orEmpty().split('|').filter(String::isNotBlank).mapNotNull { id ->
            runCatching {
                DesktopDownloadItem(
                    id = id,
                    kind = DesktopDownloadKind.valueOf(properties.getProperty("$id.kind")),
                    sourceId = properties.getProperty("$id.sourceId").toLong(),
                    mediaTitle = properties.getProperty("$id.mediaTitle"),
                    itemTitle = properties.getProperty("$id.itemTitle"),
                    targetPath = Path.of(properties.getProperty("$id.targetPath")),
                    status = properties.getProperty("$id.status").let(DesktopDownloadStatus::valueOf).let { status ->
                        if (status == DesktopDownloadStatus.Downloading || status == DesktopDownloadStatus.Queued) DesktopDownloadStatus.Paused else status
                    },
                    completedUnits = properties.getProperty("$id.completedUnits", "0").toInt(),
                    totalUnits = properties.getProperty("$id.totalUnits", "0").toInt(),
                    downloadedBytes = properties.getProperty("$id.downloadedBytes", "0").toLong(),
                    totalBytes = properties.getProperty("$id.totalBytes")?.toLongOrNull(),
                    createdAt = properties.getProperty("$id.createdAt", "0").toLong(),
                    error = properties.getProperty("$id.error")?.takeIf(String::isNotBlank),
                )
            }.getOrNull()
        }
    }

    private fun persist() {
        Files.createDirectories(downloadRoot)
        val properties = Properties()
        val snapshot = _items.value.sortedByDescending(DesktopDownloadItem::createdAt)
        properties.setProperty("ids", snapshot.joinToString("|") { it.id })
        snapshot.forEach { item ->
            properties.setProperty("${item.id}.kind", item.kind.name)
            properties.setProperty("${item.id}.sourceId", item.sourceId.toString())
            properties.setProperty("${item.id}.mediaTitle", item.mediaTitle)
            properties.setProperty("${item.id}.itemTitle", item.itemTitle)
            properties.setProperty("${item.id}.targetPath", item.targetPath.toString())
            properties.setProperty("${item.id}.status", item.status.name)
            properties.setProperty("${item.id}.completedUnits", item.completedUnits.toString())
            properties.setProperty("${item.id}.totalUnits", item.totalUnits.toString())
            properties.setProperty("${item.id}.downloadedBytes", item.downloadedBytes.toString())
            item.totalBytes?.let { properties.setProperty("${item.id}.totalBytes", it.toString()) }
            properties.setProperty("${item.id}.createdAt", item.createdAt.toString())
            item.error?.let { properties.setProperty("${item.id}.error", it) }
        }
        queueFile.outputStream().use { output -> properties.store(output, null) }
    }

    private sealed interface DownloadSpec {
        data class MangaPages(val urls: List<String>, val headers: HttpHeaders) : DownloadSpec
        data class AnimeFile(val url: String, val headers: HttpHeaders) : DownloadSpec
    }
}

private fun stableId(prefix: String, sourceId: Long, mediaUrl: String, itemUrl: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$prefix|$sourceId|$mediaUrl|$itemUrl".toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(24)
    return "$prefix-$digest"
}

private fun String.safePathSegment(): String {
    return replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Untitled" }.take(120)
}

private fun String.extensionFromUrl(default: String): String {
    val path = runCatching { URI(this).path }.getOrNull().orEmpty().substringBeforeLast('?')
    val extension = path.substringAfterLast('/', "").substringAfterLast('.', "")
    return extension.takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }?.let { ".$it" } ?: default
}

private fun VideoSource.extension(): String {
    return when {
        mimeType?.contains("mp4", ignoreCase = true) == true -> ".mp4"
        mimeType?.contains("matroska", ignoreCase = true) == true -> ".mkv"
        mimeType?.contains("webm", ignoreCase = true) == true -> ".webm"
        else -> url.extensionFromUrl(".mp4")
    }
}
