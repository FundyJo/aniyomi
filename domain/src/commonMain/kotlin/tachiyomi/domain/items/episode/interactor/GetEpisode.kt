package tachiyomi.domain.items.episode.interactor

import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class GetEpisode(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(id: Long): Episode? {
        return try {
            episodeRepository.getEpisodeById(id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun await(url: String, animeId: Long): Episode? {
        return try {
            episodeRepository.getEpisodeByUrlAndAnimeId(url, animeId)
        } catch (e: Exception) {
            null
        }
    }
}
