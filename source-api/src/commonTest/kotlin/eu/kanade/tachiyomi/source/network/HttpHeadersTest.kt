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
}
