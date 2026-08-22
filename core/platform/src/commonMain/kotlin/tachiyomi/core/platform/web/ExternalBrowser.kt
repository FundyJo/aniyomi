package tachiyomi.core.platform.web

data class ExternalBrowserRequest(
    val url: String,
    val newTask: Boolean = false,
)

interface ExternalBrowser {
    suspend fun open(request: ExternalBrowserRequest)
}
