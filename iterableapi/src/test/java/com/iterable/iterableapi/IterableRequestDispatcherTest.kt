package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor

@RunWith(TestRunner::class)
class IterableRequestDispatcherTest {
    private val requestExecutor = RecordingExecutor()
    private val callbackExecutor = RecordingExecutor()
    private val retryScheduler = RecordingRetryScheduler()
    private val dispatcher = IterableRequestDispatcher(
        requestExecutor,
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

        assertTrue(requestExecutor.tasks.isEmpty())
        assertEquals(6_000L, retryScheduler.delayMs)

        retryScheduler.task?.run()

        assertEquals(1, requestExecutor.tasks.size)
        assertTrue(requestExecutor.tasks.single() is IterableRequestTask)
    }

    private fun request(): IterableApiRequest {
        return IterableApiRequest(
            "api-key",
            "api/test",
            JSONObject(),
            IterableApiRequest.POST,
            null,
            null,
            null
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
}
