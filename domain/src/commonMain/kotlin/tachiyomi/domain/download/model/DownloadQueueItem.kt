package tachiyomi.domain.download.model

data class DownloadQueueItem(
    val download: Download,
    val position: Int,
)
