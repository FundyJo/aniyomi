package tachiyomi.domain.history.manga.model

import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.history.HistoryTimestamp

data class MangaHistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    val title: String,
    val chapterNumber: Double,
    val readAt: HistoryTimestamp?,
    val readDuration: Long,
    val coverData: MangaCover,
)
