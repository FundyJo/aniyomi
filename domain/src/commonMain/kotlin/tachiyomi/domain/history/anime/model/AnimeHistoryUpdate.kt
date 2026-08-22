package tachiyomi.domain.history.anime.model

import tachiyomi.domain.history.HistoryTimestamp

data class AnimeHistoryUpdate(
    val episodeId: Long,
    val seenAt: HistoryTimestamp,
)
