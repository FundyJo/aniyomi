package eu.kanade.tachiyomi.source.network

import kotlinx.serialization.Serializable

@Serializable
data class HttpHeaders(
    val values: Map<String, List<String>> = emptyMap(),
) : Iterable<Pair<String, String>> {

    fun get(name: String): String? = getAll(name).lastOrNull()

    fun getAll(name: String): List<String> {
        return values.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value.orEmpty()
    }

    fun contains(name: String): Boolean = values.keys.any { it.equals(name, ignoreCase = true) }

    operator fun plus(header: Pair<String, String>): HttpHeaders {
        val existingKey = values.keys.firstOrNull { it.equals(header.first, ignoreCase = true) } ?: header.first
        return copy(values = values + (existingKey to (values[existingKey].orEmpty() + header.second)))
    }

    fun toList(): List<Pair<String, String>> = values.flatMap { (name, headerValues) ->
        headerValues.map { value -> name to value }
    }

    fun toSingleValueMap(): Map<String, String> = values.mapValues { it.value.lastOrNull().orEmpty() }

    override fun iterator(): Iterator<Pair<String, String>> = toList().iterator()

    companion object {
        val Empty = HttpHeaders()

        fun of(vararg headers: Pair<String, String>): HttpHeaders {
            return headers.fold(Empty) { result, header -> result + header }
        }
    }
}
