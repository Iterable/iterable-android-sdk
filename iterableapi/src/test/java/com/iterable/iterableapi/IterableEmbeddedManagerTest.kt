package com.iterable.iterableapi

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.android.util.concurrent.PausedExecutorService

public class IterableEmbeddedManagerTest {

    private var server: MockWebServer? = null
    private lateinit var dispatcher: PathBasedQueueDispatcher
    private var backgroundExecutor: PausedExecutorService? = null

    @Before
    fun setUp() {
        backgroundExecutor = PausedExecutorService()
        server = MockWebServer()
        dispatcher = PathBasedQueueDispatcher()
        server!!.dispatcher = dispatcher

        IterableApi.overrideURLEndpointPath(server!!.url("").toString())
        IterableApi.sharedInstance = IterableApi()

    }

    @After
    fun tearDown() {
        server!!.shutdown()
        server = null
    }

    @Test
    fun testSyncEmbedded() {

    }
}