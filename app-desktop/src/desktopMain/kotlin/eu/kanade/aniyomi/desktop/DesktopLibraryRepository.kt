package eu.kanade.aniyomi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourceMediaStatus
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.data.Database
import tachiyomi.data.entries.anime.AnimeMapper
import tachiyomi.data.entries.manga.MangaMapper
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.mi.data.AnimeDatabase

data class DesktopHistoryItem(
    val type: MediaType,
    val title: String,
    val entryId: Long,
    val itemId: Long,
    val itemNumber: Double,
    val source: Long,
    val timestamp: Long,
)

data class DesktopUpdateItem(
    val type: MediaType,
    val title: String,
    val itemName: String,
    val entryId: Long,
    val itemId: Long,
    val source: Long,
    val dateUpload: Long,
    val downloaded: Boolean = false,
)

enum class MediaType {
    Anime,
    Manga,
}

class DesktopLibraryRepository(
    private val mangaDatabase: Database,
    private val animeDatabase: AnimeDatabase,
) {
    suspend fun createBackupSnapshot(): DesktopBackupSnapshot = withContext(Dispatchers.IO) {
        val manga = mangaDatabase.mangasQueries.getFavorites().executeAsList().map { mangaRow ->
            DesktopMangaBackup(
                sourceId = mangaRow.source,
                url = mangaRow.url,
                title = mangaRow.title,
                description = mangaRow.description,
                thumbnailUrl = mangaRow.thumbnail_url,
                status = mangaRow.status.toInt(),
                chapters = mangaDatabase.chaptersQueries.getChaptersByMangaId(mangaRow._id, false).executeAsList().map { chapter ->
                    DesktopChapterBackup(
                        url = chapter.url,
                        name = chapter.name,
                        number = chapter.chapter_number,
                        dateUpload = chapter.date_upload,
                        lastPageRead = chapter.last_page_read,
                        read = chapter.read,
                        bookmark = chapter.bookmark,
                    )
                },
            )
        }
        val anime = animeDatabase.animesQueries.getFavorites().executeAsList().map { animeRow ->
            DesktopAnimeBackup(
                sourceId = animeRow.source,
                url = animeRow.url,
                title = animeRow.title,
                description = animeRow.description,
                thumbnailUrl = animeRow.thumbnail_url,
                status = animeRow.status.toInt(),
                episodes = animeDatabase.episodesQueries.getEpisodesByAnimeId(animeRow._id).executeAsList().map { episode ->
                    DesktopEpisodeBackup(
                        url = episode.url,
                        name = episode.name,
                        number = episode.episode_number,
                        dateUpload = episode.date_upload,
                        lastSecondSeen = episode.last_second_seen,
                        totalSeconds = episode.total_seconds,
                        seen = episode.seen,
                        bookmark = episode.bookmark,
                    )
                },
            )
        }
        DesktopBackupSnapshot(
            version = 1,
            createdAt = System.currentTimeMillis(),
            manga = manga,
            anime = anime,
        )
    }

    suspend fun restoreBackupSnapshot(snapshot: DesktopBackupSnapshot) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        snapshot.manga.forEach { mangaBackup ->
            val mangaId = addMangaToLibrary(
                mangaBackup.sourceId,
                SourceMedia(
                    url = mangaBackup.url,
                    title = mangaBackup.title,
                    thumbnailUrl = mangaBackup.thumbnailUrl,
                    description = mangaBackup.description,
                    status = mangaBackup.status.toMangaSourceStatus(),
                ),
            )
            mangaBackup.chapters.forEach { chapter ->
                val existing = mangaDatabase.chaptersQueries.getChapterByUrlAndMangaId(chapter.url, mangaId).executeAsOneOrNull()
                if (existing == null) {
                    mangaDatabase.chaptersQueries.insert(
                        mangaId = mangaId,
                        url = chapter.url,
                        name = chapter.name,
                        scanlator = null,
                        read = chapter.read,
                        bookmark = chapter.bookmark,
                        lastPageRead = chapter.lastPageRead,
                        chapterNumber = chapter.number,
                        sourceOrder = 0,
                        dateFetch = now,
                        dateUpload = chapter.dateUpload,
                        version = 0,
                    )
                } else {
                    mangaDatabase.chaptersQueries.update(
                        mangaId = mangaId,
                        url = chapter.url,
                        name = chapter.name,
                        scanlator = null,
                        read = chapter.read,
                        bookmark = chapter.bookmark,
                        lastPageRead = chapter.lastPageRead,
                        chapterNumber = chapter.number,
                        sourceOrder = null,
                        dateFetch = now,
                        dateUpload = chapter.dateUpload,
                        version = null,
                        isSyncing = null,
                        chapterId = existing._id,
                    )
                }
            }
        }
        snapshot.anime.forEach { animeBackup ->
            val animeId = addAnimeToLibrary(
                animeBackup.sourceId,
                SourceMedia(
                    url = animeBackup.url,
                    title = animeBackup.title,
                    thumbnailUrl = animeBackup.thumbnailUrl,
                    description = animeBackup.description,
                    status = animeBackup.status.toAnimeSourceStatus(),
                ),
            )
            animeBackup.episodes.forEach { episode ->
                val existing = animeDatabase.episodesQueries.getEpisodeByUrlAndAnimeId(episode.url, animeId).executeAsOneOrNull()
                if (existing == null) {
                    animeDatabase.episodesQueries.insert(
                        animeId = animeId,
                        url = episode.url,
                        name = episode.name,
                        scanlator = null,
                        seen = episode.seen,
                        bookmark = episode.bookmark,
                        lastSecondSeen = episode.lastSecondSeen,
                        totalSeconds = episode.totalSeconds,
                        episodeNumber = episode.number,
                        sourceOrder = 0,
                        dateFetch = now,
                        dateUpload = episode.dateUpload,
                        version = 0,
                        summary = null,
                        previewUrl = null,
                        fillermark = false,
                        memo = buildJsonObject { },
                    )
                } else {
                    animeDatabase.episodesQueries.update(
                        animeId = animeId,
                        url = episode.url,
                        name = episode.name,
                        scanlator = null,
                        seen = episode.seen,
                        bookmark = episode.bookmark,
                        lastSecondSeen = episode.lastSecondSeen,
                        totalSeconds = episode.totalSeconds,
                        episodeNumber = episode.number,
                        sourceOrder = null,
                        dateFetch = now,
                        dateUpload = episode.dateUpload,
                        version = null,
                        isSyncing = null,
                        summary = null,
                        previewUrl = null,
                        fillermark = null,
                        memo = null,
                        episodeId = existing._id,
                    )
                }
            }
        }
    }

    suspend fun getMangaLibrary(): List<LibraryManga> = withContext(Dispatchers.IO) {
        mangaDatabase.libraryViewQueries.library(MangaMapper::mapLibraryManga).executeAsList()
    }

    suspend fun getAnimeLibrary(): List<LibraryAnime> = withContext(Dispatchers.IO) {
        animeDatabase.animelibViewQueries.animelib(AnimeMapper::mapLibraryAnime).executeAsList()
    }

    suspend fun getHistory(query: String): List<DesktopHistoryItem> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.lowercase()
        val manga = mangaDatabase.historyViewQueries.history(normalizedQuery) {
                id,
                mangaId,
                chapterId,
                title,
                _,
                source,
                _,
                _,
                chapterNumber,
                readAt,
                _,
            ->
            DesktopHistoryItem(MediaType.Manga, title, mangaId, chapterId, chapterNumber, source, readAt)
        }.executeAsList()
        val anime = animeDatabase.animehistoryViewQueries.animehistory(normalizedQuery) {
                id,
                animeId,
                episodeId,
                title,
                _,
                source,
                _,
                _,
                episodeNumber,
                seenAt,
            ->
            DesktopHistoryItem(MediaType.Anime, title, animeId, episodeId, episodeNumber, source, seenAt)
        }.executeAsList()
        (anime + manga).sortedByDescending { it.timestamp }
    }

    suspend fun getUpdates(after: Long = 0, limit: Long = 100): List<DesktopUpdateItem> = withContext(Dispatchers.IO) {
        val manga = mangaDatabase.updatesViewQueries.getRecentUpdates(after, limit) {
                mangaId,
                mangaTitle,
                chapterId,
                chapterName,
                _,
                _,
                _,
                _,
                source,
                _,
                _,
                _,
                dateUpload,
                _,
            ->
            DesktopUpdateItem(MediaType.Manga, mangaTitle, chapterName, mangaId, chapterId, source, dateUpload)
        }.executeAsList()
        val anime = animeDatabase.animeupdatesViewQueries.getRecentAnimeUpdates(after, limit) {
                animeId,
                animeTitle,
                episodeId,
                episodeName,
                _,
                _,
                _,
                _,
                _,
                _,
                source,
                _,
                _,
                _,
                dateUpload,
                _,
            ->
            DesktopUpdateItem(MediaType.Anime, animeTitle, episodeName, animeId, episodeId, source, dateUpload)
        }.executeAsList()
        (anime + manga).sortedByDescending { it.dateUpload }
    }

    suspend fun addMangaToLibrary(sourceId: Long, manga: SourceMedia): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        mangaDatabase.transactionWithResult {
            val existing = mangaDatabase.mangasQueries.getMangaByUrlAndSource(manga.url, sourceId).executeAsOneOrNull()
            if (existing != null) {
                mangaDatabase.mangasQueries.update(
                    source = sourceId,
                    url = manga.url,
                    artist = null,
                    author = null,
                    description = manga.description,
                    genre = null,
                    title = manga.title,
                    status = manga.status.toMangaStatus().toLong(),
                    thumbnailUrl = manga.thumbnailUrl,
                    favorite = true,
                    lastUpdate = null,
                    nextUpdate = null,
                    initialized = true,
                    viewer = null,
                    chapterFlags = null,
                    coverLastModified = null,
                    dateAdded = now,
                    updateStrategy = null,
                    calculateInterval = null,
                    version = null,
                    isSyncing = null,
                    mangaId = existing._id,
                )
                existing._id
            } else {
                mangaDatabase.mangasQueries.insert(
                    source = sourceId,
                    url = manga.url,
                    artist = null,
                    author = null,
                    description = manga.description,
                    genre = emptyList(),
                    title = manga.title,
                    status = manga.status.toMangaStatus().toLong(),
                    thumbnailUrl = manga.thumbnailUrl,
                    favorite = true,
                    lastUpdate = 0,
                    nextUpdate = 0,
                    initialized = true,
                    viewerFlags = 0,
                    chapterFlags = 0,
                    coverLastModified = 0,
                    dateAdded = now,
                    updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
                    calculateInterval = 0,
                    version = 0,
                )
                mangaDatabase.mangasQueries.selectLastInsertedRowId().executeAsOne()
            }
        }
    }

    suspend fun addAnimeToLibrary(sourceId: Long, anime: SourceMedia): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        animeDatabase.transactionWithResult {
            val existing = animeDatabase.animesQueries.getAnimeByUrlAndSource(anime.url, sourceId).executeAsOneOrNull()
            if (existing != null) {
                animeDatabase.animesQueries.update(
                    source = sourceId,
                    url = anime.url,
                    artist = null,
                    author = null,
                    description = anime.description,
                    genre = null,
                    title = anime.title,
                    status = anime.status.toAnimeStatus().toLong(),
                    thumbnailUrl = anime.thumbnailUrl,
                    favorite = true,
                    lastUpdate = null,
                    nextUpdate = null,
                    initialized = true,
                    viewer = null,
                    episodeFlags = null,
                    coverLastModified = null,
                    dateAdded = now,
                    updateStrategy = null,
                    calculateInterval = null,
                    version = null,
                    isSyncing = null,
                    fetchType = null,
                    parentId = null,
                    seasonFlags = null,
                    seasonNumber = null,
                    seasonSourceOrder = null,
                    backgroundUrl = null,
                    backgroundLastModified = null,
                    memo = null,
                    animeId = existing._id,
                )
                existing._id
            } else {
                animeDatabase.animesQueries.insert(
                    source = sourceId,
                    url = anime.url,
                    artist = null,
                    author = null,
                    description = anime.description,
                    genre = emptyList(),
                    title = anime.title,
                    status = anime.status.toAnimeStatus().toLong(),
                    thumbnailUrl = anime.thumbnailUrl,
                    favorite = true,
                    lastUpdate = 0,
                    nextUpdate = 0,
                    initialized = true,
                    viewerFlags = 0,
                    episodeFlags = 0,
                    coverLastModified = 0,
                    dateAdded = now,
                    updateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE,
                    calculateInterval = 0,
                    version = 0,
                    fetchType = FetchType.Episodes,
                    parentId = null,
                    seasonFlags = 0,
                    seasonNumber = -1.0,
                    seasonSourceOrder = 0,
                    backgroundUrl = null,
                    backgroundLastModified = 0,
                    memo = buildJsonObject { },
                )
                animeDatabase.animesQueries.selectLastInsertedRowId().executeAsOne()
            }
        }
    }

    suspend fun getChapterProgress(sourceId: Long, manga: SourceMedia, chapter: SourceEpisode): Int = withContext(Dispatchers.IO) {
        val mangaRow = mangaDatabase.mangasQueries.getMangaByUrlAndSource(manga.url, sourceId).executeAsOneOrNull() ?: return@withContext 0
        mangaDatabase.chaptersQueries.getChapterByUrlAndMangaId(chapter.url, mangaRow._id).executeAsOneOrNull()?.last_page_read?.toInt() ?: 0
    }

    suspend fun saveChapterProgress(sourceId: Long, manga: SourceMedia, chapter: SourceEpisode, pageIndex: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val mangaId = addMangaToLibrary(sourceId, manga)
        val existing = mangaDatabase.chaptersQueries.getChapterByUrlAndMangaId(chapter.url, mangaId).executeAsOneOrNull()
        val read = totalPages > 0 && pageIndex >= (totalPages - 1).coerceAtLeast((totalPages * 0.9f).toInt())
        if (existing == null) {
            mangaDatabase.chaptersQueries.insert(
                mangaId = mangaId,
                url = chapter.url,
                name = chapter.name,
                scanlator = null,
                read = read,
                bookmark = false,
                lastPageRead = pageIndex.toLong(),
                chapterNumber = chapter.number.toDouble(),
                sourceOrder = 0,
                dateFetch = now,
                dateUpload = chapter.dateUpload,
                version = 0,
            )
        } else {
            mangaDatabase.chaptersQueries.update(
                mangaId = mangaId,
                url = chapter.url,
                name = chapter.name,
                scanlator = null,
                read = read,
                bookmark = null,
                lastPageRead = pageIndex.toLong(),
                chapterNumber = chapter.number.toDouble(),
                sourceOrder = null,
                dateFetch = now,
                dateUpload = chapter.dateUpload,
                version = null,
                isSyncing = null,
                chapterId = existing._id,
            )
        }
    }

    suspend fun getEpisodeProgress(sourceId: Long, anime: SourceMedia, episode: SourceEpisode): Long = withContext(Dispatchers.IO) {
        val animeRow = animeDatabase.animesQueries.getAnimeByUrlAndSource(anime.url, sourceId).executeAsOneOrNull() ?: return@withContext 0
        animeDatabase.episodesQueries.getEpisodeByUrlAndAnimeId(episode.url, animeRow._id).executeAsOneOrNull()?.last_second_seen ?: 0
    }

    suspend fun saveEpisodeProgress(sourceId: Long, anime: SourceMedia, episode: SourceEpisode, positionSeconds: Long, durationSeconds: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val animeId = addAnimeToLibrary(sourceId, anime)
        val existing = animeDatabase.episodesQueries.getEpisodeByUrlAndAnimeId(episode.url, animeId).executeAsOneOrNull()
        val seen = durationSeconds > 0 && positionSeconds >= (durationSeconds * 0.9f).toLong()
        if (existing == null) {
            animeDatabase.episodesQueries.insert(
                animeId = animeId,
                url = episode.url,
                name = episode.name,
                scanlator = null,
                seen = seen,
                bookmark = false,
                lastSecondSeen = positionSeconds,
                totalSeconds = durationSeconds,
                episodeNumber = episode.number.toDouble(),
                sourceOrder = 0,
                dateFetch = now,
                dateUpload = episode.dateUpload,
                version = 0,
                summary = null,
                previewUrl = null,
                fillermark = false,
                memo = buildJsonObject { },
            )
        } else {
            animeDatabase.episodesQueries.update(
                animeId = animeId,
                url = episode.url,
                name = episode.name,
                scanlator = null,
                seen = seen,
                bookmark = null,
                lastSecondSeen = positionSeconds,
                totalSeconds = durationSeconds,
                episodeNumber = episode.number.toDouble(),
                sourceOrder = null,
                dateFetch = now,
                dateUpload = episode.dateUpload,
                version = null,
                isSyncing = null,
                summary = null,
                previewUrl = null,
                fillermark = null,
                memo = null,
                episodeId = existing._id,
            )
        }
    }
}

private fun SourceMediaStatus.toMangaStatus(): Int = when (this) {
    SourceMediaStatus.Ongoing -> SManga.ONGOING
    SourceMediaStatus.Completed -> SManga.COMPLETED
    SourceMediaStatus.Licensed -> SManga.LICENSED
    SourceMediaStatus.PublishingFinished -> SManga.PUBLISHING_FINISHED
    SourceMediaStatus.Cancelled -> SManga.CANCELLED
    SourceMediaStatus.OnHiatus -> SManga.ON_HIATUS
    SourceMediaStatus.Unknown -> SManga.UNKNOWN
}

private fun SourceMediaStatus.toAnimeStatus(): Int = when (this) {
    SourceMediaStatus.Ongoing -> SAnime.ONGOING
    SourceMediaStatus.Completed -> SAnime.COMPLETED
    SourceMediaStatus.Licensed -> SAnime.LICENSED
    SourceMediaStatus.PublishingFinished -> SAnime.PUBLISHING_FINISHED
    SourceMediaStatus.Cancelled -> SAnime.CANCELLED
    SourceMediaStatus.OnHiatus -> SAnime.ON_HIATUS
    SourceMediaStatus.Unknown -> SAnime.UNKNOWN
}

private fun Int.toMangaSourceStatus(): SourceMediaStatus = when (this) {
    SManga.ONGOING -> SourceMediaStatus.Ongoing
    SManga.COMPLETED -> SourceMediaStatus.Completed
    SManga.LICENSED -> SourceMediaStatus.Licensed
    SManga.PUBLISHING_FINISHED -> SourceMediaStatus.PublishingFinished
    SManga.CANCELLED -> SourceMediaStatus.Cancelled
    SManga.ON_HIATUS -> SourceMediaStatus.OnHiatus
    else -> SourceMediaStatus.Unknown
}

private fun Int.toAnimeSourceStatus(): SourceMediaStatus = when (this) {
    SAnime.ONGOING -> SourceMediaStatus.Ongoing
    SAnime.COMPLETED -> SourceMediaStatus.Completed
    SAnime.LICENSED -> SourceMediaStatus.Licensed
    SAnime.PUBLISHING_FINISHED -> SourceMediaStatus.PublishingFinished
    SAnime.CANCELLED -> SourceMediaStatus.Cancelled
    SAnime.ON_HIATUS -> SourceMediaStatus.OnHiatus
    else -> SourceMediaStatus.Unknown
}
