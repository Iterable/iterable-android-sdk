package com.iterable.integration.tests.capture

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@RunWith(AndroidJUnit4::class)
class ForwardingHttpsConnectionTest {

    private val delegate = mock(HttpsURLConnection::class.java)

    @Before
    fun setUp() {
        RequestCapture.clear()
        `when`(delegate.url).thenReturn(URL("https://api.eu.iterable.com/api/users/registerDeviceToken"))
        `when`(delegate.requestMethod).thenReturn("POST")
        `when`(delegate.getRequestProperty("Api-Key")).thenReturn("abcdefghwxyz")
        `when`(delegate.outputStream).thenReturn(ByteArrayOutputStream())
        `when`(delegate.responseCode).thenReturn(200)
        `when`(delegate.inputStream).thenReturn(ByteArrayInputStream(ByteArray(0)))
    }

    @Test
    fun `records the request once across every committing call`() {
        val connection = ForwardingHttpsConnection(delegate)
        connection.outputStream
        connection.responseCode
        connection.responseCode
        connection.inputStream

        val entries = RequestCapture.entries()
        assertEquals(1, entries.size)
        with(entries[0]) {
            assertEquals("POST", method)
            assertEquals("api.eu.iterable.com", host)
            assertEquals("/api/users/registerDeviceToken", path)
            assertEquals("****wxyz", maskedApiKey)
        }
    }

    @Test
    fun `reads the API key before the request is committed`() {
        ForwardingHttpsConnection(delegate).outputStream

        val order = inOrder(delegate)
        order.verify(delegate).getRequestProperty("Api-Key")
        order.verify(delegate).outputStream
    }
}
