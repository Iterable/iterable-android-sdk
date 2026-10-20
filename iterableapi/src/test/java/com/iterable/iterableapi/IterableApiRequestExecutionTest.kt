package com.iterable.iterableapi

import android.os.Looper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

class IterableApiRequestExecutionTest : BaseTest() {
    private lateinit var server: MockWebServer
    private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()
    private val updateEmailResponses = ConcurrentLinkedQueue<MockResponse>()

    @Before
    fun setUp() {
        getContext()
            .getSharedPreferences(
                IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION,
                0
            )
            .edit()
            .clear()
            .commit()

        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                recordedRequests.add(request)
                if (request.isFor(IterableConstants.ENDPOINT_UPDATE_EMAIL)) {
                    return updateEmailResponses.poll() ?: successfulResponse()
                }
                return successfulResponse()
            }
        }

        IterableApi.overrideURLEndpointPath(server.url("/").toString())
        IterableApi.initialize(
            getContext(),
            "api-key",
            IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setEnableEmbeddedMessaging(true)
                .build()
        )
        IterableApi.getInstance().setEmail("current@example.com")
        runMainCallbacks()
        recordedRequests.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `updating email calls the client success callback on main`() {
        updateEmailResponses.add(successfulResponse())
        var callbackLooper: Looper? = null
        var failureCalled = false

        IterableApi.getInstance().updateEmail(
            "new@example.com",
            IterableHelper.SuccessHandler {
                callbackLooper = Looper.myLooper()
            },
            IterableHelper.FailureHandler { _, _ ->
                failureCalled = true
            }
        )
        runMainCallbacks()

        assertEquals("new@example.com", IterableApi.getInstance().email)
        assertSame(Looper.getMainLooper(), callbackLooper)
        assertFalse(failureCalled)
        assertEquals(
            "current@example.com",
            updateEmailRequests().single().bodyAsJson().getString("currentEmail")
        )
    }

    @Test
    fun `updating email calls the client failure callback on main`() {
        updateEmailResponses.add(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"msg":"Invalid email"}""")
        )
        var callbackLooper: Looper? = null
        var failureReason: String? = null
        var responseData: JSONObject? = null

        IterableApi.getInstance().updateEmail(
            "invalid",
            IterableHelper.SuccessHandler {
                fail("Success callback should not be called")
            },
            IterableHelper.FailureHandler { reason, data ->
                callbackLooper = Looper.myLooper()
                failureReason = reason
                responseData = data
            }
        )
        runMainCallbacks()

        assertEquals("current@example.com", IterableApi.getInstance().email)
        assertSame(Looper.getMainLooper(), callbackLooper)
        assertEquals("Invalid email", failureReason)
        assertEquals(400, responseData?.getInt(IterableConstants.HTTP_STATUS_CODE))
        assertEquals(1, updateEmailRequests().size)
    }

    @Test
    fun `getting embedded messages calls the legacy client callback on main`() {
        var callbackLooper: Looper? = null
        var callbackData: String? = null

        IterableApi.getInstance().getEmbeddedMessages(
            arrayOf(123L),
            IterableHelper.IterableActionHandler { data ->
                callbackLooper = Looper.myLooper()
                callbackData = data
            }
        )
        runMainCallbacks()

        assertSame(Looper.getMainLooper(), callbackLooper)
        assertEquals("{}", callbackData)
        assertEquals(
            1,
            recordedRequests.count {
                it.isFor(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES)
            }
        )
    }

    @Test
    fun `server error is retried before calling success exactly once`() {
        updateEmailResponses.add(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"msg":"Server unavailable"}""")
        )
        updateEmailResponses.add(successfulResponse())
        var successCount = 0
        var failureCount = 0
        var callbackLooper: Looper? = null

        IterableApi.getInstance().updateEmail(
            "new@example.com",
            IterableHelper.SuccessHandler {
                callbackLooper = Looper.myLooper()
                successCount++
            },
            IterableHelper.FailureHandler { _, _ ->
                failureCount++
            }
        )
        runMainCallbacks()

        assertEquals(2, updateEmailRequests().size)
        assertEquals(1, successCount)
        assertEquals(0, failureCount)
        assertSame(Looper.getMainLooper(), callbackLooper)
    }

    private fun runMainCallbacks() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun successfulResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setBody("{}")
    }

    private fun updateEmailRequests(): List<RecordedRequest> {
        return recordedRequests.filter {
            it.isFor(IterableConstants.ENDPOINT_UPDATE_EMAIL)
        }
    }

    private fun RecordedRequest.isFor(endpoint: String): Boolean {
        return path?.substringBefore("?") == "/$endpoint"
    }

    private fun RecordedRequest.bodyAsJson(): JSONObject {
        return JSONObject(body.clone().readUtf8())
    }
}
