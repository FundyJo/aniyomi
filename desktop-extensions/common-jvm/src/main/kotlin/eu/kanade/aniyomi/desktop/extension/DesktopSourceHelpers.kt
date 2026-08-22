package eu.kanade.aniyomi.desktop.extension

import eu.kanade.tachiyomi.source.SourceMediaStatus
import eu.kanade.tachiyomi.source.network.HttpHeaders
import eu.kanade.tachiyomi.source.network.NetworkClient
import eu.kanade.tachiyomi.source.network.NetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val DesktopBrowserHeaders = HttpHeaders.of(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
)

suspend fun NetworkClient.getText(url: String, headers: HttpHeaders = DesktopBrowserHeaders): String {
    return execute(NetworkRequest(url = url, headers = headers)).body.readBytes().decodeToString()
}

suspend fun NetworkClient.getJsonText(url: String, headers: HttpHeaders = DesktopBrowserHeaders): String {
    return execute(NetworkRequest(url = url, headers = headers.set("Accept", "application/json"))).body.readBytes().decodeToString()
}

suspend fun NetworkClient.getDocument(url: String, headers: HttpHeaders = DesktopBrowserHeaders): Document {
    val response = execute(NetworkRequest(url = url, headers = headers))
    val html = response.body.readBytes().decodeToString()
    return withContext(Dispatchers.Default) { Jsoup.parse(html, response.finalUrl) }
}

fun absoluteUrl(baseUrl: String, url: String): String = URI(baseUrl).resolve(url).toString()

fun buildUrl(baseUrl: String, path: String, parameters: List<Pair<String, String?>> = emptyList()): String {
    val base = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
    val query = parameters
        .filter { it.second != null }
        .joinToString("&") { (name, value) -> "${name.urlEncode()}=${value.orEmpty().urlEncode()}" }
    return if (query.isBlank()) base else "$base?$query"
}

fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

fun String?.toSourceStatus(): SourceMediaStatus = when (this?.lowercase()) {
    "ended", "finished", "complete", "completed" -> SourceMediaStatus.Completed
    "continuing", "publishing", "ongoing" -> SourceMediaStatus.Ongoing
    "on hiatus" -> SourceMediaStatus.OnHiatus
    "discontinued", "cancelled" -> SourceMediaStatus.Cancelled
    else -> SourceMediaStatus.Unknown
}

fun parseDateMillis(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching { LocalDate.parse(value.substringBefore('T'), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        .getOrDefault(0L)
}

fun Long.formatBytes(): String = when {
    this >= 1_000_000_000 -> "%.2f GB".format(this / 1_000_000_000.0)
    this >= 1_000_000 -> "%.2f MB".format(this / 1_000_000.0)
    this >= 1_000 -> "%.2f KB".format(this / 1_000.0)
    this == 1L -> "1 byte"
    this > 1L -> "$this bytes"
    else -> ""
}
