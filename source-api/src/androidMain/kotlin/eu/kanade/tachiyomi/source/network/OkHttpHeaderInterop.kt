package eu.kanade.tachiyomi.source.network

import okhttp3.Headers

fun Headers.toHttpHeaders(): HttpHeaders {
    return HttpHeaders(toMultimap())
}

fun HttpHeaders.toOkHttpHeaders(): Headers {
    return Headers.headersOf(*toList().flatMap { (name, value) -> listOf(name, value) }.toTypedArray())
}
