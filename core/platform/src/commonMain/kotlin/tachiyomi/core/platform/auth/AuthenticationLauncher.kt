package tachiyomi.core.platform.auth

@JvmInline
value class SecureStorageKey(val value: String)

data class AuthenticationRequest(
    val authorizationUrl: String,
    val callbackScheme: String,
    val callbackHost: String? = null,
)

data class AuthenticationResult(
    val callbackUrl: String,
)

interface AuthenticationLauncher {
    suspend fun launch(request: AuthenticationRequest): AuthenticationResult
}

interface SecureStorage {
    suspend fun put(key: SecureStorageKey, value: ByteArray)

    suspend fun get(key: SecureStorageKey): ByteArray?

    suspend fun remove(key: SecureStorageKey)
}
