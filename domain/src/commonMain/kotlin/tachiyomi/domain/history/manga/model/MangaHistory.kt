package tachiyomi.domain.history.manga.model

import tachiyomi.domain.history.HistoryTimestamp

data class MangaHistory(
    val id: Long,
    val chapterId: Long,
    val readAt: HistoryTimestamp?,
    val readDuration: Long,
) {
    companion object {
        fun create() = MangaHistory(
            id = -1L,
            chapterId = -1L,
            readAt = null,
            readDuration = -1L,
        )
    }
}
