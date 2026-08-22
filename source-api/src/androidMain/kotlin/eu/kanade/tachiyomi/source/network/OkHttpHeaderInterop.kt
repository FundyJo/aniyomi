package eu.kanade.tachiyomi.source.network

import okhttp3.Cookie as OkHttpCookie
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

fun Headers.toHttpHeaders(): HttpHeaders {
    return HttpHeaders(toMultimap())
}

fun HttpHeaders.toOkHttpHeaders(): Headers {
    return Headers.headersOf(*toList().flatMap { (name, value) -> listOf(name, value) }.toTypedArray())
}

fun OkHttpCookie.toSharedCookie(): Cookie {
    return Cookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        expiresAt = if (persistent) expiresAt else null,
        secure = secure,
        httpOnly = httpOnly,
        hostOnly = hostOnly,
        persistent = persistent,
    )
}

fun Cookie.toOkHttpCookie(defaultDomain: String? = null): OkHttpCookie {
    val cookieDomain = domain ?: defaultDomain
    require(!cookieDomain.isNullOrBlank()) { "Cookie domain is required for OkHttp conversion" }
    val builder = OkHttpCookie.Builder()
        .name(name)
        .value(value)
        .path(path ?: "/")
    if (hostOnly) {
        builder.hostOnlyDomain(cookieDomain)
    } else {
        builder.domain(cookieDomain)
    }
    (expiresAt ?: expiresAtEpochMillis)?.let(builder::expiresAt)
    if (secure) builder.secure()
    if (httpOnly) builder.httpOnly()
    return builder.build()
}

fun Cookie.toOkHttpCookie(url: HttpUrl): OkHttpCookie = toOkHttpCookie(url.host)

fun NetworkRequest.toOkHttpRequest(): Request {
    val requestBody = body?.toOkHttpRequestBody()
    return Request.Builder()
        .url(url)
        .headers(headers.toOkHttpHeaders())
        .method(method.toOkHttpMethod(), requestBody ?: method.emptyBodyOrNull())
        .build()
}

fun Request.toNetworkRequest(): NetworkRequest {
    return NetworkRequest(
        url = url.toString(),
        method = method.toHttpMethod(),
        headers = headers.toHttpHeaders(),
    )
}

private fun NetworkBody.toOkHttpRequestBody() = when (this) {
    is NetworkBody.Bytes -> value.toRequestBody(contentType?.toMediaTypeOrNull())
    is NetworkBody.Text -> value.toRequestBody(contentType?.toMediaTypeOrNull())
}

private fun HttpMethod.toOkHttpMethod(): String = when (this) {
    HttpMethod.Get -> "GET"
    HttpMethod.Post -> "POST"
    HttpMethod.Put -> "PUT"
    HttpMethod.Patch -> "PATCH"
    HttpMethod.Delete -> "DELETE"
    HttpMethod.Head -> "HEAD"
    HttpMethod.Options -> "OPTIONS"
}

private fun HttpMethod.emptyBodyOrNull() = when (this) {
    HttpMethod.Post,
    HttpMethod.Put,
    HttpMethod.Patch,
    -> ByteArray(0).toRequestBody(null)
    else -> null
}

private fun String.toHttpMethod(): HttpMethod = when (uppercase()) {
    "GET" -> HttpMethod.Get
    "POST" -> HttpMethod.Post
    "PUT" -> HttpMethod.Put
    "PATCH" -> HttpMethod.Patch
    "DELETE" -> HttpMethod.Delete
    "HEAD" -> HttpMethod.Head
    "OPTIONS" -> HttpMethod.Options
    else -> HttpMethod.Get
}
