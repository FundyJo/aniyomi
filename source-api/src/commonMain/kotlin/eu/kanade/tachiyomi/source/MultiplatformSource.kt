package eu.kanade.tachiyomi.source

import kotlinx.serialization.Serializable

interface MediaSource {
    val id: Long
    val name: String
    val lang: String
    val capabilities: Set<SourceCapability>

    suspend fun popular(page: Int): SourcePage

    suspend fun latest(page: Int): SourcePage

    suspend fun search(page: Int, query: String, filters: SourceFilters): SourcePage

    suspend fun details(media: SourceMedia): SourceMedia

    suspend fun episodes(media: SourceMedia): List<SourceEpisode>

    suspend fun videos(episode: SourceEpisode): List<VideoSource>
}

enum class SourceCapability {
    Network,
    Cookies,
    JavaScript,
    WebView,
    Authentication,
    CustomHeaders,
    VideoResolver,
}

@Serializable
data class SourcePage(
    val entries: List<SourceMedia>,
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
data class SourceFilters(
    val values: Map<String, String> = emptyMap(),
)

@Serializable
data class VideoSource(
    val url: String,
    val quality: String,
    val headers: Map<String, String> = emptyMap(),
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

@Serializable
data class SourcePackageManifest(
    val id: String,
    val name: String,
    val version: String,
    val language: String,
    val sources: List<Long>,
    val icon: String? = null,
    val capabilities: Set<SourceCapability>,
    val checksumSha256: String,
    val signature: String,
    val hostAllowlist: Set<String>,
)
