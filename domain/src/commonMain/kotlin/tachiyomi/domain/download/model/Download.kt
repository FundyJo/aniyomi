package tachiyomi.domain.download.model

data class Download(
    val request: DownloadRequest,
    val state: DownloadState = DownloadState.Queued,
    val progress: DownloadProgress = DownloadProgress(),
)
