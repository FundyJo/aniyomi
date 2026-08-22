package eu.kanade.tachiyomi.source.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpHeadersTest {
    @Test
    fun headerLookupIsCaseInsensitive() {
        val headers = HttpHeaders.of("Content-Type" to "text/html", "content-type" to "charset=utf-8")

        assertTrue(headers.contains("CONTENT-TYPE"))
        assertEquals(listOf("text/html", "charset=utf-8"), headers.getAll("content-type"))
        assertEquals("charset=utf-8", headers.get("Content-Type"))
    }

    @Test
    fun emptyHeadersHaveNoValues() {
        assertFalse(HttpHeaders.Empty.contains("User-Agent"))
        assertEquals(emptyList(), HttpHeaders.Empty.getAll("User-Agent"))
    }

    @Test
    fun setReplacesExistingHeaderCaseInsensitively() {
        val headers = HttpHeaders.of("User-Agent" to "Aniyomi", "user-agent" to "Extension")
            .set("USER-AGENT", "Shared")

        assertEquals(listOf("Shared"), headers.getAll("user-agent"))
        assertEquals("Shared", headers.get("User-Agent"))
    }

    @Test
    fun removeDeletesExistingHeaderCaseInsensitively() {
        val headers = HttpHeaders.of("Range" to "bytes=0-", "Referer" to "https://example.org")
            .remove("range")

        assertFalse(headers.contains("RANGE"))
        assertTrue(headers.contains("referer"))
    }

    @Test
    fun iterationPreservesHeaderValues() {
        val headers = HttpHeaders.of("A" to "1", "a" to "2", "B" to "3")

        assertEquals(listOf("A" to "1", "A" to "2", "B" to "3"), headers.toList())
        assertEquals(headers.toList(), headers.toList())
    }
}
