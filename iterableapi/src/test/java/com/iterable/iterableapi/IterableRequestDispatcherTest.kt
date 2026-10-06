package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

@RunWith(TestRunner::class)
class IterableRequestDispatcherTest {
    private val requestExecutor = RecordingExecutor()
    private val retryExecutor = RecordingExecutor()
    private val callbackExecutor = RecordingExecutor()
    private val retryScheduler = RecordingRetryScheduler()
    private val dispatcher = IterableRequestDispatcher(
        requestExecutor,
        retryExecutor,
        callbackExecutor,
        retryScheduler
    )

    @Test
    fun `executing a request submits runnable work to the request executor`() {
        dispatcher.execute(request())

        assertEquals(1, requestExecutor.tasks.size)
        assertTrue(requestExecutor.tasks.single() is IterableRequestTask)
    }

    @Test
    fun `delivering a result submits the callback to the callback executor`() {
        val callback = Runnable {}

        dispatcher.deliverResult(callback)

        assertSame(callback, callbackExecutor.tasks.single())
    }

    @Test
    fun `retry waits for the scheduler before submitting request work`() {
        dispatcher.retry(request(), 4, 6_000)

        assertTrue(retryExecutor.tasks.isEmpty())
        assertEquals(6_000L, retryScheduler.delayMs)

        retryScheduler.task?.run()

        assertTrue(requestExecutor.tasks.isEmpty())
        assertEquals(1, retryExecutor.tasks.size)
        assertTrue(retryExecutor.tasks.single() is IterableRequestTask)
    }

    @Test
    fun `stale retry is not submitted`() {
        val staleRequest = request().apply {
            setRetryState { false }
        }

        dispatcher.executeRetry(staleRequest, 0)

        assertTrue(retryExecutor.tasks.isEmpty())
    }

    @Test
    fun `a rejecting executor fails asynchronously without running network work on caller`() {
        val failure = RecordingFailureHandler()
        val rejectingDispatcher = IterableRequestDispatcher(
            Executor { throw RejectedExecutionException("full") },
            callbackExecutor,
            retryScheduler
        )

        rejectingDispatcher.execute(request(failure))

        assertEquals(1, callbackExecutor.tasks.size)
        assertEquals(null, failure.message)

        callbackExecutor.tasks.single().run()

        assertEquals("Iterable request executor rejected work", failure.message)
    }

    @Test
    fun `a rejected response request receives a transient failure`() {
        var response: IterableApiResponse? = null
        val rejectingDispatcher = IterableRequestDispatcher(
            Executor { throw RejectedExecutionException("full") },
            callbackExecutor,
            retryScheduler
        )

        rejectingDispatcher.executeForResponse(request()) {
            response = it
        }
        callbackExecutor.tasks.single().run()

        assertEquals(0, response?.responseCode)
        assertEquals(false, response?.success)
    }

    private fun request(
        failure: IterableHelper.FailureHandler? = null
    ): IterableApiRequest {
        return IterableApiRequest(
            "api-key",
            "api/test",
            JSONObject(),
            IterableApiRequest.POST,
            null,
            null,
            failure
        )
    }

    private class RecordingExecutor : Executor {
        val tasks = mutableListOf<Runnable>()

        override fun execute(command: Runnable) {
            tasks.add(command)
        }
    }

    private class RecordingRetryScheduler : IterableRequestDispatcher.RetryScheduler {
        var task: Runnable? = null
        var delayMs: Long? = null

        override fun schedule(runnable: Runnable, delayMs: Long) {
            task = runnable
            this.delayMs = delayMs
        }
    }

    private class RecordingFailureHandler : IterableHelper.FailureHandler {
        var message: String? = null

        override fun onFailure(reason: String, data: JSONObject?) {
            message = reason
        }
    }
}
