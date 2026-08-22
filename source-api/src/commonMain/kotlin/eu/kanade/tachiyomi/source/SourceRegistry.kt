package eu.kanade.tachiyomi.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SourceRegistry {
    fun sources(): Flow<List<MultiplatformSource>>
    fun get(id: Long): MultiplatformSource?
}

class BuiltinSourceRegistry(
    sources: List<MultiplatformSource> = emptyList(),
) : SourceRegistry {
    private val sourcesFlow = MutableStateFlow(sources)

    override fun sources(): StateFlow<List<MultiplatformSource>> = sourcesFlow

    override fun get(id: Long): MultiplatformSource? = sourcesFlow.value.firstOrNull { it.id == id }

    fun replace(sources: List<MultiplatformSource>) {
        sourcesFlow.value = sources
    }
}
