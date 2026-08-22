package tachiyomi.domain.download.model

data class DownloadRequest(
    val entryId: Long,
    val itemId: Long,
    val sourceId: Long,
    val type: DownloadType,
)

enum class DownloadType {
    MangaChapter,
    AnimeEpisode,
}
