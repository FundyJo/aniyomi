package eu.kanade.tachiyomi.source.network

interface NetworkClient {
    suspend fun execute(request: NetworkRequest): NetworkResponse
}
