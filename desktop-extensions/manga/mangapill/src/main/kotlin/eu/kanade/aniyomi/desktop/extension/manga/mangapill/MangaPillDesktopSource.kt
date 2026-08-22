package eu.kanade.aniyomi.desktop.extension.manga.mangapill

import eu.kanade.aniyomi.desktop.extension.DesktopBrowserHeaders
import eu.kanade.aniyomi.desktop.extension.absoluteUrl
import eu.kanade.aniyomi.desktop.extension.buildUrl
import eu.kanade.aniyomi.desktop.extension.getDocument
import eu.kanade.aniyomi.desktop.extension.toSourceStatus
import eu.kanade.tachiyomi.source.MultiplatformMangaSource
import eu.kanade.tachiyomi.source.SourceCapability
import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceFilter
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourcePage
import eu.kanade.tachiyomi.source.SourcePageImage
import eu.kanade.tachiyomi.source.network.KtorNetworkClient
import eu.kanade.tachiyomi.source.network.NetworkClient
import org.jsoup.nodes.Element
import java.net.URI
import java.util.Locale

class MangaPillDesktopSource(
    private val networkClient: NetworkClient = KtorNetworkClient(),
) : MultiplatformMangaSource {
    override val id: Long = 7645603772455389384L
    override val name: String = "MangaPill"
    override val lang: String = "en"
    override val capabilities: Set<SourceCapability> = setOf(
        SourceCapability.Manga,
        SourceCapability.Popular,
        SourceCapability.Latest,
        SourceCapability.Search,
        SourceCapability.Network,
    )

    private val baseUrl = "https://mangapill.com"
    private val headers = DesktopBrowserHeaders.set("Referer", "$baseUrl/")

    override fun getMangaFilters(): List<SourceFilter> = listOf(
        SourceFilter.Header("Ignored when using text search"),
        SourceFilter.Separator(),
        SourceFilter.Select("Status", STATUS_FILTERS.map { it.first }),
        SourceFilter.Select("Type", TYPE_FILTERS.map { it.first }),
        SourceFilter.Group("Genres", GENRES.map { SourceFilter.TriState(it) }),
    )

    override suspend fun getPopularManga(page: Int): SourcePage<SourceMedia> {
        val document = networkClient.getDocument("$baseUrl/", headers)
        val entries = document.select("div:has(h4:contains(Trending)) > .grid > div:not([class])")
            .mapNotNull(::mangaFromGridElement)
        return SourcePage(entries, hasNextPage = false)
    }

    override suspend fun getLatestManga(page: Int): SourcePage<SourceMedia> {
        val document = networkClient.getDocument("$baseUrl/chapters", headers)
        val entries = document.select(".grid > div:not([class])").mapNotNull(::mangaFromGridElement)
        return SourcePage(entries, hasNextPage = false)
    }

    override suspend fun searchManga(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia> {
        val document = networkClient.getDocument(searchUrl(page, query, filters), headers)
        val entries = document.select(".grid > div:not([class])").mapNotNull(::mangaFromGridElement)
        val hasNextPage = document.selectFirst("a.btn.btn-sm") != null
        return SourcePage(entries, hasNextPage)
    }

    override suspend fun getMangaDetails(manga: SourceMedia): SourceMedia {
        val document = networkClient.getDocument(absoluteUrl(baseUrl, manga.url), headers)
        val genres = document.select("a[href*=genre]").eachText().filter(String::isNotBlank)
        val description = document.select("div.container > div:first-child > div:last-child > div:nth-child(2) > p").text()
        val statusText = document.select("div.container > div:first-child > div:last-child > div:nth-child(3) > div:nth-child(2) > div").text()
        val thumbnailUrl = document.select("div.container > div:first-child > div:first-child > img").firstOrNull()
            ?.let(::imageUrlFromElement)
            ?: manga.thumbnailUrl
        return manga.copy(
            thumbnailUrl = thumbnailUrl,
            description = buildString {
                append(description)
                if (genres.isNotEmpty()) {
                    if (isNotBlank()) append("\n\n")
                    append("Genres: ").append(genres.joinToString(", "))
                }
            }.ifBlank { manga.description.orEmpty() },
            status = statusText.toSourceStatus(),
        )
    }

    override suspend fun getChapterList(manga: SourceMedia): List<SourceEpisode> {
        val document = networkClient.getDocument(absoluteUrl(baseUrl, manga.url), headers)
        return document.select("#chapters > div > a").map { element ->
            SourceEpisode(
                url = relativeUrl(element.absUrl("href")),
                name = element.text(),
            )
        }
    }

    override suspend fun getPageList(chapter: SourceEpisode): List<SourcePageImage> {
        val document = networkClient.getDocument(absoluteUrl(baseUrl, chapter.url), headers)
        return document.select("picture img").mapIndexed { index, element ->
            SourcePageImage(
                index = index,
                url = chapter.url,
                imageUrl = imageUrlFromElement(element),
            )
        }
    }

    private fun searchUrl(page: Int, query: String, filters: List<SourceFilter>): String {
        val parameters = mutableListOf("page" to page.toString(), "q" to query)
        filters.forEach { filter ->
            when (filter) {
                is SourceFilter.Group -> filter.values.filterIsInstance<SourceFilter.TriState>()
                    .filter { it.state == SourceFilter.TriState.STATE_INCLUDE }
                    .forEach { parameters += "genre" to it.name }
                is SourceFilter.Select -> when (filter.name) {
                    "Status" -> parameters += "status" to STATUS_FILTERS.getOrNull(filter.state)?.second.orEmpty()
                    "Type" -> parameters += "type" to TYPE_FILTERS.getOrNull(filter.state)?.second.orEmpty()
                }
                else -> Unit
            }
        }
        return buildUrl(baseUrl, "search", parameters)
    }

    private fun mangaFromGridElement(element: Element): SourceMedia? {
        val link = element.selectFirst("a[href^='/manga/']") ?: return null
        val title = element.selectFirst("div.line-clamp-2")?.text()?.takeIf(String::isNotBlank) ?: return null
        return SourceMedia(
            url = relativeUrl(link.absUrl("href")),
            title = title,
            thumbnailUrl = element.selectFirst("img")?.let(::imageUrlFromElement),
        )
    }

    private fun imageUrlFromElement(element: Element): String? {
        return element.attr("abs:data-src").ifBlank { element.attr("abs:src") }.takeIf(String::isNotBlank)
    }

    private fun relativeUrl(url: String): String {
        val uri = URI(url)
        return buildString {
            append(uri.rawPath)
            uri.rawQuery?.let { append('?').append(it) }
        }
    }

    private companion object {
        val TYPE_FILTERS = listOf(
            "All" to "",
            "Manga" to "manga",
            "Novel" to "novel",
            "One-Shot" to "one-shot",
            "Doujinshi" to "doujinshi",
            "Manhwa" to "manhwa",
            "Manhua" to "manhua",
            "Oel" to "oel",
        )

        val STATUS_FILTERS = listOf(
            "All" to "",
            "Publishing" to "publishing",
            "Finished" to "finished",
            "On Hiatus" to "on hiatus",
            "Discontinued" to "discontinued",
            "Not yet Published" to "not yet published",
        )

        val GENRES = listOf(
            "Action", "Adventure", "Cars", "Comedy", "Dementia", "Demons", "Drama", "Ecchi", "Fantasy",
            "Game", "Harem", "Hentai", "Historical", "Horror", "Josei", "Kids", "Magic", "Martial Arts",
            "Mecha", "Military", "Music", "Mystery", "Parody", "Police", "Psychological", "Romance",
            "Samurai", "School", "Sci-Fi", "Seinen", "Shoujo", "Shoujo Ai", "Shounen", "Shounen Ai",
            "Slice of Life", "Space", "Sports", "Super Power", "Supernatural", "Thriller", "Vampire", "Yaoi", "Yuri",
        ).map { it.replaceFirstChar { char -> char.titlecase(Locale.ENGLISH) } }
    }
}
