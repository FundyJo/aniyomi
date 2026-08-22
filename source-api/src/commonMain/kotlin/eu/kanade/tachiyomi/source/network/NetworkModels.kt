package eu.kanade.tachiyomi.source.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

@Serializable
data class NetworkRequest(
    val url: String,
    val method: HttpMethod = HttpMethod.Get,
    val headers: HttpHeaders = HttpHeaders.Empty,
    val body: NetworkBody? = null,
    val timeoutMillis: Long? = null,
)

data class NetworkResponse(
    val status: Int,
    val headers: HttpHeaders = HttpHeaders.Empty,
    val body: NetworkResponseBody,
    val finalUrl: String,
)

@Serializable
enum class HttpMethod {
    Get,
    Post,
    Put,
    Patch,
    Delete,
    Head,
    Options,
}

@Serializable
sealed interface NetworkBody {
    val contentType: String?

    @Serializable
    data class Text(
        val value: String,
        override val contentType: String? = "text/plain; charset=utf-8",
    ) : NetworkBody

    @Serializable
    data class Bytes(
        val value: ByteArray,
        override val contentType: String? = null,
    ) : NetworkBody {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Bytes) return false
            return value.contentEquals(other.value) && contentType == other.contentType
        }

        override fun hashCode(): Int {
            var result = value.contentHashCode()
            result = 31 * result + (contentType?.hashCode() ?: 0)
            return result
        }
    }
}

sealed interface NetworkResponseBody {
    suspend fun readBytes(): ByteArray

    suspend fun bytes(): ByteArray = readBytes()

    fun stream(): Flow<ByteArray>

    data class ByteArrayBody(private val value: ByteArray) : NetworkResponseBody {
        override suspend fun readBytes(): ByteArray = value

        override fun stream(): Flow<ByteArray> = flowOf(value)
    }

    data class StreamBody(val chunks: Flow<ByteArray>) : NetworkResponseBody {
        override suspend fun readBytes(): ByteArray {
            val parts = mutableListOf<ByteArray>()
            var totalSize = 0
            chunks.collect { chunk ->
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

        override fun stream(): Flow<ByteArray> = chunks
    }
}

@Serializable
data class Cookie(
    val name: String,
    val value: String,
    val domain: String? = null,
    val path: String? = null,
    val expiresAt: Long? = null,
    val expiresAtEpochMillis: Long? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val hostOnly: Boolean = domain == null,
    val persistent: Boolean = expiresAt != null || expiresAtEpochMillis != null,
)
