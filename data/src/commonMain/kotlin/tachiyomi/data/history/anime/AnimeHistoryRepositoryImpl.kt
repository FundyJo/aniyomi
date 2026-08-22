package tachiyomi.data.history.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.history.historyTimestampToEpochMilliseconds
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryUpdate
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository

class AnimeHistoryRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeHistoryRepository {

    override fun getAnimeHistory(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return handler.subscribeToList {
            animehistoryViewQueries.animehistory(query, AnimeHistoryMapper::mapAnimeHistoryWithRelations)
        }
    }

    override suspend fun getLastAnimeHistory(): AnimeHistoryWithRelations? {
        return handler.awaitOneOrNull {
            animehistoryViewQueries.getLatestAnimeHistory(AnimeHistoryMapper::mapAnimeHistoryWithRelations)
        }
    }

    override suspend fun getHistoryByAnimeId(animeId: Long): List<AnimeHistory> {
        return handler.awaitList {
            animehistoryQueries.getHistoryByAnimeId(
                animeId,
                AnimeHistoryMapper::mapAnimeHistory,
            )
        }
    }

    override suspend fun resetAnimeHistory(historyId: Long) {
        handler.await { animehistoryQueries.resetAnimeHistoryById(historyId) }
    }

    override suspend fun resetHistoryByAnimeId(animeId: Long) {
        handler.await { animehistoryQueries.resetHistoryByAnimeId(animeId) }
    }

    override suspend fun deleteAllAnimeHistory(): Boolean {
        handler.await { animehistoryQueries.removeAllHistory() }
        return true
    }

    override suspend fun upsertAnimeHistory(historyUpdate: AnimeHistoryUpdate) {
        handler.await {
            animehistoryQueries.upsert(
                historyUpdate.episodeId,
                historyTimestampToEpochMilliseconds(historyUpdate.seenAt),
            )
        }
    }
}
