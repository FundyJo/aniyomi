package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimeRelation
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

interface AnimeCatalogueSource : AnimeSource {

    override suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate = supervisorScope {
        val asyncAnime = if (fetchDetails) async { getAnimeDetails(anime) } else null
        val asyncEpisodes = if (fetchEpisodes) async { getEpisodeList(anime) } else null
        SAnimeEpisodeUpdate(asyncAnime?.await() ?: anime, asyncEpisodes?.await() ?: episodes)
    }

    override suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate = supervisorScope {
        val asyncAnime = if (fetchDetails) async { getAnimeDetails(anime) } else null
        val asyncSeasons = if (fetchSeasons) async { getSeasonList(anime) } else null
        SAnimeSeasonUpdate(asyncAnime?.await() ?: anime, asyncSeasons?.await() ?: seasons)
    }

    override suspend fun getRelatedAnimeList(anime: SAnime): List<AnimeRelation> {
        throw Exception("Stub!")
    }

    override fun getFilterList(): AnimeFilterList = AnimeFilterList()
}
