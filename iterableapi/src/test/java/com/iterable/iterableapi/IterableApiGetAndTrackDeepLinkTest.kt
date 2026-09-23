package com.iterable.iterableapi

import android.net.Uri
import android.os.AsyncTask
import android.os.Looper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class IterableApiGetAndTrackDeepLinkTest : BaseTest() {
    private lateinit var server: MockWebServer
    private var releaseHostTask: CountDownLatch? = null
    private var hostTaskFinished: CountDownLatch? = null
    private var releaseDeepLinkResponse: CountDownLatch? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        IterableApi.sharedInstance._applicationContext = getContext()
        IterableApi.sharedInstance.config = IterableConfig.Builder()
            .setAllowedProtocols(arrayOf("http"))
            .build()
    }

    @After
    fun tearDown() {
        releaseHostTask?.countDown()
        releaseDeepLinkResponse?.countDown()
        hostTaskFinished?.let {
            assertTrue(
                "Host AsyncTask queue did not resume",
                it.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
        }
        server.shutdown()
    }

    @Test
    fun `Iterable link returns destination and stores attribution on main thread`() {
        val iterableLink = server.url("/a/abc123").toString()
        val destinationUrl = "https://example.com/destination"
        enqueueRedirectWithAttribution(destinationUrl)

        var callbackUrl: String? = null
        var callbackLooper: Looper? = null
        var attributionDuringCallback: IterableAttributionInfo? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
            callbackLooper = Looper.myLooper()
            attributionDuringCallback = IterableApi.getInstance().attributionInfo
        }
        awaitDeepLinkResolution()

        assertEquals(destinationUrl, callbackUrl)
        assertSame(Looper.getMainLooper(), callbackLooper)
        assertNull(attributionDuringCallback)

        val attributionInfo = IterableApi.getInstance().attributionInfo
        assertNotNull(attributionInfo)
        with(attributionInfo!!) {
            assertEquals(123, campaignId)
            assertEquals(456, templateId)
            assertEquals("message-id", messageId)
        }
    }

    @Test
    fun `Iterable link does not wait for host AsyncTask queue`() {
        val iterableLink = server.url("/a/abc123").toString()
        val destinationUrl = "https://example.com/destination"
        enqueueRedirect(destinationUrl)
        blockHostAsyncTaskQueue()
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
        }
        awaitDeepLinkResolution()

        assertEquals(destinationUrl, callbackUrl)
    }

    @Test
    fun `slow Iterable link does not block push work`() {
        val iterableLink = server.url("/a/abc123").toString()
        val destinationUrl = "https://example.com/destination"
        val redirectStarted = CountDownLatch(1)
        val releaseRedirect = CountDownLatch(1)
        releaseDeepLinkResponse = releaseRedirect
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                redirectStarted.countDown()
                try {
                    releaseRedirect.await()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                return redirectResponse(destinationUrl)
            }
        }
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
        }
        assertTrue(
            "Deep-link request did not start",
            redirectStarted.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        val pushWorkCompleted = CountDownLatch(1)
        IterableExecutors.push().execute(pushWorkCompleted::countDown)

        assertTrue(
            "Push work was blocked by the deep-link request",
            pushWorkCompleted.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        releaseRedirect.countDown()
        awaitDeepLinkResolution()
        assertEquals(destinationUrl, callbackUrl)
    }

    @Test
    fun `redirect without attribution returns destination without attribution`() {
        val iterableLink = server.url("/a/abc123").toString()
        val destinationUrl = "https://example.com/destination"
        enqueueRedirect(destinationUrl)
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
        }
        awaitDeepLinkResolution()

        assertEquals(destinationUrl, callbackUrl)
        assertNull(IterableApi.getInstance().attributionInfo)
    }

    @Test
    fun `successful response without redirect returns original URL`() {
        val iterableLink = server.url("/a/abc123").toString()
        server.enqueue(MockResponse().setResponseCode(200))
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
        }
        awaitDeepLinkResolution()

        assertEquals(iterableLink, callbackUrl)
        assertNull(IterableApi.getInstance().attributionInfo)
    }

    @Test
    fun `server error returns original URL on main thread without attribution`() {
        val iterableLink = server.url("/a/abc123").toString()
        server.enqueue(MockResponse().setResponseCode(500))
        var callbackUrl: String? = null
        var callbackLooper: Looper? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
            callbackLooper = Looper.myLooper()
        }
        awaitDeepLinkResolution()

        assertEquals(iterableLink, callbackUrl)
        assertSame(Looper.getMainLooper(), callbackLooper)
        assertNull(IterableApi.getInstance().attributionInfo)
    }

    @Test
    fun `read timeout returns original URL on main thread without attribution`() {
        val iterableLink = server.url("/a/abc123").toString()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        var callbackUrl: String? = null
        var callbackLooper: Looper? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
            callbackLooper = Looper.myLooper()
        }
        awaitDeepLinkResolution(READ_TIMEOUT_WAIT_SECONDS)

        assertEquals(iterableLink, callbackUrl)
        assertSame(Looper.getMainLooper(), callbackLooper)
        assertNull(IterableApi.getInstance().attributionInfo)
    }

    @Test
    fun `malformed Iterable link returns original URL without attribution`() {
        val iterableLink = "http://[invalid/a/abc123"
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
        }
        awaitDeepLinkResolution()

        assertEquals(iterableLink, callbackUrl)
        assertNull(IterableApi.getInstance().attributionInfo)
    }

    @Test
    fun `invalid attribution cookie still returns destination URL`() {
        val iterableLink = server.url("/a/abc123").toString()
        val destinationUrl = "https://example.com/destination"
        enqueueRedirectWithInvalidAttribution(destinationUrl)
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(iterableLink) { url ->
            callbackUrl = url
        }
        awaitDeepLinkResolution()

        assertEquals(destinationUrl, callbackUrl)
        assertNull(IterableApi.getInstance().attributionInfo)
    }

    @Test
    fun `non-Iterable link returns original URL`() {
        val originalUrl = server.url("/not-an-iterable-link").toString()
        var callbackUrl: String? = null

        IterableApi.getInstance().getAndTrackDeepLink(originalUrl) { url ->
            callbackUrl = url
        }
        awaitDeepLinkResolution()

        assertEquals(originalUrl, callbackUrl)
    }

    @Test
    fun `disallowed protocol does not invoke callback`() {
        var callbackInvoked = false

        IterableApi.getInstance().getAndTrackDeepLink(
            "ftp://links.example.com/a/abc123"
        ) {
            callbackInvoked = true
        }
        awaitDeepLinkResolution()

        assertFalse(callbackInvoked)
    }

    @Test
    fun `Iterable links are resolved in submission order`() {
        val firstIterableLink = server.url("/a/first").toString()
        val secondIterableLink = server.url("/a/second").toString()
        val firstDestination = "https://example.com/first"
        val secondDestination = "https://example.com/second"
        server.enqueue(redirectResponse(firstDestination))
        server.enqueue(redirectResponse(secondDestination))
        val callbackUrls = mutableListOf<String?>()

        IterableApi.getInstance().getAndTrackDeepLink(firstIterableLink) { url ->
            callbackUrls.add(url)
        }
        IterableApi.getInstance().getAndTrackDeepLink(secondIterableLink) { url ->
            callbackUrls.add(url)
        }
        awaitDeepLinkResolution()

        assertEquals(
            listOf(firstDestination, secondDestination),
            callbackUrls
        )
    }

    @Test
    fun `handle app link opens resolved destination through client URL handler`() {
        val iterableLink = server.url("/a/abc123").toString()
        val destinationUrl = "https://example.com/destination"
        enqueueRedirect(destinationUrl)
        var openedUri: Uri? = null
        var receivedContext: IterableActionContext? = null
        IterableApi.initialize(
            getContext(),
            "fake-api-key",
            IterableConfig.Builder()
                .setAllowedProtocols(arrayOf("http"))
                .setAutoPushRegistration(false)
                .setUrlHandler { uri, context ->
                    openedUri = uri
                    receivedContext = context
                    true
                }
                .build()
        )

        val handled = IterableApi.getInstance().handleAppLink(iterableLink)
        awaitDeepLinkResolution()

        assertTrue(handled)
        assertEquals(destinationUrl, openedUri.toString())
        assertEquals(IterableActionSource.APP_LINK, receivedContext?.source)
        assertTrue(
            receivedContext!!.action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL)
        )
        assertEquals(destinationUrl, receivedContext!!.action.data)
    }

    private fun enqueueRedirect(destinationUrl: String) {
        server.enqueue(redirectResponse(destinationUrl))
    }

    private fun redirectResponse(destinationUrl: String): MockResponse {
        return MockResponse()
            .setResponseCode(302)
            .addHeader(IterableConstants.LOCATION_HEADER_FIELD, destinationUrl)
    }

    private fun enqueueRedirectWithAttribution(destinationUrl: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader(IterableConstants.LOCATION_HEADER_FIELD, destinationUrl)
                .addHeader("Set-Cookie", "iterableEmailCampaignId=123")
                .addHeader("Set-Cookie", "iterableTemplateId=456")
                .addHeader("Set-Cookie", "iterableMessageId=message-id")
        )
    }

    private fun enqueueRedirectWithInvalidAttribution(destinationUrl: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader(IterableConstants.LOCATION_HEADER_FIELD, destinationUrl)
                .addHeader("Set-Cookie", "iterableEmailCampaignId=invalid")
        )
    }

    private fun awaitDeepLinkResolution(
        waitTimeoutSeconds: Long = WAIT_TIMEOUT_SECONDS
    ) {
        val sdkQueueDrained = CountDownLatch(1)
        IterableExecutors.deepLink().execute(sdkQueueDrained::countDown)

        assertTrue(
            "SDK serial executor did not finish",
            sdkQueueDrained.await(waitTimeoutSeconds, TimeUnit.SECONDS)
        )
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Suppress("DEPRECATION")
    private fun blockHostAsyncTaskQueue() {
        val hostTaskStarted = CountDownLatch(1)
        val releaseTask = CountDownLatch(1)
        val taskFinished = CountDownLatch(1)
        releaseHostTask = releaseTask
        hostTaskFinished = taskFinished

        AsyncTask.SERIAL_EXECUTOR.execute {
            hostTaskStarted.countDown()
            try {
                releaseTask.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                taskFinished.countDown()
            }
        }

        assertTrue(
            "Host AsyncTask queue did not block",
            hostTaskStarted.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )
    }

    private companion object {
        const val WAIT_TIMEOUT_SECONDS = 5L
        const val READ_TIMEOUT_WAIT_SECONDS = 10L
    }
}
