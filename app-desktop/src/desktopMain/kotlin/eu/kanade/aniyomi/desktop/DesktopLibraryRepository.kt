package eu.kanade.aniyomi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
}
