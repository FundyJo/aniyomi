package tachiyomi.core.platform.filesystem

import kotlinx.coroutines.flow.Flow

@JvmInline
value class PlatformPath(val value: String)

data class FileMetadata(
    val path: PlatformPath,
    val isDirectory: Boolean,
    val size: Long?,
    val lastModifiedEpochMillis: Long?,
)

data class FilePickerRequest(
    val title: String? = null,
    val mimeTypes: List<String> = emptyList(),
    val allowMultiple: Boolean = false,
)

interface PlatformFileSystem {
    suspend fun read(path: PlatformPath): ByteArray

    suspend fun write(path: PlatformPath, bytes: ByteArray, overwrite: Boolean = true)

    suspend fun delete(path: PlatformPath, recursive: Boolean = false)

    suspend fun list(path: PlatformPath): List<FileMetadata>

    suspend fun copy(source: PlatformPath, target: PlatformPath, overwrite: Boolean = false)

    suspend fun move(source: PlatformPath, target: PlatformPath, overwrite: Boolean = false)

    suspend fun exists(path: PlatformPath): Boolean

    suspend fun metadata(path: PlatformPath): FileMetadata?

    suspend fun createDirectories(path: PlatformPath)

    suspend fun availableSpace(path: PlatformPath): Long?
}

interface FilePicker {
    val selections: Flow<List<PlatformPath>>

    suspend fun openFile(request: FilePickerRequest = FilePickerRequest()): List<PlatformPath>

    suspend fun saveFile(defaultName: String, mimeType: String? = null): PlatformPath?

    suspend fun openDirectory(title: String? = null): PlatformPath?
}
