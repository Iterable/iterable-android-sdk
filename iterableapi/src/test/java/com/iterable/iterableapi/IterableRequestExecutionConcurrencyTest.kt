package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    private fun successfulResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setBody("{}")
    }

    companion object {
        private const val TIMEOUT_SECONDS = 5L
    }
}
