package com.iterable.iterableapi

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.TimeUnit

class OfflineRequestProcessorTest : BaseTest() {
    private lateinit var requestProcessor: OfflineRequestProcessor
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var healthMonitor: HealthMonitor
    private lateinit var immediateRequestDispatcher: IterableRequestDispatcher
    private val remoteRetryConfig = IterableRemoteRetryConfig()
    private val enabledPolicy = IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).build()
    private lateinit var authManager: ImmediateRefreshAuthManager

    @Before
    fun setUp() {
        taskScheduler = mock(TaskScheduler::class.java)
        healthMonitor = mock(HealthMonitor::class.java)
        immediateRequestDispatcher = mock(IterableRequestDispatcher::class.java)
        requestProcessor = OfflineRequestProcessor(
            taskScheduler,
            mock(IterableTaskRunner::class.java),
            mock(IterableTaskStorage::class.java),
            healthMonitor,
            immediateRequestDispatcher,
            remoteRetryConfig,
            { enabledPolicy }
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
        verifyNoInteractions(immediateRequestDispatcher)
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

    @Test
    fun `immediate requests get the network retry policy only when remote autoRetry is on`() {
        val sends = listOf<Pair<String, () -> Unit>>(
            "GET" to {
                requestProcessor.processGetRequest("api-key", IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, JSONObject(), null, null, null)
            },
            "legacy GET" to {
                requestProcessor.processGetRequest("api-key", IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, JSONObject(), null,
                    null as IterableHelper.IterableActionHandler?)
            },
            "POST" to {
                requestProcessor.processPostRequest("api-key", IterableConstants.ENDPOINT_UPDATE_EMAIL, JSONObject(), null, null, null)
            }
        )
        for (autoRetry in listOf(true, false)) {
            remoteRetryConfig.isAutoRetryEnabled = autoRetry
            for ((name, send) in sends) {
                clearInvocations(immediateRequestDispatcher)
                send()
                assertSame("$name, autoRetry $autoRetry", if (autoRetry) enabledPolicy else null,
                    captureDispatchedRequest().networkRetryPolicy)
            }
        }
    }

    @Test
    fun `stored requests do not get the network retry policy`() {
        remoteRetryConfig.isAutoRetryEnabled = true
        `when`(healthMonitor.canSchedule()).thenReturn(true)

        requestProcessor.processPostRequest("api-key", IterableConstants.ENDPOINT_TRACK, JSONObject(), null, null, null)

        val requestCaptor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(taskScheduler).scheduleTask(requestCaptor.capture(), isNull(), isNull())
        assertNull(requestCaptor.value.networkRetryPolicy)
    }

    @Test
    fun `request sent after a failed task insert is not re-sent inline on a JWT 401 when autoRetry is on`() {
        remoteRetryConfig.isAutoRetryEnabled = true
        val server = sendAfterFailedInsertAndGetJwt401()

        try {
            assertNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("autoRetry is on, so the JWT 401 is not re-sent inline", 1, server.requestCount)
            assertEquals(listOf(IterableAuthRefreshReason.JWT_401), authManager.refreshReasons)
        } finally {
            server.shutdown()
            IterableRequestTask.overrideUrl = null
        }
    }

    @Test
    fun `request sent after a failed task insert is re-sent inline on a JWT 401 when autoRetry is off`() {
        remoteRetryConfig.isAutoRetryEnabled = false
        val server = sendAfterFailedInsertAndGetJwt401()

        try {
            assertNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            val resend = server.takeRequest(1, TimeUnit.SECONDS)
            assertNotNull("With autoRetry off a JWT 401 is re-sent inline, as on master", resend)
            assertEquals(
                IterableConstants.HEADER_SDK_AUTH_FORMAT + ImmediateRefreshAuthManager.FRESH_TOKEN,
                resend!!.getHeader(IterableConstants.HEADER_SDK_AUTHORIZATION)
            )
        } finally {
            server.shutdown()
            IterableRequestTask.overrideUrl = null
        }
    }

    private fun sendAfterFailedInsertAndGetJwt401(): MockWebServer {
        `when`(healthMonitor.canSchedule()).thenReturn(true)
        val storage = mock(IterableTaskStorage::class.java)
        val dispatcher = IterableRequestDispatcher(Runnable::run, Runnable::run) { runnable, _ -> runnable.run() }
        val processor = OfflineRequestProcessor(
            TaskScheduler(storage, mock(IterableTaskRunner::class.java), dispatcher),
            mock(IterableTaskRunner::class.java),
            storage,
            healthMonitor,
            dispatcher,
            remoteRetryConfig,
            { enabledPolicy }
        )
        authManager = ImmediateRefreshAuthManager(IterableApi.getInstance())
        ReflectionHelpers.setField(IterableApi.getInstance(), "authManager", authManager)
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(401).setBody(
                """{"code":"${IterableNetworkRetryClassifier.ERROR_CODE_INVALID_JWT_PAYLOAD}"}"""))
            enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            start()
        }
        IterableApi.overrideURLEndpointPath(server.url("").toString())
        processor.processPostRequest("api-key", IterableConstants.ENDPOINT_TRACK, JSONObject(), "expired-token", null, null)
        return server
    }

    private fun captureDispatchedRequest(): IterableApiRequest {
        val requestCaptor = ArgumentCaptor.forClass(IterableApiRequest::class.java)
        verify(immediateRequestDispatcher).execute(requestCaptor.capture())
        return requestCaptor.value
    }
}
