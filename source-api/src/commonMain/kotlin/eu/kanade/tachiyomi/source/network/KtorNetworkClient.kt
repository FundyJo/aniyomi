package eu.kanade.tachiyomi.source.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorNetworkClient(
    private val httpClient: HttpClient = HttpClient(defaultKtorEngineFactory()) {
        expectSuccess = false
        followRedirects = true
        install(HttpRedirect)
        install(HttpCookies)
        install(HttpTimeout)
    },
) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): NetworkResponse {
        val response = httpClient.request(request.url) {
            method = request.method.toKtorHttpMethod()
            request.headers.forEach { (name, value) -> header(name, value) }
            request.timeoutMillis?.let { timeoutMillis ->
                timeout {
                    requestTimeoutMillis = timeoutMillis
                    connectTimeoutMillis = timeoutMillis
                    socketTimeoutMillis = timeoutMillis
                }
            }
            when (val requestBody = request.body) {
                is NetworkBody.Bytes -> setBody(
                    ByteArrayContent(
                        requestBody.value,
                        requestBody.contentType?.let(ContentType::parse),
                    ),
                )
                is NetworkBody.Text -> setBody(
                    TextContent(
                        requestBody.value,
                        ContentType.parse(requestBody.contentType ?: "text/plain; charset=utf-8"),
                    ),
                )
                null -> Unit
            }
        }
        return NetworkResponse(
            status = response.status.value,
            headers = HttpHeaders(response.headers.entries().associate { (name, values) -> name to values }),
            body = KtorNetworkResponseBody(response.bodyAsChannel()),
            finalUrl = response.call.request.url.toString(),
        )
    }
}

internal expect fun defaultKtorEngineFactory(): HttpClientEngineFactory<*>

private fun HttpMethod.toKtorHttpMethod(): KtorHttpMethod = when (this) {
    HttpMethod.Get -> KtorHttpMethod.Get
    HttpMethod.Post -> KtorHttpMethod.Post
    HttpMethod.Put -> KtorHttpMethod.Put
    HttpMethod.Patch -> KtorHttpMethod.Patch
    HttpMethod.Delete -> KtorHttpMethod.Delete
    HttpMethod.Head -> KtorHttpMethod.Head
    HttpMethod.Options -> KtorHttpMethod.Options
}

private class KtorNetworkResponseBody(
    private val channel: ByteReadChannel,
) : NetworkResponseBody {

    override suspend fun readBytes(): ByteArray = stream().collectBytes()

    override fun stream(): Flow<ByteArray> = flow {
        val buffer = ByteArray(DEFAULT_CHUNK_SIZE)
        while (true) {
            val read = channel.readAvailable(buffer, 0, buffer.size)
            if (read == -1) break
            if (read > 0) emit(buffer.copyOf(read))
        }
    }

    private suspend fun Flow<ByteArray>.collectBytes(): ByteArray {
        val parts = mutableListOf<ByteArray>()
        var totalSize = 0
        collect { chunk ->
            parts += chunk
            totalSize += chunk.size
        }
        val output = ByteArray(totalSize)
        var offset = 0
        parts.forEach { part ->
            part.copyInto(output, offset)
            offset += part.size
        }
        return output
    }
}

private const val DEFAULT_CHUNK_SIZE = 8 * 1024
