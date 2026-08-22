package tachiyomi.domain.history.anime.model

import tachiyomi.domain.history.HistoryTimestamp

data class AnimeHistory(
    val id: Long,
    val episodeId: Long,
    val seenAt: HistoryTimestamp?,
) {
    companion object {
        fun create() = AnimeHistory(
            id = -1L,
            episodeId = -1L,
            seenAt = null,
        )
    }
}
