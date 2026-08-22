package tachiyomi.domain.entries.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.repository.MangaRepository

class GetManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(id: Long): Manga? {
        return try {
            mangaRepository.getMangaById(id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun subscribe(id: Long): Flow<Manga> {
        return mangaRepository.getMangaByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Manga?> {
        return mangaRepository.getMangaByUrlAndSourceIdAsFlow(url, sourceId)
    }
}
