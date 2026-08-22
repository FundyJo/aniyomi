package tachiyomi.domain.category

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.anime.interactor.GetVisibleAnimeCategories
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.category.manga.interactor.GetVisibleMangaCategories
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryCommonTest {

    private val hidden = Category(1, "Hidden", 1, 0, hidden = true)
    private val visible = Category(2, "Visible", 2, 0, hidden = false)

    @Test
    fun systemCategoryUsesUncategorizedId() {
        assertTrue(Category(Category.UNCATEGORIZED_ID, "Default", 0, 0, hidden = false).isSystemCategory)
        assertFalse(visible.isSystemCategory)
    }

    @Test
    fun animeCategoryInteractorsDelegateToRepository() = runTest {
        val repository = FakeAnimeCategoryRepository(listOf(hidden, visible))

        assertEquals(listOf(hidden, visible), GetAnimeCategories(repository).await())
        assertEquals(listOf(hidden, visible), GetAnimeCategories(repository).await(animeId = 10))
        assertEquals(listOf(visible), GetVisibleAnimeCategories(repository).await())
        assertEquals(listOf(visible), GetVisibleAnimeCategories(repository).await(animeId = 10))
    }

    @Test
    fun mangaCategoryInteractorsDelegateToRepository() = runTest {
        val repository = FakeMangaCategoryRepository(listOf(hidden, visible))

        assertEquals(listOf(hidden, visible), GetMangaCategories(repository).await())
        assertEquals(listOf(hidden, visible), GetMangaCategories(repository).await(mangaId = 20))
        assertEquals(listOf(visible), GetVisibleMangaCategories(repository).await())
        assertEquals(listOf(visible), GetVisibleMangaCategories(repository).await(mangaId = 20))
    }

    private class FakeAnimeCategoryRepository(
        private val categories: List<Category>,
    ) : AnimeCategoryRepository {
        private val visibleCategories = categories.filterNot { it.hidden }

        override suspend fun getAnimeCategory(id: Long): Category? = categories.find { it.id == id }
        override suspend fun getAllAnimeCategories(): List<Category> = categories
        override suspend fun getAllVisibleAnimeCategories(): List<Category> = visibleCategories
        override fun getAllAnimeCategoriesAsFlow(): Flow<List<Category>> = flowOf(categories)
        override fun getAllVisibleAnimeCategoriesAsFlow(): Flow<List<Category>> = flowOf(visibleCategories)
        override suspend fun getCategoriesByAnimeId(animeId: Long): List<Category> = categories
        override suspend fun getVisibleCategoriesByAnimeId(animeId: Long): List<Category> = visibleCategories
        override fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> = flowOf(categories)
        override fun getVisibleCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> = flowOf(visibleCategories)
        override suspend fun insertAnimeCategory(category: Category) = Unit
        override suspend fun updatePartialAnimeCategory(update: CategoryUpdate) = Unit
        override suspend fun updatePartialAnimeCategories(updates: List<CategoryUpdate>) = Unit
        override suspend fun updateAllAnimeCategoryFlags(flags: Long?) = Unit
        override suspend fun deleteAnimeCategory(categoryId: Long) = Unit
    }

    private class FakeMangaCategoryRepository(
        private val categories: List<Category>,
    ) : MangaCategoryRepository {
        private val visibleCategories = categories.filterNot { it.hidden }

        override suspend fun getMangaCategory(id: Long): Category? = categories.find { it.id == id }
        override suspend fun getAllMangaCategories(): List<Category> = categories
        override suspend fun getAllVisibleMangaCategories(): List<Category> = visibleCategories
        override fun getAllMangaCategoriesAsFlow(): Flow<List<Category>> = flowOf(categories)
        override fun getAllVisibleMangaCategoriesAsFlow(): Flow<List<Category>> = flowOf(visibleCategories)
        override suspend fun getCategoriesByMangaId(mangaId: Long): List<Category> = categories
        override suspend fun getVisibleCategoriesByMangaId(mangaId: Long): List<Category> = visibleCategories
        override fun getCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>> = flowOf(categories)
        override fun getVisibleCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>> = flowOf(visibleCategories)
        override suspend fun insertMangaCategory(category: Category) = Unit
        override suspend fun updatePartialMangaCategory(update: CategoryUpdate) = Unit
        override suspend fun updatePartialMangaCategories(updates: List<CategoryUpdate>) = Unit
        override suspend fun updateAllMangaCategoryFlags(flags: Long?) = Unit
        override suspend fun deleteMangaCategory(categoryId: Long) = Unit
    }
}
