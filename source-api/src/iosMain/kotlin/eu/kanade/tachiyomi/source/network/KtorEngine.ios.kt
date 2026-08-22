package eu.kanade.tachiyomi.source.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun defaultKtorEngineFactory(): HttpClientEngineFactory<*> = Darwin
