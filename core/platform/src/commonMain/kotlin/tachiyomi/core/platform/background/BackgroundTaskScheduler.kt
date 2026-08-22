package tachiyomi.core.platform.background

import kotlinx.coroutines.flow.Flow

@JvmInline
value class BackgroundTaskId(val value: String)

enum class BackgroundTaskKind {
    LibraryUpdate,
    SourceUpdate,
    TrackerSync,
    Backup,
    Download,
    Retry,
}

data class BackgroundTaskRequest(
    val id: BackgroundTaskId,
    val kind: BackgroundTaskKind,
    val requiresNetwork: Boolean = true,
    val requiresCharging: Boolean = false,
)

data class BackgroundTaskState(
    val id: BackgroundTaskId,
    val isRunning: Boolean,
    val lastError: String? = null,
)

interface BackgroundTaskScheduler {
    val tasks: Flow<List<BackgroundTaskState>>

    suspend fun schedule(request: BackgroundTaskRequest)

    suspend fun cancel(id: BackgroundTaskId)
}
