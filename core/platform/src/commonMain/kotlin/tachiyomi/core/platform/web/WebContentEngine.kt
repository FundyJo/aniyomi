package tachiyomi.core.platform.web

import kotlinx.coroutines.flow.StateFlow

data class WebContentRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val javaScriptEnabled: Boolean = true,
)

data class WebCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val secure: Boolean = true,
    val httpOnly: Boolean = true,
)

data class WebContentState(
    val url: String?,
    val isLoading: Boolean,
    val title: String? = null,
    val error: String? = null,
)

interface WebContentEngine {
    val state: StateFlow<WebContentState>

    suspend fun load(request: WebContentRequest)

    suspend fun evaluateJavaScript(script: String): String?

    suspend fun exportCookies(url: String): List<WebCookie>

    suspend fun importCookies(cookies: List<WebCookie>)

    fun stopLoading()
}

interface ExternalBrowser {
    suspend fun open(url: String)
}
