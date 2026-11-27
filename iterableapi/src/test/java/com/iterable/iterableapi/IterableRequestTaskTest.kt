package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import java.net.MalformedURLException
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
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
    fun `connection IO failures are marked only when they are network failures`() {
        val server = MockWebServer().apply { start() }
        val refused = server.url("/").toString()
        server.shutdown()
        val rows = listOf(
            refused to true,
            // The test protocol throws what OkHttp throws on devices for a dropped connection.
            "dropped://api.iterable.com/" to true,
            "notaprotocol://api.iterable.com/" to false
        )
        val previous = System.getProperty("java.protocol.handler.pkgs")
        System.setProperty("java.protocol.handler.pkgs", com.iterable.iterableapi.testprotocol.dropped.Handler.PACKAGES)
        try {
            for ((url, networkFailure) in rows) {
                val response = execute(url)
                assertEquals(url, 0, response.responseCode)
                assertEquals(url, networkFailure, response.networkFailure)
                if (!networkFailure) {
                    val thrown = assertThrows(MalformedURLException::class.java) { URL(url) }
                    assertEquals(url, thrown.message, response.errorMessage)
                }
            }
        } finally {
            if (previous == null) {
                System.clearProperty("java.protocol.handler.pkgs")
            } else {
                System.setProperty("java.protocol.handler.pkgs", previous)
            }
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
