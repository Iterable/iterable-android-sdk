package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(TestRunner::class)
class IterableRequestExecutionConcurrencyTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        IterableApi.sharedInstance = IterableApi()
        server = MockWebServer()
        IterableApi.overrideURLEndpointPath(server.url("/").toString())
    }

    @After
    fun tearDown() {
        IterableRequestTask.overrideUrl = null
        server.shutdown()
        IterableApi.sharedInstance = IterableApi()
    }

    @Test
    fun `online requests can execute concurrently`() {
        val slowRequestStarted = CountDownLatch(1)
        val releaseFirstResponse = CountDownLatch(1)
        val fastRequestStarted = CountDownLatch(1)

        server.dispatcher = blockingFirstRequestDispatcher(
            slowRequestStarted,
            releaseFirstResponse,
            fastRequestStarted
        )
        val slowRequestFinished = execute(
            IterableRequestDispatcher.online(),
            "api/first"
        )

        try {
            assertCompleted(slowRequestStarted, "Slow online request did not reach the server")

            val fastRequestFinished = execute(
                IterableRequestDispatcher.online(),
                "api/second"
            )

            assertCompleted(
                fastRequestStarted,
                "Second online request waited for the first response",
            )
            assertCompleted(
                fastRequestFinished,
                "Second online request did not finish while the first response was blocked",
            )
        } finally {
            releaseFirstResponse.countDown()
        }

        assertCompleted(
            slowRequestFinished,
            "First online request did not finish after its response was released"
        )
    }

    @Test
    fun `slow online request does not block push request`() {
        val onlineRequestStarted = CountDownLatch(1)
        val releaseOnlineResponse = CountDownLatch(1)
        val pushRequestStarted = CountDownLatch(1)

        server.dispatcher = blockingFirstRequestDispatcher(
            onlineRequestStarted,
            releaseOnlineResponse,
            pushRequestStarted
        )
        val onlineRequestFinished = execute(
            IterableRequestDispatcher.online(),
            "api/first"
        )

        try {
            assertCompleted(onlineRequestStarted, "Online request did not reach the server")

            val pushRequestFinished = execute(
                IterableRequestDispatcher.push(),
                "api/second"
            )

            assertCompleted(
                pushRequestStarted,
                "Push request waited for the online response",
            )
            assertCompleted(
                pushRequestFinished,
                "Push request did not finish while the online response was blocked",
            )
        } finally {
            releaseOnlineResponse.countDown()
        }

        assertCompleted(
            onlineRequestFinished,
            "Online request did not finish after its response was released"
        )
    }

    @Test
    fun `offline immediate requests execute serially`() {
        val firstRequestStarted = CountDownLatch(1)
        val releaseFirstResponse = CountDownLatch(1)
        val secondRequestStarted = CountDownLatch(1)

        server.dispatcher = blockingFirstRequestDispatcher(
            firstRequestStarted,
            releaseFirstResponse,
            secondRequestStarted
        )
        val firstRequestFinished = execute(
            IterableRequestDispatcher.offlineImmediate(),
            "api/first"
        )

        try {
            assertCompleted(
                firstRequestStarted,
                "First offline immediate request did not reach the server"
            )

            val secondRequestFinished = execute(
                IterableRequestDispatcher.offlineImmediate(),
                "api/second"
            )

            assertStillPending(
                secondRequestStarted,
                "Second offline immediate request overtook the blocked first request"
            )
            releaseFirstResponse.countDown()
            assertCompleted(
                secondRequestFinished,
                "Second offline immediate request did not finish after the first response"
            )
        } finally {
            releaseFirstResponse.countDown()
        }

        assertCompleted(
            firstRequestFinished,
            "First offline immediate request did not finish after its response was released"
        )
    }

    @Test
    fun `stored offline request does not wait for the immediate offline lane`() {
        val immediateRequestStarted = CountDownLatch(1)
        val releaseImmediateResponse = CountDownLatch(1)
        val storedRequestStarted = CountDownLatch(1)

        server.dispatcher = blockingFirstRequestDispatcher(
            immediateRequestStarted,
            releaseImmediateResponse,
            storedRequestStarted
        )
        val immediateRequestFinished = execute(
            IterableRequestDispatcher.offlineImmediate(),
            "api/first"
        )

        try {
            assertCompleted(
                immediateRequestStarted,
                "Offline immediate request did not reach the server"
            )

            val storedRequestFinished = execute(
                IterableRequestDispatcher.offlineStored(),
                "api/second"
            )

            assertCompleted(
                storedRequestStarted,
                "Stored offline request waited for the immediate offline lane"
            )
            assertCompleted(
                storedRequestFinished,
                "Stored offline request did not finish while the immediate lane was blocked"
            )
        } finally {
            releaseImmediateResponse.countDown()
        }

        assertCompleted(
            immediateRequestFinished,
            "Offline immediate request did not finish after its response was released"
        )
    }

    @Test
    fun `online server-error retries execute serially`() {
        val firstRetryStarted = CountDownLatch(1)
        val releaseFirstRetry = CountDownLatch(1)
        val secondInitialResponse = CountDownLatch(1)
        val secondRetryScheduled = CountDownLatch(1)
        val secondRetryStarted = CountDownLatch(1)
        val firstRequestFinished = CountDownLatch(1)
        val secondRequestFinished = CountDownLatch(1)
        val firstRequestCount = AtomicInteger()
        val secondRequestCount = AtomicInteger()
        val retryCount = AtomicInteger()

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path?.substringBefore("?")) {
                    "/api/first" -> {
                        if (firstRequestCount.incrementAndGet() == 1) {
                            MockResponse().setResponseCode(500)
                        } else {
                            firstRetryStarted.countDown()
                            releaseFirstRetry.await()
                            successfulResponse()
                        }
                    }

                    "/api/second" -> {
                        if (secondRequestCount.incrementAndGet() == 1) {
                            secondInitialResponse.countDown()
                            MockResponse().setResponseCode(500)
                        } else {
                            secondRetryStarted.countDown()
                            successfulResponse()
                        }
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val dispatcher = IterableRequestDispatcher(
            IterableExecutors.request(),
            IterableExecutors.requestRetry(),
            Runnable::run,
            { runnable, _ ->
                if (retryCount.incrementAndGet() == 2) {
                    secondRetryScheduled.countDown()
                }
                runnable.run()
            }
        )

        dispatcher.execute(retryingRequest("api/first", firstRequestFinished))
        assertCompleted(firstRetryStarted, "First online retry did not reach the server")

        try {
            dispatcher.execute(retryingRequest("api/second", secondRequestFinished))
            assertCompleted(
                secondInitialResponse,
                "Second initial online request did not reach the server"
            )
            assertCompleted(
                secondRetryScheduled,
                "Second online retry was not submitted"
            )
            assertStillPending(
                secondRetryStarted,
                "Second online retry overtook the blocked first retry"
            )
        } finally {
            releaseFirstRetry.countDown()
        }

        assertCompleted(
            secondRetryStarted,
            "Second online retry did not run after the first retry completed"
        )
        assertCompleted(
            firstRequestFinished,
            "First online request did not finish after its retry response"
        )
        assertCompleted(
            secondRequestFinished,
            "Second online request did not finish after its retry response"
        )
    }

    private fun blockingFirstRequestDispatcher(
        firstRequestStarted: CountDownLatch,
        releaseFirstResponse: CountDownLatch,
        secondRequestStarted: CountDownLatch
    ): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path?.substringBefore("?")) {
                    "/api/first" -> {
                        firstRequestStarted.countDown()
                        releaseFirstResponse.await()
                        successfulResponse()
                    }

                    "/api/second" -> {
                        secondRequestStarted.countDown()
                        successfulResponse()
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
    }

    private fun execute(
        dispatcher: IterableRequestDispatcher,
        resourcePath: String
    ): CountDownLatch {
        return CountDownLatch(1).also { requestFinished ->
            dispatcher.executeForResponse(request(resourcePath)) {
                requestFinished.countDown()
            }
        }
    }

    private fun assertCompleted(latch: CountDownLatch, message: String) {
        assertTrue(message, latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private fun assertStillPending(latch: CountDownLatch, message: String) {
        assertFalse(message, latch.await(SERIAL_ASSERTION_MILLIS, TimeUnit.MILLISECONDS))
    }

    private fun request(resourcePath: String): IterableApiRequest {
        return IterableApiRequest(
            "api-key",
            resourcePath,
            JSONObject(),
            IterableApiRequest.GET,
            null,
            null,
            null
        )
    }

    private fun retryingRequest(
        resourcePath: String,
        requestFinished: CountDownLatch
    ): IterableApiRequest {
        return IterableApiRequest(
            "api-key",
            resourcePath,
            JSONObject(),
            IterableApiRequest.GET,
            null,
            IterableHelper.SuccessHandler { requestFinished.countDown() },
            null
        )
    }

    private fun successfulResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setBody("{}")
    }

    companion object {
        private const val TIMEOUT_SECONDS = 5L
        private const val SERIAL_ASSERTION_MILLIS = 500L
    }
}
