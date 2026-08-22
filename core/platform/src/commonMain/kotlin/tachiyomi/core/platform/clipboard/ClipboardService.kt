package tachiyomi.core.platform.clipboard

interface ClipboardService {
    suspend fun getText(): String?

    suspend fun setText(text: String)
}
