package tachiyomi.domain.download.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.download.model.Download
import tachiyomi.domain.download.model.DownloadQueueItem
import tachiyomi.domain.download.model.DownloadRequest

interface DownloadRepository {
    fun observeQueue(): Flow<List<DownloadQueueItem>>

    fun observe(request: DownloadRequest): Flow<Download?>

    suspend fun enqueue(requests: List<DownloadRequest>)

    suspend fun remove(request: DownloadRequest)
}
