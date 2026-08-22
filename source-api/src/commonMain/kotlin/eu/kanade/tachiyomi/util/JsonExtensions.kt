package eu.kanade.tachiyomi.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * App provided default [Json] instance. Configured as
 * ```
 * Json {
 *     ignoreUnknownKeys = true
 *     explicitNulls = false
 * }
 * ```
 *
 * @since extensions-lib 16
 */
@OptIn(ExperimentalSerializationApi::class)
val defaultJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
