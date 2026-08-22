package eu.kanade.tachiyomi.source.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun defaultKtorEngineFactory(): HttpClientEngineFactory<*> = OkHttp
