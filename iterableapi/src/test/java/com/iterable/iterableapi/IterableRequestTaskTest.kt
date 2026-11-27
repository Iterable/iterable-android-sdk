package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.util.concurrent.CountDownLatch
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlin.concurrent.thread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

@RunWith(TestRunner::class)
class IterableRequestTaskTest {
    private lateinit var requestDispatcher: IterableRequestDispatcher

    @Before
    fun setUp() {
        requestDispatcher = mock(IterableRequestDispatcher::class.java)
    }

    @Test
    fun `server errors follow the legacy retry schedule`() {
        val expectedDelays = longArrayOf(0, 0, 0, 6000, 8000, 10000)

        for (code in intArrayOf(500, 503, 599)) {
            for (retryCount in expectedDelays.indices) {
                val success = mock(IterableHelper.SuccessHandler::class.java)
                val failure = mock(IterableHelper.FailureHandler::class.java)
                val request = request(success, failure)

                IterableRequestTask(request, retryCount, requestDispatcher).handleResponse(serverError(code))

                verify(requestDispatcher).retry(request, retryCount + 1, expectedDelays[retryCount])
                verifyNoInteractions(success, failure)
            }
        }
    }

    @Test
    fun `code 0 and client errors are not retried by the legacy loop`() {
        for (code in intArrayOf(0, 400, 401, 404, 429)) {
            val failure = mock(IterableHelper.FailureHandler::class.java)

            IterableRequestTask(request(onFailure = failure), 0, requestDispatcher).handleResponse(
                IterableApiResponse.failure(code, null, null, "error $code")
            )

            verify(failure).onFailure("error $code", null)
        }
        verifyNoRetry()
    }

    @After
    fun tearDown() {
        IterableRequestTask.overrideUrl = null
    }

    @Test
    fun `legacy loop ignores the network failure marker`() {
        val failure = mock(IterableHelper.FailureHandler::class.java)

        IterableRequestTask(request(onFailure = failure), 0, requestDispatcher).handleResponse(
            IterableApiResponse.networkFailure(null, "timeout")
        )

        verify(failure).onFailure("timeout", null)
        verifyNoRetry()
    }

    @Test
    fun `refused connection is marked as a network failure`() {
        val server = MockWebServer().apply { start() }
        val url = server.url("/").toString()
        server.shutdown()

        val response = execute(url)

        assertEquals(0, response.responseCode)
        assertTrue(response.networkFailure)
    }

    @Test
    fun `dropped connection is marked as a network failure`() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            start()
        }

        val response = execute(server.url("/").toString())
        server.shutdown()

        assertEquals(0, response.responseCode)
        assertTrue(response.networkFailure)
    }

    @Test
    fun `read timeout is marked as a network failure`() {
        val release = CountDownLatch(1)
        val response = ServerSocket(0).use { server ->
            thread {
                server.accept().use { release.await() }
            }
            try {
                execute("http://127.0.0.1:${server.localPort}/")
            } finally {
                release.countDown()
            }
        }

        assertEquals(0, response.responseCode)
        assertTrue(response.errorMessage, response.networkFailure)
    }

    @Test
    fun `device-style dropped connections thrown by the connection are network failures`() {
        val previous = System.getProperty("java.protocol.handler.pkgs")
        System.setProperty("java.protocol.handler.pkgs", com.iterable.iterableapi.testprotocol.dropped.Handler.PACKAGES)
        try {
            for (host in listOf("eof", "eof-cause", "reset-cause")) {
                val response = execute("dropped://$host/")
                assertEquals(host, 0, response.responseCode)
                assertTrue(host, response.networkFailure)
            }
            val other = execute("dropped://other/")
            assertEquals("stream closed", other.errorMessage)
            assertFalse(other.networkFailure)
        } finally {
            if (previous == null) {
                System.clearProperty("java.protocol.handler.pkgs")
            } else {
                System.setProperty("java.protocol.handler.pkgs", previous)
            }
        }
    }

    @Test
    fun `TLS handshake closed by the server is a network failure`() {
        val response = ServerSocket(0).use { server ->
            thread {
                server.accept().use { socket ->
                    val input = java.io.DataInputStream(socket.getInputStream())
                    val header = ByteArray(5)
                    input.readFully(header)
                    val length = ((header[3].toInt() and 0xff) shl 8) or (header[4].toInt() and 0xff)
                    input.readFully(ByteArray(length))
                }
            }
            execute("https://127.0.0.1:${server.localPort}/")
        }

        assertEquals(0, response.responseCode)
        assertTrue(response.errorMessage, response.networkFailure)
    }

    @Test
    fun `timeouts, DNS failures and dropped connections are network failures`() {
        val networkFailures = listOf(
            SocketTimeoutException("Read timed out"),
            UnknownHostException("api.iterable.com"),
            ConnectException("Connection refused"),
            SocketException("Connection reset"),
            EOFException(),
            IOException("unexpected end of stream on http://api.iterable.com/...", EOFException("\\n not found")),
            IOException("unexpected end of stream", SocketException("Connection reset")),
            IOException("outer", IOException("inner", EOFException())),
            IOException("wrapped", SocketTimeoutException("Read timed out")),
            IOException("wrapped", UnknownHostException("api.iterable.com")),
            SSLException("Read error", SocketException("Connection reset"))
        )
        for (e in networkFailures) {
            assertTrue(e.toString(), IterableNetworkRetryClassifier.isNetworkFailure(e))
        }
    }

    @Test
    fun `other IO failures are not network failures`() {
        val otherFailures = listOf(
            IOException("stream closed"),
            IOException("wrapped", IllegalStateException("not network")),
            SSLHandshakeException("handshake failed"),
            MalformedURLException("no protocol"),
            SSLHandshakeException("handshake failed").apply {
                initCause(CertificateException("Could not parse certificate", EOFException()))
            },
            SSLHandshakeException("handshake failed").apply {
                initCause(CertPathValidatorException("path validation failed", EOFException()))
            }
        )
        for (e in otherFailures) {
            assertFalse(e.toString(), IterableNetworkRetryClassifier.isNetworkFailure(e))
        }
    }

    @Test
    fun `TLS and bad URL failures are not network failures`() {
        val tls = ServerSocket(0).use { server ->
            thread {
                server.accept().use { socket ->
                    socket.getInputStream().read(ByteArray(512))
                    socket.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray())
                    socket.getOutputStream().flush()
                    socket.getInputStream().readBytes()
                }
            }
            execute("https://127.0.0.1:${server.localPort}/")
        }
        val badUrl = execute("notaprotocol://api.iterable.com/")

        for (response in listOf(tls, badUrl)) {
            assertEquals(0, response.responseCode)
            assertFalse(response.errorMessage, response.networkFailure)
        }
    }

    @Test
    fun `server error after retry limit calls failure exactly once`() {
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val request = request(onFailure = failure)
        val responseData = JSONObject()

        // A literal on purpose, so changing MAX_RETRY_COUNT fails this test.
        IterableRequestTask(request, 6, requestDispatcher).handleResponse(
            IterableApiResponse.failure(
                500,
                """{"msg":"Server unavailable"}""",
                responseData,
                "Server unavailable"
            )
        )

        verify(requestDispatcher, never()).retry(
            any(IterableApiRequest::class.java),
            anyInt(),
            anyLong()
        )
        verify(failure).onFailure("Server unavailable", responseData)
        assertEquals(
            500,
            responseData.getInt(IterableConstants.HTTP_STATUS_CODE)
        )
    }

    @Test
    fun `auth retry keeps handlers only for opted-in online requests`() {
        val plain = mock(IterableHelper.SuccessHandler::class.java)
        val optedIn = IterableApiRequest.AuthRetrySuccessHandler { }
        val rows = listOf(
            Triple(IterableApiRequest.ProcessorType.ONLINE, plain, false),
            Triple(IterableApiRequest.ProcessorType.OFFLINE, plain, false),
            Triple(IterableApiRequest.ProcessorType.ONLINE, optedIn, true),
            Triple(IterableApiRequest.ProcessorType.OFFLINE, optedIn, false)
        )
        for ((processorType, success, kept) in rows) {
            val failure = mock(IterableHelper.FailureHandler::class.java)
            val request = request(success, failure).apply { setProcessorType(processorType) }
            val dispatcher = mock(IterableRequestDispatcher::class.java)

            IterableRequestTask.retryRequestWithNewAuthToken("fresh-token", request, dispatcher)

            val captor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
            verify(dispatcher).executeRetry(captor.capture(), eq(0))
            val retried = captor.value
            val row = "$processorType ${if (success === optedIn) "opted in" else "plain"}"
            assertEquals(row, "fresh-token", retried.authToken)
            assertEquals(row, request.resourcePath, retried.resourcePath)
            assertSame(row, request.json, retried.json)
            assertEquals(row, IterableApiRequest.ProcessorType.ONLINE, retried.processorType)
            assertSame(row, if (kept) success else null, retried.successCallback)
            assertSame(row, if (kept) failure else null, retried.failureCallback)
        }
    }

    @Test
    fun `auth retry keeps the legacy callback and replaces token`() {
        val callback = mock(IterableHelper.IterableActionHandler::class.java)
        val request = IterableApiRequest(
            "api-key",
            "api/test",
            JSONObject(),
            IterableApiRequest.GET,
            "expired-token",
            callback
        )

        IterableRequestTask.retryRequestWithNewAuthToken(
            "fresh-token",
            request,
            requestDispatcher
        )

        val retriedRequest = captureDispatchedRequest()
        assertEquals("fresh-token", retriedRequest.authToken)
        assertSame(callback, retriedRequest.legacyCallback)
    }

    @Test
    fun `auth retry preserves a stale request guard`() {
        val request = request().apply {
            setRetryState { false }
        }

        IterableRequestTask.retryRequestWithNewAuthToken(
            "fresh-token",
            request,
            requestDispatcher
        )

        assertFalse(captureDispatchedRequest().canRetry())
    }

    private fun execute(baseUrl: String): IterableApiResponse {
        IterableRequestTask.overrideUrl = baseUrl
        return IterableRequestTask.executeApiRequest(request(), requestDispatcher)
    }

    private fun verifyNoRetry() {
        verify(requestDispatcher, never()).retry(
            any(IterableApiRequest::class.java),
            anyInt(),
            anyLong()
        )
    }

    private fun captureDispatchedRequest(): IterableApiRequest {
        val requestCaptor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(requestDispatcher).executeRetry(requestCaptor.capture(), eq(0))
        return requestCaptor.value
    }

    private fun request(
        onSuccess: IterableHelper.SuccessHandler? = null,
        onFailure: IterableHelper.FailureHandler? = null
    ): IterableApiRequest {
        return IterableApiRequest(
            "api-key",
            "api/test",
            JSONObject(),
            IterableApiRequest.POST,
            "expired-token",
            onSuccess,
            onFailure
        )
    }

    private fun serverError(code: Int = 500): IterableApiResponse {
        return IterableApiResponse.failure(
            code,
            """{"msg":"Server unavailable"}""",
            JSONObject(),
            "Server unavailable"
        )
    }
}
