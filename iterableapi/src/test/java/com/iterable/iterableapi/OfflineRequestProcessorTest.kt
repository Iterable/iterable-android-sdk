package com.iterable.iterableapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class OfflineRequestProcessorTest : BaseTest() {
    private lateinit var requestProcessor: OfflineRequestProcessor
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var healthMonitor: HealthMonitor
    private lateinit var requestDispatcher: IterableRequestDispatcher

    @Before
    fun setUp() {
        taskScheduler = mock(TaskScheduler::class.java)
        healthMonitor = mock(HealthMonitor::class.java)
        requestDispatcher = mock(IterableRequestDispatcher::class.java)
        requestProcessor = OfflineRequestProcessor(
            taskScheduler,
            mock(IterableTaskRunner::class.java),
            mock(IterableTaskStorage::class.java),
            healthMonitor,
            requestDispatcher
        )
    }

    @Test
    fun `offline compatible request is stored when storage is healthy`() {
        `when`(healthMonitor.canSchedule()).thenReturn(true)

        requestProcessor.processPostRequest(
            "api-key",
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            JSONObject(),
            null,
            null,
            null
        )

        val requestCaptor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(taskScheduler).scheduleTask(
            requestCaptor.capture(),
            isNull(),
            isNull()
        )
        assertEquals(
            IterableApiRequest.ProcessorType.OFFLINE,
            requestCaptor.value.processorType
        )
        verifyNoInteractions(requestDispatcher)
    }

    @Test
    fun `request that cannot be stored is dispatched immediately`() {
        `when`(healthMonitor.canSchedule()).thenReturn(false)
        val success = mock(IterableHelper.SuccessHandler::class.java)
        val failure = mock(IterableHelper.FailureHandler::class.java)

        requestProcessor.processPostRequest(
            "api-key",
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            JSONObject(),
            "auth-token",
            success,
            failure
        )

        val request = captureDispatchedRequest()
        assertEquals(
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            request.resourcePath
        )
        assertSame(success, request.successCallback)
        assertSame(failure, request.failureCallback)
        verifyNoInteractions(taskScheduler)
    }

    @Test
    fun `request not supported offline is dispatched immediately`() {
        requestProcessor.processPostRequest(
            "api-key",
            IterableConstants.ENDPOINT_UPDATE_EMAIL,
            JSONObject(),
            null,
            null,
            null
        )

        assertEquals(
            IterableConstants.ENDPOINT_UPDATE_EMAIL,
            captureDispatchedRequest().resourcePath
        )
        verifyNoInteractions(taskScheduler)
    }

    @Test
    fun `all supported offline endpoints use task storage`() {
        val supportedEndpoints = listOf(
            IterableConstants.ENDPOINT_TRACK,
            IterableConstants.ENDPOINT_TRACK_PUSH_OPEN,
            IterableConstants.ENDPOINT_TRACK_PURCHASE,
            IterableConstants.ENDPOINT_TRACK_INAPP_OPEN,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE,
            IterableConstants.ENDPOINT_TRACK_INBOX_SESSION,
            IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY,
            IterableConstants.ENDPOINT_INAPP_CONSUME,
            IterableConstants.ENDPOINT_UPDATE_CART,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION
        )

        supportedEndpoints.forEach {
            assertEquals(true, requestProcessor.isRequestOfflineCompatible(it))
        }
    }

    private fun captureDispatchedRequest(): IterableApiRequest {
        val requestCaptor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(requestDispatcher).execute(requestCaptor.capture())
        return requestCaptor.value
    }
}
