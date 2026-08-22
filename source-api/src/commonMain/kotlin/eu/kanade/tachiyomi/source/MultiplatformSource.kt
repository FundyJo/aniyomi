package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.network.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface MultiplatformSource {
    val id: Long
    val name: String
    val lang: String
    val capabilities: Set<SourceCapability>
}

interface MultiplatformSourceFactory {
    fun createSources(): List<MultiplatformSource>
}

interface MultiplatformAnimeSource : MultiplatformSource {
    fun getAnimeFilters(): List<SourceFilter> = emptyList()
    suspend fun getPopularAnime(page: Int): SourcePage<SourceMedia>
    suspend fun getLatestAnime(page: Int): SourcePage<SourceMedia>
    suspend fun searchAnime(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia>
    suspend fun getAnimeDetails(anime: SourceMedia): SourceMedia
    suspend fun getEpisodeList(anime: SourceMedia): List<SourceEpisode>
    suspend fun getVideoList(episode: SourceEpisode): List<VideoSource>
}

interface MultiplatformMangaSource : MultiplatformSource {
    fun getMangaFilters(): List<SourceFilter> = emptyList()
    suspend fun getPopularManga(page: Int): SourcePage<SourceMedia>
    suspend fun getLatestManga(page: Int): SourcePage<SourceMedia>
    suspend fun searchManga(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia>
    suspend fun getMangaDetails(manga: SourceMedia): SourceMedia
    suspend fun getChapterList(manga: SourceMedia): List<SourceEpisode>
    suspend fun getPageList(chapter: SourceEpisode): List<SourcePageImage>
}

@Deprecated("Use MultiplatformAnimeSource or MultiplatformMangaSource")
interface MediaSource : MultiplatformAnimeSource

enum class SourceCapability {
    Popular,
    Latest,
    Search,
    Manga,
    Anime,
    Video,
    Network,
    Cookies,
    JavaScript,
    WebView,
    Authentication,
    CustomHeaders,
    VideoResolver,
}

@Serializable
data class SourcePage<T>(
    val entries: List<T>,
    val hasNextPage: Boolean,
)

@Serializable
data class SourceMedia(
    val url: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val status: SourceMediaStatus = SourceMediaStatus.Unknown,
)

@Serializable
data class SourceEpisode(
    val url: String,
    val name: String,
    val dateUpload: Long = 0,
    val number: Float = -1f,
)

@Serializable
data class SourcePageImage(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null,
)

sealed class SourceFilter(open val name: String) {
    data class Header(override val name: String) : SourceFilter(name)
    data class Separator(override val name: String = "") : SourceFilter(name)
    data class Select(override val name: String, val values: List<String>, val state: Int = 0) : SourceFilter(name)
    data class Text(override val name: String, val state: String = "") : SourceFilter(name)
    data class CheckBox(override val name: String, val state: Boolean = false) : SourceFilter(name)
    data class TriState(override val name: String, val state: Int = STATE_IGNORE) : SourceFilter(name) {
        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }
    data class Group(override val name: String, val values: List<SourceFilter>) : SourceFilter(name)
    data class Sort(override val name: String, val values: List<String>, val selection: Selection? = null) : SourceFilter(name) {
        data class Selection(val index: Int, val ascending: Boolean)
    }
}

@Serializable
data class SourceFilters(
    val values: Map<String, String> = emptyMap(),
)

@Serializable
data class VideoSource(
    val url: String,
    val quality: String,
    val headers: HttpHeaders = HttpHeaders.Empty,
    val subtitles: List<SourceSubtitle> = emptyList(),
    val audioTracks: List<SourceTrack> = emptyList(),
    val backupUrls: List<String> = emptyList(),
    val mimeType: String? = null,
)

@Serializable
data class SourceSubtitle(
    val url: String,
    val language: String? = null,
    val label: String? = null,
    val mimeType: String? = null,
)

@Serializable
data class SourceTrack(
    val id: String,
    val label: String,
    val language: String? = null,
)

enum class SourceMediaStatus {
    Unknown,
    Ongoing,
    Completed,
    Licensed,
    PublishingFinished,
    Cancelled,
    OnHiatus,
}

enum class SourcePackageType {
    @SerialName("anime")
    Anime,

    @SerialName("manga")
    Manga,

    @SerialName("mixed")
    Mixed,
}

@Serializable
data class SourcePackageManifest(
    val id: String,
    val name: String,
    val version: String,
    val language: String,
    val entryPoints: List<String>,
    val type: SourcePackageType,
    val sources: List<Long> = emptyList(),
    val icon: String? = null,
    val capabilities: Set<SourceCapability> = emptySet(),
    val checksumSha256: String? = null,
    val signature: String? = null,
    val hostAllowlist: Set<String> = emptySet(),
)
