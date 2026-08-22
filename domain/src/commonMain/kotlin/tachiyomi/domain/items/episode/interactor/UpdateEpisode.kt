package tachiyomi.domain.items.episode.interactor

import tachiyomi.domain.items.episode.model.EpisodeUpdate
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class UpdateEpisode(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(episodeUpdate: EpisodeUpdate) {
        try {
            episodeRepository.updateEpisode(episodeUpdate)
        } catch (e: Exception) {
        }
    }

    suspend fun awaitAll(episodeUpdates: List<EpisodeUpdate>) {
        try {
            episodeRepository.updateAllEpisodes(episodeUpdates)
        } catch (e: Exception) {
        }
    }
}
