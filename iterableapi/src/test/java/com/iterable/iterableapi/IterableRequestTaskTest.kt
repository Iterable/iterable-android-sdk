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
import org.junit.Assert.assertTrue
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.robolectric.util.ReflectionHelpers

@RunWith(TestRunner::class)
class IterableRequestTaskTest {
    private lateinit var requestDispatcher: IterableRequestDispatcher

    @Before
    fun setUp() {
        requestDispatcher = mock(IterableRequestDispatcher::class.java)
        `when`(requestDispatcher.elapsedRealtimeMillis()).thenReturn(1_000L)
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
    fun `policy retries server errors with fixed spacing until max attempts`() {
        for (retryCount in 0..3) {
            val request = policyRequest()

            IterableRequestTask(request, retryCount, requestDispatcher).handleResponse(serverError())

            verify(requestDispatcher).retry(request, retryCount + 1, 2000)
        }
        val failure = mock(IterableHelper.FailureHandler::class.java)

        IterableRequestTask(policyRequest(failure), 4, requestDispatcher).handleResponse(serverError())

        verify(failure).onFailure(eq("Server unavailable"), any())
    }

    @Test
    fun `policy stops retrying at the total deadline`() {
        val request = policyRequest()
        `when`(requestDispatcher.elapsedRealtimeMillis()).thenReturn(1_000L + 57_500L)
        IterableRequestTask(request, 1, requestDispatcher).handleResponse(serverError())
        verify(requestDispatcher).retry(request, 2, 2000)

        val failure = mock(IterableHelper.FailureHandler::class.java)
        `when`(requestDispatcher.elapsedRealtimeMillis()).thenReturn(1_000L + 57_501L)
        IterableRequestTask(policyRequest(failure), 1, requestDispatcher).handleResponse(serverError())

        verify(failure).onFailure(eq("Server unavailable"), any())
        verify(requestDispatcher).retry(any(IterableApiRequest::class.java), anyInt(), anyLong())
    }

    @Test
    fun `policy retries a network failure`() {
        val request = policyRequest()

        IterableRequestTask(request, 0, requestDispatcher).handleResponse(
            IterableApiResponse.networkFailure(null, "timeout")
        )

        verify(requestDispatcher).retry(request, 1, 2000)
    }

    @Test
    fun `policy treats client errors, other code 0 failures and failed 2xx as terminal`() {
        val terminal = listOf(
            IterableApiResponse.failure(429, null, null, "Too many requests"),
            IterableApiResponse.failure(400, null, null, "Invalid Request"),
            IterableApiResponse.failure(401, null, jwtError(), "JWT Authorization header error"),
            IterableApiResponse.failure(0, null, null, "SSL handshake failed"),
            IterableApiResponse.failure(200, "", null, "No data received"),
            IterableApiResponse.failure(200, null, null, "Read timed out"),
            IterableApiResponse.failure(204, "", null, "Received non-200 response: 204")
        )
        for (response in terminal) {
            val failure = mock(IterableHelper.FailureHandler::class.java)

            IterableRequestTask(policyRequest(failure), 0, requestDispatcher).handleResponse(response)

            verify(failure).onFailure(eq(response.errorMessage), any())
        }
        verifyNoRetry()
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
    fun `policy applies only when enabled locally and by remote autoRetry`() {
        assertSame(enabledPolicy, activePolicyFor(enabledPolicy, autoRetry = true))
        assertNull(activePolicyFor(enabledPolicy, autoRetry = false))
        assertNull(activePolicyFor(IterableNetworkRetryPolicy.DEFAULT, autoRetry = true))
    }

    @Test
    fun `auth retry keeps the network retry policy`() {
        val request = policyRequest()

        IterableRequestTask.retryRequestWithNewAuthToken("fresh-token", request, requestDispatcher)

        assertSame(enabledPolicy, captureDispatchedRequest().networkRetryPolicy)
    }

    @Test
    fun `auth retry keeps the attempt count and the deadline start`() {
        val request = policyRequest().apply { networkRetryAttempts = 3 }

        IterableRequestTask.retryRequestWithNewAuthToken("fresh-token", request, requestDispatcher)

        val captor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(requestDispatcher).executeRetry(captor.capture(), eq(3))
        assertEquals(1_000L, captor.value.networkRetryStartMillis)
    }

    @Test
    fun `deadline starts at the first attempt and is not restarted by retries`() {
        val clock = ManualClock().apply { now = 100_000L }
        val retries = PendingRetries()
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val server = MockWebServer().apply {
            repeat(5) { enqueue(MockResponse().setResponseCode(500).setBody("{}")) }
            start()
        }
        IterableRequestTask.overrideUrl = server.url("/").toString()

        try {
            dispatcher(clock, retries).execute(policyRequest(failure).apply { networkRetryStartMillis = 0L })
            assertEquals(1, retries.tasks.size)

            clock.now = 100_000L + 58_500L
            retries.runNext()

            assertEquals(2, server.requestCount)
            assertTrue(retries.tasks.isEmpty())
            verify(failure).onFailure(eq("Internal Server Error"), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `retry that starts after the total deadline is not sent and delivers the failure`() {
        val clock = ManualClock()
        val retries = PendingRetries()
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    clock.now += 57_000L
                    return MockResponse().setResponseCode(500).setBody("{}")
                }
            }
            start()
        }
        IterableRequestTask.overrideUrl = server.url("/").toString()

        try {
            dispatcher(clock, retries).execute(policyRequest(failure))
            assertEquals(1, retries.tasks.size)
            verifyNoInteractions(failure)

            clock.now = 600_000L
            retries.runNext()

            assertEquals(1, server.requestCount)
            assertTrue(retries.tasks.isEmpty())
            verify(failure, times(1)).onFailure(eq("Internal Server Error"), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `retry approved at the last moment is sent when it starts within the tolerance`() {
        val clock = ManualClock()
        val retries = PendingRetries()
        val success = mock(IterableHelper.SuccessHandler::class.java)
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val lastApproval = 57_500L
        val server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (requestCount == 1) {
                        clock.now = lastApproval
                        return MockResponse().setResponseCode(500).setBody("{}")
                    }
                    return MockResponse().setResponseCode(200).setBody("{}")
                }
            }
            start()
        }
        IterableRequestTask.overrideUrl = server.url("/").toString()

        try {
            dispatcher(clock, retries).execute(policyRequest(failure).apply { successCallback = success })
            assertEquals(1, retries.tasks.size)

            clock.now = 60_000L
            retries.runNext()

            assertEquals(2, server.requestCount)
            verify(success).onSuccess(any())
            verifyNoInteractions(failure)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `failure just after the last approval moment is not retried`() {
        val clock = ManualClock()
        val retries = PendingRetries()
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    clock.now = 57_501L
                    return MockResponse().setResponseCode(500).setBody("{}")
                }
            }
            start()
        }
        IterableRequestTask.overrideUrl = server.url("/").toString()

        try {
            dispatcher(clock, retries).execute(policyRequest(failure))

            assertTrue(retries.tasks.isEmpty())
            verify(failure).onFailure(eq("Internal Server Error"), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `JWT re-send continues the attempt count of the same call`() {
        val previousApi = IterableApi.sharedInstance
        IterableApi.sharedInstance = IterableTestUtils.newApiWithInlineRequests()
        ReflectionHelpers.setField(IterableApi.sharedInstance, "authManager",
            ImmediateRefreshAuthManager(IterableApi.sharedInstance))
        val retries = PendingRetries()
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(500).setBody("{}"))
            enqueue(MockResponse().setResponseCode(500).setBody("{}"))
            enqueue(MockResponse().setResponseCode(401).setBody(jwtError().toString()))
            repeat(5) { enqueue(MockResponse().setResponseCode(500).setBody("{}")) }
            start()
        }
        IterableRequestTask.overrideUrl = server.url("/").toString()
        val threeAttempts = IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).setMaxAttempts(3).build()

        try {
            dispatcher(ManualClock(), retries).execute(request().apply { networkRetryPolicy = threeAttempts })
            var guard = 0
            while (retries.tasks.isNotEmpty() && guard++ < 10) {
                retries.runNext()
            }

            assertEquals(4, server.requestCount)
        } finally {
            server.shutdown()
            IterableApi.sharedInstance = previousApi
        }
    }

    private class ManualClock : IterableRequestDispatcher.Clock {
        @Volatile var now = 0L
        override fun elapsedRealtimeMillis() = now
    }

    private class PendingRetries : IterableRequestDispatcher.RetryScheduler {
        val tasks = mutableListOf<Runnable>()
        override fun schedule(runnable: Runnable, delayMs: Long) {
            tasks += runnable
        }
        fun runNext() = tasks.removeAt(0).run()
    }

    private fun dispatcher(clock: IterableRequestDispatcher.Clock, retries: PendingRetries) =
        IterableRequestDispatcher(Runnable::run, Runnable::run, Runnable::run, retries, clock)

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

    private val enabledPolicy = IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).build()

    private fun policyRequest(onFailure: IterableHelper.FailureHandler? = null): IterableApiRequest {
        return request(onFailure = onFailure).apply {
            networkRetryPolicy = enabledPolicy
            networkRetryStartMillis = 1_000L
        }
    }

    private fun activePolicyFor(
        localPolicy: IterableNetworkRetryPolicy,
        autoRetry: Boolean
    ): IterableNetworkRetryPolicy? {
        val remote = IterableRemoteRetryConfig().apply { isAutoRetryEnabled = autoRetry }
        OnlineRequestProcessor(requestDispatcher, { localPolicy }, remote)
            .processPostRequest("api-key", "api/test", JSONObject(), null, null, null)
        val captor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(requestDispatcher, org.mockito.Mockito.atLeastOnce()).execute(captor.capture())
        return captor.value.networkRetryPolicy
    }

    private fun execute(baseUrl: String): IterableApiResponse {
        IterableRequestTask.overrideUrl = baseUrl
        return IterableRequestTask.executeApiRequest(request(), requestDispatcher)
    }

    private fun jwtError(): JSONObject {
        return JSONObject().put("code", IterableNetworkRetryClassifier.ERROR_CODE_INVALID_JWT_PAYLOAD)
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
