package eu.kanade.tachiyomi.source.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CookieTest {
    @Test
    fun persistentCookieIsDerivedFromExpiry() {
        val cookie = Cookie(name = "sid", value = "value", domain = "example.org", expiresAt = 1_000L)

        assertTrue(cookie.persistent)
        assertFalse(cookie.hostOnly)
    }

    @Test
    fun hostOnlyCookieDoesNotRequireDomain() {
        val cookie = Cookie(name = "sid", value = "value")

        assertTrue(cookie.hostOnly)
        assertFalse(cookie.persistent)
    }
}
