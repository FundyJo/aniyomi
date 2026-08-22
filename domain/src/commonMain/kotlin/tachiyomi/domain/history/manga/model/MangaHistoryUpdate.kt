package tachiyomi.domain.history.manga.model

import tachiyomi.domain.history.HistoryTimestamp

data class MangaHistoryUpdate(
    val chapterId: Long,
    val readAt: HistoryTimestamp,
    val sessionReadDuration: Long,
)
