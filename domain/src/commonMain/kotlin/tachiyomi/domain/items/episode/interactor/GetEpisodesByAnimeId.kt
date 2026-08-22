package tachiyomi.domain.items.episode.interactor

import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class GetEpisodesByAnimeId(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(animeId: Long): List<Episode> {
        return try {
            episodeRepository.getEpisodeByAnimeId(animeId)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
