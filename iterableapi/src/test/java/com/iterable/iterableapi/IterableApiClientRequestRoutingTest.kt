package com.iterable.iterableapi

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.Executor

class IterableApiClientRequestRoutingTest : BaseTest() {
    private lateinit var onlineExecutor: RecordingExecutor
    private lateinit var pushExecutor: RecordingExecutor
    private lateinit var offlineImmediateExecutor: RecordingExecutor
    private lateinit var offlineStoredExecutor: RecordingExecutor
    private lateinit var authProvider: IterableApiClient.AuthProvider
    private lateinit var taskStorage: IterableTaskStorage
    private lateinit var client: IterableApiClient

    @Before
    fun setUp() {
        onlineExecutor = RecordingExecutor()
        pushExecutor = RecordingExecutor()
        offlineImmediateExecutor = RecordingExecutor()
        offlineStoredExecutor = RecordingExecutor()
        authProvider = mock(IterableApiClient.AuthProvider::class.java)
        `when`(authProvider.context).thenReturn(getContext())
        ReflectionHelpers.setStaticField(
            IterableTaskStorage::class.java,
            "sharedInstance",
            null
        )
        taskStorage = IterableTaskStorage.sharedInstance(getContext())
        taskStorage.deleteAllTasks()
        client = IterableApiClient(
            authProvider,
            IterableRequestDispatchers(
                dispatcher(onlineExecutor),
                dispatcher(pushExecutor),
                dispatcher(offlineImmediateExecutor),
                dispatcher(offlineStoredExecutor)
            )
        )
    }

    @After
    fun tearDown() {
        client.dispose()
        taskStorage.deleteAllTasks()
        ReflectionHelpers.setStaticField(
            IterableTaskStorage::class.java,
            "sharedInstance",
            null
        )
    }

    @Test
    fun `ordinary API request uses online lane`() {
        client.sendPostRequest("api/ordinary", JSONObject())

        assertEquals(1, onlineExecutor.tasks.size)
        assertTrue(pushExecutor.tasks.isEmpty())
        assertTrue(offlineImmediateExecutor.tasks.isEmpty())
        assertTrue(offlineStoredExecutor.tasks.isEmpty())
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
        assertTrue(offlineImmediateExecutor.tasks.isEmpty())
        assertTrue(offlineStoredExecutor.tasks.isEmpty())
    }

    @Test
    fun `offline immediate request uses its own lane`() {
        client.setOfflineProcessingEnabled(true)

        client.sendPostRequest(IterableConstants.ENDPOINT_UPDATE_EMAIL, JSONObject())

        assertEquals(1, offlineImmediateExecutor.tasks.size)
        assertTrue(onlineExecutor.tasks.isEmpty())
        assertTrue(pushExecutor.tasks.isEmpty())
        assertTrue(offlineStoredExecutor.tasks.isEmpty())
    }

    @Test
    fun `logout clears persisted requests before offline processor is created`() {
        val taskId = taskStorage.createTask(
            "api/stored-before-restart",
            IterableTaskType.API,
            "{}"
        )
        assertNotNull(taskId)

        client.onLogout()

        assertNull(taskStorage.getNextScheduledTask())
        verify(authProvider).resetAuth()
    }

    @Test
    fun `logout clears persisted requests after switching back online`() {
        client.setOfflineProcessingEnabled(true)
        val taskId = taskStorage.createTask(
            "api/stored-before-disable",
            IterableTaskType.API,
            "{}"
        )
        assertNotNull(taskId)
        client.setOfflineProcessingEnabled(false)

        client.onLogout()

        assertNull(taskStorage.getNextScheduledTask())
        verify(authProvider).resetAuth()
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
