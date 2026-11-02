package com.iterable.integration.tests.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class RequestCaptureTest {

    private val factory = CapturingHandlerFactory(URL("https://api.iterable.com/"))

    private fun open(spec: String) = URL(null, spec, factory.createURLStreamHandler("https")).openConnection()

    @Test
    fun `masks key keeping last four characters`() {
        assertEquals("****wxyz", maskApiKey("abcdefghwxyz"))
    }

    @Test
    fun `short key is fully masked`() {
        assertEquals("****", maskApiKey("abc"))
    }

    @Test
    fun `null or blank key returns placeholder`() {
        assertEquals("<none>", maskApiKey(null))
        assertEquals("<none>", maskApiKey("  "))
    }

    @Test
    fun `matches US and EU API hosts only`() {
        assertTrue(isIterableApiHost("api.iterable.com"))
        assertTrue(isIterableApiHost("api.eu.iterable.com"))
        assertFalse(isIterableApiHost("links.iterable.com"))
        assertFalse(isIterableApiHost("api.iterable.com.example.net"))
        assertFalse(isIterableApiHost(null))
    }

    @Test
    fun `only handles https`() {
        assertNull(factory.createURLStreamHandler("http"))
    }

    @Test
    fun `wraps connections to Iterable API hosts`() {
        assertTrue(open("https://api.iterable.com/api/users/registerDeviceToken") is ForwardingHttpsConnection)
        assertTrue(open("https://api.eu.iterable.com/api/users/disableDevice") is ForwardingHttpsConnection)
    }

    @Test
    fun `leaves other hosts to the platform`() {
        assertFalse(open("https://links.iterable.com/u/click") is ForwardingHttpsConnection)
    }

    @Test
    fun `forwards request configuration to the platform connection`() {
        val connection = open("https://api.eu.iterable.com/api/events/track") as ForwardingHttpsConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Api-Key", "abcdefghwxyz")

        assertEquals("POST", connection.requestMethod)
        assertTrue(connection.doOutput)
        assertEquals("abcdefghwxyz", connection.getRequestProperty("Api-Key"))
        assertEquals("api.eu.iterable.com", connection.url.host)
    }
}
