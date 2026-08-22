package eu.kanade.tachiyomi.source.legacy

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.MultiplatformAnimeSource
import eu.kanade.tachiyomi.source.MultiplatformMangaSource
import eu.kanade.tachiyomi.source.SourceCapability
import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceFilter
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourceMediaStatus
import eu.kanade.tachiyomi.source.SourcePage
import eu.kanade.tachiyomi.source.SourcePageImage
import eu.kanade.tachiyomi.source.VideoSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.network.HttpHeaders
import kotlinx.coroutines.CancellationException

class AndroidLegacyAnimeSourceAdapter(
    private val source: AnimeSource,
) : MultiplatformAnimeSource {
    override val id: Long = source.id
    override val name: String = source.name
    override val lang: String = source.lang
    override val capabilities: Set<SourceCapability> = buildSet {
        add(SourceCapability.Anime)
        add(SourceCapability.Popular)
        add(SourceCapability.Search)
        add(SourceCapability.Video)
        add(SourceCapability.Network)
        if (source.supportsLatest) add(SourceCapability.Latest)
    }

    override fun getAnimeFilters(): List<SourceFilter> {
        return source.getFilterList().map(AnimeFilter<*>::toSourceFilter)
    }

    override suspend fun getPopularAnime(page: Int): SourcePage<SourceMedia> {
        return source.getPopularAnime(page).let { pageResult ->
            SourcePage(pageResult.animes.map(SAnime::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun getLatestAnime(page: Int): SourcePage<SourceMedia> {
        return source.getLatestUpdates(page).let { pageResult ->
            SourcePage(pageResult.animes.map(SAnime::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun searchAnime(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia> {
        return source.getSearchAnime(page, query, filters.toAnimeFilterList()).let { pageResult ->
            SourcePage(pageResult.animes.map(SAnime::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun getAnimeDetails(anime: SourceMedia): SourceMedia {
        return source.getAnimeEpisodeUpdate(anime.toSAnime(), emptyList(), fetchDetails = true, fetchEpisodes = false)
            .anime
            .toSourceMedia()
    }

    override suspend fun getEpisodeList(anime: SourceMedia): List<SourceEpisode> {
        return source.getAnimeEpisodeUpdate(anime.toSAnime(), emptyList(), fetchDetails = false, fetchEpisodes = true)
            .episodes
            .map(SEpisode::toSourceEpisode)
    }

    override suspend fun getVideoList(episode: SourceEpisode): List<VideoSource> {
        val legacyEpisode = episode.toSEpisode()
        val videos = try {
            source.getHosterList(legacyEpisode).flatMap { hoster -> source.getVideoList(hoster) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: UnsupportedOperationException) {
            @Suppress("DEPRECATION")
            source.getVideoList(legacyEpisode)
        } catch (_: IllegalStateException) {
            @Suppress("DEPRECATION")
            source.getVideoList(legacyEpisode)
        }
        return videos.map { video ->
            VideoSource(
                url = video.videoUrl,
                quality = video.videoTitle,
                headers = video.headers ?: HttpHeaders.Empty,
                subtitles = emptyList(),
                audioTracks = emptyList(),
            )
        }
    }
}

class AndroidLegacyMangaSourceAdapter(
    private val source: CatalogueSource,
) : MultiplatformMangaSource {
    override val id: Long = source.id
    override val name: String = source.name
    override val lang: String = source.lang
    override val capabilities: Set<SourceCapability> = buildSet {
        add(SourceCapability.Manga)
        add(SourceCapability.Popular)
        add(SourceCapability.Search)
        add(SourceCapability.Network)
        if (source.supportsLatest) add(SourceCapability.Latest)
    }

    override fun getMangaFilters(): List<SourceFilter> {
        return source.getFilterList().map(Filter<*>::toSourceFilter)
    }

    override suspend fun getPopularManga(page: Int): SourcePage<SourceMedia> {
        return source.getPopularManga(page).let { pageResult ->
            SourcePage(pageResult.mangas.map(SManga::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun getLatestManga(page: Int): SourcePage<SourceMedia> {
        return source.getLatestUpdates(page).let { pageResult ->
            SourcePage(pageResult.mangas.map(SManga::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun searchManga(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia> {
        return source.getSearchManga(page, query, filters.toFilterList()).let { pageResult ->
            SourcePage(pageResult.mangas.map(SManga::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun getMangaDetails(manga: SourceMedia): SourceMedia {
        return source.getMangaDetails(manga.toSManga()).toSourceMedia()
    }

    override suspend fun getChapterList(manga: SourceMedia): List<SourceEpisode> {
        return source.getChapterList(manga.toSManga()).map(SChapter::toSourceEpisode)
    }

    override suspend fun getPageList(chapter: SourceEpisode): List<SourcePageImage> {
        return source.getPageList(chapter.toSChapter()).map { page ->
            SourcePageImage(index = page.index, url = page.url, imageUrl = page.imageUrl)
        }
    }
}

private fun SAnime.toSourceMedia(): SourceMedia = SourceMedia(
    url = url,
    title = title,
    thumbnailUrl = thumbnail_url,
    description = description,
    status = animeStatusToSourceMediaStatus(status),
)

private fun SourceMedia.toSAnime(): SAnime = SAnime.create().also {
    it.url = url
    it.title = title
    it.thumbnail_url = thumbnailUrl
    it.description = description
}

private fun SManga.toSourceMedia(): SourceMedia = SourceMedia(
    url = url,
    title = title,
    thumbnailUrl = thumbnail_url,
    description = description,
    status = mangaStatusToSourceMediaStatus(status),
)

private fun SourceMedia.toSManga(): SManga = SManga.create().also {
    it.url = url
    it.title = title
    it.thumbnail_url = thumbnailUrl
    it.description = description
}

private fun SEpisode.toSourceEpisode(): SourceEpisode = SourceEpisode(
    url = url,
    name = name,
    dateUpload = date_upload,
    number = episode_number,
)

private fun SourceEpisode.toSEpisode(): SEpisode = SEpisode.create().also {
    it.url = url
    it.name = name
    it.date_upload = dateUpload
    it.episode_number = number
}

private fun SChapter.toSourceEpisode(): SourceEpisode = SourceEpisode(
    url = url,
    name = name,
    dateUpload = date_upload,
    number = chapter_number,
)

private fun SourceEpisode.toSChapter(): SChapter = SChapter.create().also {
    it.url = url
    it.name = name
    it.date_upload = dateUpload
    it.chapter_number = number
}

private fun AnimeFilter<*>.toSourceFilter(): SourceFilter = when (this) {
    is AnimeFilter.Header -> SourceFilter.Header(name)
    is AnimeFilter.Separator -> SourceFilter.Separator(name)
    is AnimeFilter.Select<*> -> SourceFilter.Select(name, values.map { it.toString() }, state)
    is AnimeFilter.Text -> SourceFilter.Text(name, state)
    is AnimeFilter.CheckBox -> SourceFilter.CheckBox(name, state)
    is AnimeFilter.TriState -> SourceFilter.TriState(name, state)
    is AnimeFilter.Group<*> -> SourceFilter.Group(
        name,
        state.filterIsInstance<AnimeFilter<*>>().map(AnimeFilter<*>::toSourceFilter),
    )
    is AnimeFilter.Sort -> SourceFilter.Sort(
        name,
        values.toList(),
        state?.let { SourceFilter.Sort.Selection(it.index, it.ascending) },
    )
}

private fun Filter<*>.toSourceFilter(): SourceFilter = when (this) {
    is Filter.Header -> SourceFilter.Header(name)
    is Filter.Separator -> SourceFilter.Separator(name)
    is Filter.Select<*> -> SourceFilter.Select(name, values.map { it.toString() }, state)
    is Filter.Text -> SourceFilter.Text(name, state)
    is Filter.CheckBox -> SourceFilter.CheckBox(name, state)
    is Filter.TriState -> SourceFilter.TriState(name, state)
    is Filter.Group<*> -> SourceFilter.Group(
        name,
        state.filterIsInstance<Filter<*>>().map(Filter<*>::toSourceFilter),
    )
    is Filter.Sort -> SourceFilter.Sort(
        name,
        values.toList(),
        state?.let { SourceFilter.Sort.Selection(it.index, it.ascending) },
    )
}

private fun List<SourceFilter>.toAnimeFilterList(): AnimeFilterList {
    return AnimeFilterList(map(SourceFilter::toAnimeFilter))
}

private fun SourceFilter.toAnimeFilter(): AnimeFilter<*> = when (this) {
    is SourceFilter.Header -> AnimeFilter.Header(name)
    is SourceFilter.Separator -> AnimeFilter.Separator(name)
    is SourceFilter.Select -> object : AnimeFilter.Select<String>(name, values.toTypedArray(), state) {}
    is SourceFilter.Text -> object : AnimeFilter.Text(name, state) {}
    is SourceFilter.CheckBox -> object : AnimeFilter.CheckBox(name, state) {}
    is SourceFilter.TriState -> object : AnimeFilter.TriState(name, state) {}
    is SourceFilter.Group -> object : AnimeFilter.Group<AnimeFilter<*>>(
        name,
        values.map(SourceFilter::toAnimeFilter),
    ) {}
    is SourceFilter.Sort -> object : AnimeFilter.Sort(
        name,
        values.toTypedArray(),
        selection?.let { AnimeFilter.Sort.Selection(it.index, it.ascending) },
    ) {}
}

private fun List<SourceFilter>.toFilterList(): FilterList {
    return FilterList(map(SourceFilter::toFilter))
}

private fun SourceFilter.toFilter(): Filter<*> = when (this) {
    is SourceFilter.Header -> Filter.Header(name)
    is SourceFilter.Separator -> Filter.Separator(name)
    is SourceFilter.Select -> object : Filter.Select<String>(name, values.toTypedArray(), state) {}
    is SourceFilter.Text -> object : Filter.Text(name, state) {}
    is SourceFilter.CheckBox -> object : Filter.CheckBox(name, state) {}
    is SourceFilter.TriState -> object : Filter.TriState(name, state) {}
    is SourceFilter.Group -> object : Filter.Group<Filter<*>>(
        name,
        values.map(SourceFilter::toFilter),
    ) {}
    is SourceFilter.Sort -> object : Filter.Sort(
        name,
        values.toTypedArray(),
        selection?.let { Filter.Sort.Selection(it.index, it.ascending) },
    ) {}
}

private fun animeStatusToSourceMediaStatus(status: Int): SourceMediaStatus = when (status) {
    SAnime.ONGOING -> SourceMediaStatus.Ongoing
    SAnime.COMPLETED -> SourceMediaStatus.Completed
    SAnime.LICENSED -> SourceMediaStatus.Licensed
    SAnime.PUBLISHING_FINISHED -> SourceMediaStatus.PublishingFinished
    SAnime.CANCELLED -> SourceMediaStatus.Cancelled
    SAnime.ON_HIATUS -> SourceMediaStatus.OnHiatus
    else -> SourceMediaStatus.Unknown
}

private fun mangaStatusToSourceMediaStatus(status: Int): SourceMediaStatus = when (status) {
    SManga.ONGOING -> SourceMediaStatus.Ongoing
    SManga.COMPLETED -> SourceMediaStatus.Completed
    SManga.LICENSED -> SourceMediaStatus.Licensed
    SManga.PUBLISHING_FINISHED -> SourceMediaStatus.PublishingFinished
    SManga.CANCELLED -> SourceMediaStatus.Cancelled
    SManga.ON_HIATUS -> SourceMediaStatus.OnHiatus
    else -> SourceMediaStatus.Unknown
}
