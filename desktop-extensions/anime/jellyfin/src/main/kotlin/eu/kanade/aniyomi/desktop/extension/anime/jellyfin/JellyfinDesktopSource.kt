package eu.kanade.aniyomi.desktop.extension.anime.jellyfin

import eu.kanade.aniyomi.desktop.extension.DesktopBrowserHeaders
import eu.kanade.aniyomi.desktop.extension.buildUrl
import eu.kanade.aniyomi.desktop.extension.formatBytes
import eu.kanade.aniyomi.desktop.extension.getJsonText
import eu.kanade.aniyomi.desktop.extension.parseDateMillis
import eu.kanade.aniyomi.desktop.extension.toSourceStatus
import eu.kanade.tachiyomi.source.MultiplatformAnimeSource
import eu.kanade.tachiyomi.source.MultiplatformSourceFactory
import eu.kanade.tachiyomi.source.SourceCapability
import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceFilter
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourcePage
import eu.kanade.tachiyomi.source.SourceSubtitle
import eu.kanade.tachiyomi.source.VideoSource
import eu.kanade.tachiyomi.source.network.HttpHeaders
import eu.kanade.tachiyomi.source.network.KtorNetworkClient
import eu.kanade.tachiyomi.source.network.NetworkClient
import eu.kanade.tachiyomi.source.preference.DesktopSourcePreferenceStores
import eu.kanade.tachiyomi.source.preference.SourcePreferenceStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.net.URI
import java.security.MessageDigest

class JellyfinDesktopSourceFactory : MultiplatformSourceFactory {
    override fun createSources(): List<JellyfinDesktopSource> = (1..3).map { index ->
        JellyfinDesktopSource(index.toString())
    }
}

class JellyfinDesktopSource(
    private val suffix: String = "1",
    private val networkClient: NetworkClient = KtorNetworkClient(),
    private val preferences: SourcePreferenceStore = DesktopSourcePreferenceStores.forSource(sourceId(suffix)),
) : MultiplatformAnimeSource {
    override val id: Long = sourceId(suffix)
    override val name: String = "Jellyfin${if (suffix == "1") "" else " ($suffix)"}"
    override val lang: String = "all"
    override val capabilities: Set<SourceCapability> = setOf(
        SourceCapability.Anime,
        SourceCapability.Popular,
        SourceCapability.Latest,
        SourceCapability.Search,
        SourceCapability.Video,
        SourceCapability.Network,
        SourceCapability.CustomHeaders,
        SourceCapability.Authentication,
    )

    override fun getAnimeFilters(): List<SourceFilter> = listOf(
        SourceFilter.Header("Configure host_url, user_id, api_key, and optionally library_id in source preferences."),
        SourceFilter.Text("library_id", libraryId),
    )

    override suspend fun getPopularAnime(page: Int): SourcePage<SourceMedia> {
        val response = getItemList(catalogUrl(page, sortBy = "SortName", sortOrder = "Ascending"))
        return SourcePage(response.items.map { it.toSourceMedia }, SEASONS_FETCH_LIMIT * page < response.totalRecordCount)
    }

    override suspend fun getLatestAnime(page: Int): SourcePage<SourceMedia> {
        val response = getItemList(catalogUrl(page, sortBy = "DateCreated,SortName", sortOrder = "Descending"))
        return SourcePage(response.items.map { it.toSourceMedia }, SEASONS_FETCH_LIMIT * page < response.totalRecordCount)
    }

    override suspend fun searchAnime(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia> {
        val response = getItemList(
            catalogUrl(page, sortBy = "SortName", sortOrder = "Ascending") + "&SearchTerm=${query.encodeQuery()}",
        )
        return SourcePage(response.items.map { it.toSourceMedia }, SEASONS_FETCH_LIMIT * page < response.totalRecordCount)
    }

    override suspend fun getAnimeDetails(anime: SourceMedia): SourceMedia {
        val item = getItem(itemIdFromUrl(anime.url))
        return item.toSourceMedia.copy(title = anime.title.ifBlank { item.name })
    }

    override suspend fun getEpisodeList(anime: SourceMedia): List<SourceEpisode> {
        val itemId = itemIdFromUrl(anime.url)
        val itemType = itemTypeFromUrl(anime.url)
        if (itemType == "Movie") {
            return listOf(getItem(itemId).toSourceEpisode)
        }
        val response = getItemList(
            buildUrl(
                hostUrl,
                "/Users/$userId/Items",
                listOf(
                    "ParentId" to itemId,
                    "Recursive" to "true",
                    "SortBy" to "SortName",
                    "SortOrder" to "Ascending",
                    "IncludeItemTypes" to "Episode,Movie",
                    "Fields" to "Overview,MediaSources,PremiereDate,DateCreated,RunTimeTicks",
                ),
            ),
        )
        return response.items.map { it.toSourceEpisode }
    }

    override suspend fun getVideoList(episode: SourceEpisode): List<VideoSource> {
        val itemId = itemIdFromUrl(episode.url)
        val item = getItem(itemId)
        val size = item.mediaSources.firstOrNull()?.size?.formatBytes()?.takeIf(String::isNotBlank)
        val subtitles = item.mediaSources.flatMap { source -> source.mediaStreams }
            .filter { it.type == "Subtitle" && it.deliveryUrl != null }
            .map { stream ->
                SourceSubtitle(
                    url = absoluteJellyfinUrl(stream.deliveryUrl.orEmpty()),
                    language = stream.language,
                    label = stream.displayTitle,
                    mimeType = stream.codec?.let { "text/$it" },
                )
            }
        return listOf(
            VideoSource(
                url = buildUrl(hostUrl, "/Videos/$itemId/stream", listOf("static" to "true", "api_key" to apiKey)),
                quality = listOfNotNull("Source", size).joinToString(" - "),
                headers = authHeaders,
                subtitles = subtitles,
            ),
        )
    }

    private suspend fun getItemList(url: String): ItemListDto {
        ensureConfigured()
        return json.decodeFromString(networkClient.getJsonText(url, authHeaders))
    }

    private suspend fun getItem(itemId: String): ItemDto {
        ensureConfigured()
        return json.decodeFromString(
            networkClient.getJsonText(
                buildUrl(hostUrl, "/Users/$userId/Items/$itemId", listOf("Fields" to "Overview,Genres,Studios,MediaSources")),
                authHeaders,
            ),
        )
    }

    private fun catalogUrl(page: Int, sortBy: String, sortOrder: String): String {
        val startIndex = (page - 1).coerceAtLeast(0) * SEASONS_FETCH_LIMIT
        return buildUrl(
            hostUrl,
            "/Users/$userId/Items",
            listOf(
                "StartIndex" to startIndex.toString(),
                "Limit" to SEASONS_FETCH_LIMIT.toString(),
                "Recursive" to "true",
                "SortBy" to sortBy,
                "SortOrder" to sortOrder,
                "IncludeItemTypes" to "Movie,Season,Series,BoxSet",
                "ImageTypeLimit" to "1",
                "EnableImageTypes" to "Primary",
                "ParentId" to libraryId.takeIf(String::isNotBlank),
                "Fields" to "Overview,Genres,Studios,MediaSources",
            ),
        )
    }

    private val ItemDto.toSourceMedia: SourceMedia
        get() {
            val descriptionParts = buildList {
                overview?.htmlToText()?.takeIf(String::isNotBlank)?.let(::add)
                genres.takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { add("Genres: $it") }
                studios.takeIf { it.isNotEmpty() }?.joinToString(", ") { studio -> studio.name }?.let { add("Studios: $it") }
            }
            return SourceMedia(
                url = "/Users/$userId/Items/$id#$type",
                title = displayTitle,
                thumbnailUrl = "$hostUrl/Items/$id/Images/Primary",
                description = descriptionParts.joinToString("\n\n").takeIf(String::isNotBlank),
                status = if (type == "Movie") "Ended".toSourceStatus() else seriesStatus.toSourceStatus(),
            )
        }

    private val ItemDto.toSourceEpisode: SourceEpisode
        get() = SourceEpisode(
            url = "/Users/$userId/Items/$id#$type",
            name = episodeTitle,
            dateUpload = parseDateMillis(premiereDate ?: dateCreated),
            number = indexNumber?.toFloat() ?: if (type == "Movie") 1f else -1f,
        )

    private val ItemDto.displayTitle: String
        get() = when {
            type == "Season" && !seriesName.isNullOrBlank() && name != seriesName -> "$seriesName $name"
            else -> name
        }

    private val ItemDto.episodeTitle: String
        get() = buildString {
            if (type == "Episode") {
                append("Episode")
                indexNumber?.let { append(' ').append(it) }
                if (name.isNotBlank()) append(" - ")
            }
            append(name)
        }.trim()

    private val hostUrl: String
        get() = preferences.getString(HOST_URL_KEY, "").trimEnd('/')

    private val userId: String
        get() = preferences.getString(USER_ID_KEY, "")

    private val apiKey: String
        get() = preferences.getString(API_KEY_KEY, "")

    private val libraryId: String
        get() = preferences.getString(LIBRARY_ID_KEY, "")

    private val authHeaders: HttpHeaders
        get() = DesktopBrowserHeaders
            .set("Accept", "application/json")
            .set("Authorization", "MediaBrowser Client=\"Aniyomi Desktop\", Device=\"Desktop\", DeviceId=\"aniyomi-desktop\", Version=\"1.0.0\", Token=\"$apiKey\"")

    private fun ensureConfigured() {
        require(hostUrl.isNotBlank()) { "Set Jellyfin host_url in source preferences." }
        require(userId.isNotBlank()) { "Set Jellyfin user_id in source preferences." }
        require(apiKey.isNotBlank()) { "Set Jellyfin api_key in source preferences." }
    }

    private fun itemIdFromUrl(url: String): String = URI(url).path.substringAfterLast('/')

    private fun itemTypeFromUrl(url: String): String = URI(url).fragment.orEmpty()

    private fun absoluteJellyfinUrl(url: String): String = if (url.startsWith("http")) url else hostUrl + "/" + url.trimStart('/')

    private fun String.htmlToText(): String = Jsoup.parseBodyFragment(replace("<br>", "br2n")).text().replace("br2n", "\n")

    private fun String.encodeQuery(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8)

    @Serializable
    private data class ItemListDto(
        @SerialName("Items") val items: List<ItemDto> = emptyList(),
        @SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
    )

    @Serializable
    private data class ItemDto(
        @SerialName("Name") val name: String = "",
        @SerialName("Type") val type: String = "",
        @SerialName("Id") val id: String = "",
        @SerialName("SeriesName") val seriesName: String? = null,
        @SerialName("SeriesStatus") val seriesStatus: String? = null,
        @SerialName("Overview") val overview: String? = null,
        @SerialName("Genres") val genres: List<String> = emptyList(),
        @SerialName("Studios") val studios: List<StudioDto> = emptyList(),
        @SerialName("IndexNumber") val indexNumber: Int? = null,
        @SerialName("PremiereDate") val premiereDate: String? = null,
        @SerialName("DateCreated") val dateCreated: String? = null,
        @SerialName("MediaSources") val mediaSources: List<MediaDto> = emptyList(),
    )

    @Serializable
    private data class StudioDto(
        @SerialName("Name") val name: String = "",
    )

    @Serializable
    private data class MediaDto(
        @SerialName("Size") val size: Long? = null,
        @SerialName("MediaStreams") val mediaStreams: List<MediaStreamDto> = emptyList(),
    )

    @Serializable
    private data class MediaStreamDto(
        @SerialName("Type") val type: String = "",
        @SerialName("Codec") val codec: String? = null,
        @SerialName("Language") val language: String? = null,
        @SerialName("DisplayTitle") val displayTitle: String? = null,
        @SerialName("DeliveryUrl") val deliveryUrl: String? = null,
    )

    private companion object {
        const val HOST_URL_KEY = "host_url"
        const val USER_ID_KEY = "user_id"
        const val API_KEY_KEY = "api_key"
        const val LIBRARY_ID_KEY = "library_id"
        const val SEASONS_FETCH_LIMIT = 20
        val json = Json { ignoreUnknownKeys = true }

        fun sourceId(suffix: String): Long {
            val key = "jellyfin${if (suffix == "1") "" else " ($suffix)"}/all/1"
            val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            return (0..7).map { (bytes[it].toLong() and 0xffL) shl (8 * (7 - it)) }.reduce(Long::or) and Long.MAX_VALUE
        }
    }
}
