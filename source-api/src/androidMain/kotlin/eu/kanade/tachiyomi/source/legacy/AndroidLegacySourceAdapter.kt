package eu.kanade.tachiyomi.source.legacy

import eu.kanade.tachiyomi.animesource.AnimeSource
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
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.network.HttpHeaders

class AndroidLegacyAnimeSourceAdapter(
    private val source: AnimeSource,
) : MultiplatformAnimeSource {
    override val id: Long = source.id
    override val name: String = source.name
    override val lang: String = source.lang
    override val capabilities: Set<SourceCapability> = setOf(SourceCapability.Network)

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
        return source.getSearchAnime(page, query, source.getFilterList()).let { pageResult ->
            SourcePage(pageResult.animes.map(SAnime::toSourceMedia), pageResult.hasNextPage)
        }
    }

    override suspend fun getAnimeDetails(anime: SourceMedia): SourceMedia {
        return source.getAnimeDetails(anime.toSAnime()).toSourceMedia()
    }

    override suspend fun getEpisodeList(anime: SourceMedia): List<SourceEpisode> {
        return source.getEpisodeList(anime.toSAnime()).map(SEpisode::toSourceEpisode)
    }

    override suspend fun getVideoList(episode: SourceEpisode): List<VideoSource> {
        return source.getVideoList(episode.toSEpisode()).map { video ->
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
    override val capabilities: Set<SourceCapability> = setOf(SourceCapability.Network)

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
        return source.getSearchManga(page, query, FilterList()).let { pageResult ->
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
