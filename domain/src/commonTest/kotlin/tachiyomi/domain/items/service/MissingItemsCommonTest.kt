package tachiyomi.domain.items.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.service.calculateChapterGap
import tachiyomi.domain.items.chapter.service.missingChaptersCount
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.service.calculateEpisodeGap
import tachiyomi.domain.items.episode.service.missingEntriesCount

class MissingItemsCommonTest {

    @Test
    fun missingCountsIgnoreUnknownAndRepeatedEntries() {
        assertEquals(0, emptyList<Double>().missingEntriesCount())
        assertEquals(0, listOf(-1.0, -1.0).missingEntriesCount())
        assertEquals(5, listOf(-1.0, 1.0, 2.0, 2.2, 4.0, 6.0, 10.0, 10.0).missingEntriesCount())

        assertEquals(0, emptyList<Double>().missingChaptersCount())
        assertEquals(0, listOf(-1.0, -1.0).missingChaptersCount())
        assertEquals(5, listOf(-1.0, 1.0, 2.0, 2.2, 4.0, 6.0, 10.0, 10.0).missingChaptersCount())
    }

    @Test
    fun itemGapsUseRecognizedNumbersOnly() {
        assertEquals(1, calculateEpisodeGap(episode(10.0), episode(8.5)))
        assertEquals(0, calculateEpisodeGap(episode(-1.0), episode(10.0)))
        assertEquals(1, calculateChapterGap(chapter(10.0), chapter(8.5)))
        assertEquals(0, calculateChapterGap(chapter(99.0), chapter(-1.0)))
    }

    private fun episode(number: Double) = Episode.create().copy(
        episodeNumber = number,
    )

    private fun chapter(number: Double) = Chapter.create().copy(
        chapterNumber = number,
    )
}
