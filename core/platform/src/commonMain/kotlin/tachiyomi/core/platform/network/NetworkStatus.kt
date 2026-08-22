package tachiyomi.core.platform.network

import kotlinx.coroutines.flow.StateFlow

data class NetworkState(
    val isOnline: Boolean,
    val isMetered: Boolean? = null,
)

interface NetworkStatus {
    val state: StateFlow<NetworkState>
}
