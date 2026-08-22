package tachiyomi.domain.history.anime.model

import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.history.HistoryTimestamp

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    val title: String,
    val episodeNumber: Double,
    val seenAt: HistoryTimestamp?,
    val coverData: AnimeCover,
)
