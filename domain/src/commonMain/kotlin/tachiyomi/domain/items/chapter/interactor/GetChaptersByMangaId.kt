package tachiyomi.domain.items.chapter.interactor

import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.repository.ChapterRepository

class GetChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter> {
        return try {
            chapterRepository.getChapterByMangaId(mangaId, applyScanlatorFilter)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
