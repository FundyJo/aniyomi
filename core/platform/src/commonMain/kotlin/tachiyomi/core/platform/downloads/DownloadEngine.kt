package tachiyomi.core.platform.downloads

import kotlinx.coroutines.flow.Flow
import tachiyomi.core.platform.filesystem.PlatformPath

@JvmInline
value class DownloadId(val value: String)

enum class DownloadMediaKind {
    Anime,
    Manga,
}

enum class DownloadStatus {
    Queued,
    Running,
    Paused,
    Completed,
    Failed,
    Cancelled,
}

data class DownloadRequest(
    val id: DownloadId,
    val kind: DownloadMediaKind,
    val url: String,
    val destination: PlatformPath,
    val headers: Map<String, String> = emptyMap(),
    val expectedBytes: Long? = null,
)

data class DownloadTask(
    val id: DownloadId,
    val kind: DownloadMediaKind,
    val status: DownloadStatus,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long? = null,
    val error: String? = null,
)

interface DownloadEngine {
    val downloads: Flow<List<DownloadTask>>

    suspend fun enqueue(request: DownloadRequest)

    suspend fun pause(id: DownloadId)

    suspend fun resume(id: DownloadId)

    suspend fun cancel(id: DownloadId)

    suspend fun retry(id: DownloadId)
}
