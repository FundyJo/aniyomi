package tachiyomi.domain.download.model

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data object Queued : DownloadState
    data object Downloading : DownloadState
    data object Downloaded : DownloadState
    data object Paused : DownloadState
    data class Failed(val reason: String? = null) : DownloadState
}
