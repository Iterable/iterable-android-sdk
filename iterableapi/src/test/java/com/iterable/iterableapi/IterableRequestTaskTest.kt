package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
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
    fun `server error schedules a retry without calling a terminal callback`() {
        val success = mock(IterableHelper.SuccessHandler::class.java)
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val request = request(success, failure)

        IterableRequestTask(request, 0, requestDispatcher).handleResponse(
            serverError()
        )

        verify(requestDispatcher).retry(request, 1, 0)
        verifyNoInteractions(success, failure)
    }

    @Test
    fun `later server error retries use the existing backoff`() {
        val request = request()

        IterableRequestTask(request, 3, requestDispatcher).handleResponse(
            serverError()
        )

        verify(requestDispatcher).retry(
            request,
            4,
            IterableRequestTask.RETRY_DELAY_MS * 3
        )
    }

    @Test
    fun `server error after retry limit calls failure exactly once`() {
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val request = request(onFailure = failure)
        val responseData = JSONObject()

        IterableRequestTask(
            request,
            IterableRequestTask.MAX_RETRY_COUNT + 1,
            requestDispatcher
        ).handleResponse(
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
    fun `auth retry does not attach modern callbacks to the retried request`() {
        val success = mock(IterableHelper.SuccessHandler::class.java)
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val request = request(success, failure).apply {
            setProcessorType(IterableApiRequest.ProcessorType.OFFLINE)
        }

        IterableRequestTask.retryRequestWithNewAuthToken(
            "fresh-token",
            request,
            requestDispatcher
        )

        val retriedRequest = captureDispatchedRequest()
        assertEquals("fresh-token", retriedRequest.authToken)
        assertEquals(request.resourcePath, retriedRequest.resourcePath)
        assertSame(request.json, retriedRequest.json)
        assertNull(retriedRequest.successCallback)
        assertNull(retriedRequest.failureCallback)
        assertEquals(
            IterableApiRequest.ProcessorType.ONLINE,
            retriedRequest.processorType
        )
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

    private fun captureDispatchedRequest(): IterableApiRequest {
        val requestCaptor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(requestDispatcher).execute(requestCaptor.capture())
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

    private fun serverError(): IterableApiResponse {
        return IterableApiResponse.failure(
            500,
            """{"msg":"Server unavailable"}""",
            JSONObject(),
            "Server unavailable"
        )
    }
}
