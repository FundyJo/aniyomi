package eu.kanade.tachiyomi.source.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

internal actual fun defaultKtorEngineFactory(): HttpClientEngineFactory<*> = CIO
