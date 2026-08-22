package tachiyomi.domain.download.model

data class DownloadProgress(
    val downloaded: Long = 0L,
    val total: Long? = null,
) {
    val fraction: Float?
        get() = total?.takeIf { it > 0L }?.let { downloaded.toFloat() / it.toFloat() }
}
