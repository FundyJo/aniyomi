package eu.kanade.tachiyomi.source.legacy

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.MultiplatformSource
import eu.kanade.tachiyomi.source.SourceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AndroidLegacySourceRegistry(
    private val animeSources: Flow<List<AnimeSource>>,
    private val mangaSources: Flow<List<CatalogueSource>>,
    private val getAnimeSource: (Long) -> AnimeSource? = { null },
    private val getMangaSource: (Long) -> CatalogueSource? = { null },
) : SourceRegistry {
    override fun sources(): Flow<List<MultiplatformSource>> {
        val adaptedAnimeSources = animeSources.map { sources ->
            sources.map { source -> AndroidLegacyAnimeSourceAdapter(source) as MultiplatformSource }
        }
        val adaptedMangaSources = mangaSources.map { sources ->
            sources.map { source -> AndroidLegacyMangaSourceAdapter(source) as MultiplatformSource }
        }
        return adaptedAnimeSources.combine(adaptedMangaSources) { anime, manga -> anime + manga }
    }

    override fun get(id: Long): MultiplatformSource? {
        return getAnimeSource(id)?.let(::AndroidLegacyAnimeSourceAdapter)
            ?: getMangaSource(id)?.let(::AndroidLegacyMangaSourceAdapter)
    }
}
