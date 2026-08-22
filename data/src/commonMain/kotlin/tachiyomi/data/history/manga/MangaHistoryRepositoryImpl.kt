package tachiyomi.data.history.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.history.historyTimestampToEpochMilliseconds
import tachiyomi.domain.history.manga.model.MangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryUpdate
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

class MangaHistoryRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : MangaHistoryRepository {

    override fun getMangaHistory(query: String): Flow<List<MangaHistoryWithRelations>> {
        return handler.subscribeToList {
            historyViewQueries.history(query, MangaHistoryMapper::mapMangaHistoryWithRelations)
        }
    }

    override suspend fun getLastMangaHistory(): MangaHistoryWithRelations? {
        return handler.awaitOneOrNull {
            historyViewQueries.getLatestHistory(MangaHistoryMapper::mapMangaHistoryWithRelations)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { historyQueries.getReadDuration() }
    }

    override suspend fun getHistoryByMangaId(mangaId: Long): List<MangaHistory> {
        return handler.awaitList { historyQueries.getHistoryByMangaId(mangaId, MangaHistoryMapper::mapMangaHistory) }
    }

    override suspend fun resetMangaHistory(historyId: Long) {
        handler.await { historyQueries.resetHistoryById(historyId) }
    }

    override suspend fun resetHistoryByMangaId(mangaId: Long) {
        handler.await { historyQueries.resetHistoryByMangaId(mangaId) }
    }

    override suspend fun deleteAllMangaHistory(): Boolean {
        handler.await { historyQueries.removeAllHistory() }
        return true
    }

    override suspend fun upsertMangaHistory(historyUpdate: MangaHistoryUpdate) {
        handler.await {
            historyQueries.upsert(
                historyUpdate.chapterId,
                historyTimestampToEpochMilliseconds(historyUpdate.readAt),
                historyUpdate.sessionReadDuration,
            )
        }
    }
}
