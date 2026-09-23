package com.iterable.iterableapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.Executor

class IterableApiClientRequestRoutingTest {
    private lateinit var onlineExecutor: RecordingExecutor
    private lateinit var pushExecutor: RecordingExecutor
    private lateinit var offlineExecutor: RecordingExecutor
    private lateinit var client: IterableApiClient

    @Before
    fun setUp() {
        onlineExecutor = RecordingExecutor()
        pushExecutor = RecordingExecutor()
        offlineExecutor = RecordingExecutor()
        client = IterableApiClient(
            mock(IterableApiClient.AuthProvider::class.java),
            IterableRequestDispatchers(
                dispatcher(onlineExecutor),
                dispatcher(pushExecutor),
                dispatcher(offlineExecutor)
            )
        )
    }

    @Test
    fun `ordinary API request uses online lane`() {
        client.sendPostRequest("api/ordinary", JSONObject())

        assertEquals(1, onlineExecutor.tasks.size)
        assertTrue(pushExecutor.tasks.isEmpty())
        assertTrue(offlineExecutor.tasks.isEmpty())
    }

    @Test
    fun `push token request uses push lane`() {
        client.disableToken(
            "user@example.com",
            null,
            null,
            "device-token",
            null,
            null
        )

        assertEquals(1, pushExecutor.tasks.size)
        assertTrue(onlineExecutor.tasks.isEmpty())
        assertTrue(offlineExecutor.tasks.isEmpty())
    }

    private fun dispatcher(executor: Executor): IterableRequestDispatcher {
        return IterableRequestDispatcher(
            executor,
            Runnable::run,
            { runnable, _ -> runnable.run() }
        )
    }

    private class RecordingExecutor : Executor {
        val tasks = mutableListOf<Runnable>()

        override fun execute(command: Runnable) {
            tasks.add(command)
        }
    }
}
