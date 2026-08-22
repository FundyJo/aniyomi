package tachiyomi.data.history.manga

import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.history.historyTimestampFromEpochMilliseconds
import tachiyomi.domain.history.manga.model.MangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations

object MangaHistoryMapper {
    fun mapMangaHistory(
        id: Long,
        chapterId: Long,
        readAt: Long?,
        readDuration: Long,
    ): MangaHistory = MangaHistory(
        id = id,
        chapterId = chapterId,
        readAt = readAt?.let(::historyTimestampFromEpochMilliseconds),
        readDuration = readDuration,
    )

    fun mapMangaHistoryWithRelations(
        historyId: Long,
        mangaId: Long,
        chapterId: Long,
        title: String,
        thumbnailUrl: String?,
        sourceId: Long,
        isFavorite: Boolean,
        coverLastModified: Long,
        chapterNumber: Double,
        readAt: Long?,
        readDuration: Long,
    ): MangaHistoryWithRelations = MangaHistoryWithRelations(
        id = historyId,
        chapterId = chapterId,
        mangaId = mangaId,
        title = title,
        chapterNumber = chapterNumber,
        readAt = readAt?.let(::historyTimestampFromEpochMilliseconds),
        readDuration = readDuration,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = isFavorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
