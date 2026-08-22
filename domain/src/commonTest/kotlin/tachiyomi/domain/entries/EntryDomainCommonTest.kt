package tachiyomi.domain.entries

import aniyomi.domain.anime.SeasonAnime
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.model.toAnimeUpdate
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.model.toChapterUpdate
import tachiyomi.domain.items.chapter.service.getChapterSort
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.model.toEpisodeUpdate
import tachiyomi.domain.items.episode.service.getEpisodeSort
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.anime.model.sort as animeSort
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.manga.model.sort as mangaSort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryDomainCommonTest {

    @Test
    fun animeStatusFavoriteAndFiltersUseFlags() {
        val anime = Anime.create().copy(
            favorite = true,
            status = SAnime.COMPLETED.toLong(),
            episodeFlags = Anime.EPISODE_SHOW_UNSEEN or
                Anime.EPISODE_SHOW_BOOKMARKED or
                Anime.EPISODE_SHOW_FILLERMARKED or
                Anime.EPISODE_SORT_ASC or
                Anime.EPISODE_SORTING_NUMBER,
            seasonFlags = Anime.SEASON_SHOW_SEEN or
                Anime.SEASON_SHOW_STARTED or
                Anime.SEASON_SHOW_COMPLETED or
                Anime.SEASON_SHOW_BOOKMARKED or
                Anime.SEASON_SHOW_FILLERMARKED,
        )

        assertTrue(anime.favorite)
        assertEquals(TriState.ENABLED_IS, anime.unseenFilter)
        assertEquals(TriState.ENABLED_IS, anime.bookmarkedFilter)
        assertEquals(TriState.ENABLED_IS, anime.fillermarkedFilter)
        assertEquals(TriState.ENABLED_NOT, anime.seasonUnseenFilter)
        assertEquals(TriState.ENABLED_IS, anime.seasonStartedFilter)
        assertEquals(TriState.ENABLED_IS, anime.seasonCompletedFilter)
        assertEquals(TriState.ENABLED_IS, anime.seasonBookmarkedFilter)
        assertEquals(TriState.ENABLED_IS, anime.seasonFillermarkedFilter)
        assertFalse(anime.sortDescending())
        assertEquals(Anime.EPISODE_SORTING_NUMBER, anime.sorting)
    }

    @Test
    fun animeUpdateCopyPreservesAllMutableFields() {
        val anime = Anime.create().copy(
            id = 42,
            favorite = true,
            title = "Updated",
            episodeFlags = Anime.EPISODE_SORTING_UPLOAD_DATE,
            status = SAnime.ONGOING.toLong(),
            seasonNumber = 2.0,
        )

        val update = anime.toAnimeUpdate()

        assertEquals(
            AnimeUpdate(
                id = 42,
                source = anime.source,
                favorite = true,
                lastUpdate = anime.lastUpdate,
                nextUpdate = anime.nextUpdate,
                fetchInterval = anime.fetchInterval,
                dateAdded = anime.dateAdded,
                viewerFlags = anime.viewerFlags,
                episodeFlags = Anime.EPISODE_SORTING_UPLOAD_DATE,
                coverLastModified = anime.coverLastModified,
                backgroundLastModified = anime.backgroundLastModified,
                url = anime.url,
                title = "Updated",
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre,
                status = SAnime.ONGOING.toLong(),
                thumbnailUrl = anime.thumbnailUrl,
                backgroundUrl = anime.backgroundUrl,
                updateStrategy = anime.updateStrategy,
                initialized = anime.initialized,
                version = anime.version,
                fetchType = anime.fetchType,
                parentId = anime.parentId,
                seasonFlags = anime.seasonFlags,
                seasonNumber = 2.0,
                seasonSourceOrder = anime.seasonSourceOrder,
                memo = anime.memo,
            ),
            update,
        )
    }

    @Test
    fun mangaStatusFavoriteAndFiltersUseFlags() {
        val manga = Manga.create().copy(
            favorite = true,
            status = SManga.COMPLETED.toLong(),
            chapterFlags = Manga.CHAPTER_SHOW_UNREAD or
                Manga.CHAPTER_SHOW_BOOKMARKED or
                Manga.CHAPTER_SORT_ASC or
                Manga.CHAPTER_SORTING_NUMBER,
        )

        assertTrue(manga.favorite)
        assertEquals(TriState.ENABLED_IS, manga.unreadFilter)
        assertEquals(TriState.ENABLED_IS, manga.bookmarkedFilter)
        assertFalse(manga.sortDescending())
        assertEquals(Manga.CHAPTER_SORTING_NUMBER, manga.sorting)
    }

    @Test
    fun episodeProgressAndSortingUseDomainState() {
        val anime = Anime.create().copy(episodeFlags = Anime.EPISODE_SORTING_NUMBER or Anime.EPISODE_SORT_ASC)
        val first = Episode.create().copy(id = 1, seen = false, lastSecondSeen = 12, totalSeconds = 24, episodeNumber = 2.0, name = "B")
        val second = Episode.create().copy(id = 2, seen = true, lastSecondSeen = 24, totalSeconds = 24, episodeNumber = 1.0, name = "A")

        val sorted = listOf(first, second).sortedWith(getEpisodeSort(anime))
        val update = second.toEpisodeUpdate()

        assertEquals(listOf(second, first), sorted)
        assertFalse(first.seen)
        assertEquals(12, first.lastSecondSeen)
        assertTrue(second.seen)
        assertEquals(24, update.lastSecondSeen)
    }

    @Test
    fun chapterReadBookmarkAndSortingUseDomainState() {
        val manga = Manga.create().copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SORT_ASC)
        val first = Chapter.create().copy(id = 1, read = false, bookmark = true, chapterNumber = 2.0, name = "B")
        val second = Chapter.create().copy(id = 2, read = true, bookmark = false, chapterNumber = 1.0, name = "A")

        val sorted = listOf(first, second).sortedWith(getChapterSort(manga))
        val update = first.toChapterUpdate()

        assertEquals(listOf(second, first), sorted)
        assertFalse(first.read)
        assertTrue(first.bookmark)
        assertEquals(true, update.bookmark)
    }

    @Test
    fun libraryModelsExposeCategoryCountsAndSorting() {
        val category = Category(
            id = 7,
            name = "Library",
            order = 0,
            flags = AnimeLibrarySort.Type.UnseenCount + AnimeLibrarySort.Direction.Descending,
            hidden = false,
        )
        val animeLibrary = LibraryAnime(Anime.create(), category.id, totalCount = 5, seenCount = 2, bookmarkCount = 1, fillermarkCount = 0, latestUpload = 9, episodeFetchedAt = 8, lastSeen = 7)
        val mangaLibrary = LibraryManga(Manga.create(), category.id, totalChapters = 6, readCount = 4, bookmarkCount = 0, latestUpload = 9, chapterFetchedAt = 8, lastRead = 7)
        val mangaCategory = category.copy(flags = MangaLibrarySort.Type.UnreadCount + MangaLibrarySort.Direction.Ascending)

        assertEquals(3, animeLibrary.unseenCount)
        assertTrue(animeLibrary.hasBookmarks)
        assertTrue(animeLibrary.hasStarted)
        assertEquals(2, mangaLibrary.unreadCount)
        assertFalse(mangaLibrary.hasBookmarks)
        assertTrue(mangaLibrary.hasStarted)
        assertEquals(AnimeLibrarySort.Type.UnseenCount, category.animeSort.type)
        assertEquals(MangaLibrarySort.Type.UnreadCount, mangaCategory.mangaSort.type)
        assertEquals(LibraryDisplayMode.CoverOnlyGrid, LibraryDisplayMode.deserialize("COVER_ONLY_GRID"))
    }

    @Test
    fun triStateFilteringAndSeasonAnimeUseCommonLogic() {
        val season = SeasonAnime(
            anime = Anime.create(),
            totalCount = 3,
            seenCount = 1,
            bookmarkCount = 1,
            fillermarkCount = 0,
            latestUpload = 0,
            fetchedAt = 0,
            lastSeen = 0,
        )

        assertTrue(applyFilter(TriState.DISABLED) { false })
        assertTrue(applyFilter(TriState.ENABLED_IS) { true })
        assertTrue(applyFilter(TriState.ENABLED_NOT) { false })
        assertFalse(season.seen)
        assertEquals(2, season.unseenCount)
        assertTrue(season.hasStarted)
        assertTrue(season.hasBookmarks)
        assertFalse(season.hasFillermarks)
    }
}
