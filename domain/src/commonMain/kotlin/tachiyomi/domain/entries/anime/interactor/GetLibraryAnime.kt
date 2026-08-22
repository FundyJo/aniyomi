package tachiyomi.domain.entries.anime.interactor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.library.anime.LibraryAnime
import kotlin.time.Duration.Companion.seconds

class GetLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<LibraryAnime> {
        return animeRepository.getLibraryAnime()
    }

    fun subscribe(): Flow<List<LibraryAnime>> {
        return animeRepository.getLibraryAnimeAsFlow()
            .retry {
                if (it is NullPointerException) {
                    delay(0.5.seconds)
                    true
                } else {
                    false
                }
            }.catch {
            }
    }
}
