package eu.kanade.tachiyomi.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

data class GlobalSearchQuery(
    val query: String,
    val page: Int = 1,
    val filters: Map<Long, List<SourceFilter>> = emptyMap(),
)

data class SourceSearchResult(
    val source: MultiplatformSource,
    val page: SourcePage<SourceMedia>? = null,
    val error: SearchError? = null,
)

sealed interface SearchState {
    data object Loading : SearchState
    data class Partial(val results: List<SourceSearchResult>) : SearchState
    data class Complete(val results: List<SourceSearchResult>) : SearchState
}

data class SearchError(
    val type: SourceError,
    val message: String? = null,
)

sealed interface SourceError {
    data object Network : SourceError
    data object Parse : SourceError
    data object Authentication : SourceError
    data object Unavailable : SourceError
    data object UnsupportedFeature : SourceError
    data object Unknown : SourceError
}

class GlobalSourceSearch(
    private val registry: SourceRegistry,
    private val concurrencyLimit: Int = DEFAULT_CONCURRENCY_LIMIT,
) {
    suspend fun search(query: GlobalSearchQuery): List<SourceSearchResult> {
        val searchableSources = registry.sources().first().filter { SourceCapability.Search in it.capabilities }
        val semaphore = Semaphore(concurrencyLimit.coerceAtLeast(1))
        return kotlinx.coroutines.coroutineScope {
            searchableSources.map { source ->
                async {
                    semaphore.withPermit {
                        source.search(query)
                    }
                }
            }.awaitAll()
        }
    }

    fun states(query: GlobalSearchQuery): Flow<SearchState> = channelFlow {
        send(SearchState.Loading)
        val searchableSources = registry.sources().first().filter { SourceCapability.Search in it.capabilities }
        if (searchableSources.isEmpty()) {
            send(SearchState.Complete(emptyList()))
            close()
            return@channelFlow
        }

        val semaphore = Semaphore(concurrencyLimit.coerceAtLeast(1))
        val mutex = Mutex()
        val results = mutableListOf<SourceSearchResult>()
        val jobs = searchableSources.map { source ->
            launch {
                val result = semaphore.withPermit {
                    source.search(query)
                }
                val snapshot = mutex.withLock {
                    results += result
                    results.toList()
                }
                send(SearchState.Partial(snapshot))
            }
        }
        launch {
            jobs.joinAll()
            val snapshot = mutex.withLock { results.toList() }
            send(SearchState.Complete(snapshot))
            close()
        }
        awaitClose()
    }

    private suspend fun MultiplatformSource.search(query: GlobalSearchQuery): SourceSearchResult {
        return try {
            val page = when (this) {
                is MultiplatformAnimeSource -> searchAnime(query.page, query.query, query.filters[id].orEmpty())
                is MultiplatformMangaSource -> searchManga(query.page, query.query, query.filters[id].orEmpty())
                else -> throw UnsupportedOperationException("Source does not support search")
            }
            SourceSearchResult(this, page)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            SourceSearchResult(this, error = SearchError(error.toSourceError(), error.message))
        }
    }

    private fun Throwable.toSourceError(): SourceError = when (this) {
        is UnsupportedOperationException -> SourceError.UnsupportedFeature
        else -> SourceError.Unknown
    }
}

private const val DEFAULT_CONCURRENCY_LIMIT = 8
