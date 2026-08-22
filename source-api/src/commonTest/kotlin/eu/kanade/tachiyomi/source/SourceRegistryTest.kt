package eu.kanade.tachiyomi.source

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SourceRegistryTest {
    @Test
    fun builtinRegistryReturnsRegisteredSources() = runTest {
        val source = FakeMangaSource(id = 1)
        val registry = BuiltinSourceRegistry(listOf(source))

        assertEquals(listOf(source), registry.sources().first())
        assertEquals(source, registry.get(1))
    }

    @Test
    fun globalSearchIsolatesSourceErrors() = runTest {
        val registry = BuiltinSourceRegistry(
            listOf(
                FakeMangaSource(id = 1, title = "result"),
                FakeMangaSource(id = 2, error = UnsupportedOperationException("disabled")),
            ),
        )

        val results = GlobalSourceSearch(registry, concurrencyLimit = 1).search(GlobalSearchQuery("query"))

        assertEquals(2, results.size)
        assertNotNull(results.first { it.source.id == 1 }.page)
        assertIs<SourceError.UnsupportedFeature>(results.first { it.source.id == 2 }.error?.type)
    }
}

private class FakeMangaSource(
    override val id: Long,
    private val title: String = "title",
    private val error: Throwable? = null,
) : MultiplatformMangaSource {
    override val name: String = "Fake"
    override val lang: String = "en"
    override val capabilities: Set<SourceCapability> = setOf(SourceCapability.Manga, SourceCapability.Search)

    override suspend fun getPopularManga(page: Int): SourcePage<SourceMedia> = SourcePage(emptyList(), false)

    override suspend fun getLatestManga(page: Int): SourcePage<SourceMedia> = SourcePage(emptyList(), false)

    override suspend fun searchManga(page: Int, query: String, filters: List<SourceFilter>): SourcePage<SourceMedia> {
        error?.let { throw it }
        return SourcePage(listOf(SourceMedia(url = "/$title", title = title)), false)
    }

    override suspend fun getMangaDetails(manga: SourceMedia): SourceMedia = manga

    override suspend fun getChapterList(manga: SourceMedia): List<SourceEpisode> = emptyList()

    override suspend fun getPageList(chapter: SourceEpisode): List<SourcePageImage> = emptyList()
}
