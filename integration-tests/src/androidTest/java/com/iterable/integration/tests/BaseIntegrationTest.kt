package com.iterable.integration.tests

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.integration.tests.utils.IntegrationTestUtils
import com.iterable.integration.tests.TestConstants
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
abstract class BaseIntegrationTest {

    companion object {
        const val TIMEOUT_SECONDS = TestConstants.TIMEOUT_SECONDS
        const val POLL_INTERVAL_SECONDS = TestConstants.POLL_INTERVAL_SECONDS
    }

    protected lateinit var context: Context
    protected lateinit var testUtils: IntegrationTestUtils

    // URL handler tracking for tests
    private val urlHandlerCalled = AtomicBoolean(false)
    private val lastHandledUrl = AtomicReference<String?>(null)

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testUtils = IntegrationTestUtils(context)

        // Reset tracking flags
        resetUrlHandlerTracking()

        // Set test mode flag to prevent MainActivity from initializing SDK
        // This ensures our test config (with test handlers) is the one used
        System.setProperty("iterable.test.mode", "true")

        // Initialize Iterable SDK for testing
        initializeIterableSDK()

        // Setup test environment
        setupTestEnvironment()
    }

    @After
    open fun tearDown() {
        // Clear test mode flag
        System.clearProperty("iterable.test.mode")
    }

    private fun initializeIterableSDK() {
        val config = IterableConfig.Builder()
            .setAutoPushRegistration(true)
            .setEnableEmbeddedMessaging(true)
            .setLogLevel(Log.VERBOSE)
            .setInAppDisplayInterval(3.0)
            .setInAppHandler { message ->
                // Handle in-app messages during tests
                Log.d("BaseIntegrationTest", "In-app message received: ${message.messageId}")
                testUtils.setInAppMessageDisplayed(true)
                com.iterable.iterableapi.IterableInAppHandler.InAppResponse.SHOW
            }
            .setCustomActionHandler { action, context ->
                // Handle custom actions during tests
                Log.d("BaseIntegrationTest", "Custom action triggered: $action")
                true
            }
            .setUrlHandler { url, context ->
                // Handle URLs during tests
                Log.d("BaseIntegrationTest", "URL handler triggered: $url")
                urlHandlerCalled.set(true)
                lastHandledUrl.set(url.toString())
                true
            }
            .build()

        IterableApi.initialize(context, BuildConfig.ITERABLE_API_KEY, config)

        // Set the user email for integration testing
        val userEmail = TestConstants.TEST_USER_EMAIL
        IterableApi.getInstance().setEmail(userEmail)
        Log.d("BaseIntegrationTest", "User email set to: $userEmail")
        Log.d("BaseIntegrationTest", "Iterable SDK initialized with email: $userEmail")
    }

    private fun setupTestEnvironment() {
        // Grant notification permissions
        grantNotificationPermissions()
    }


    private fun grantNotificationPermissions() {
        // Grant notification permissions for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            )
        }
    }

    /**
     * Wait for a condition to be true with timeout
     */
    protected fun waitForCondition(
        condition: () -> Boolean,
        timeoutSeconds: Long = TIMEOUT_SECONDS
    ): Boolean {
        return try {
            Awaitility.await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .until { condition() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }


    /**
     * Trigger a campaign via Iterable API
     */
    protected fun triggerCampaignViaAPI(
        campaignId: Int,
        recipientEmail: String = TestConstants.TEST_USER_EMAIL,
        dataFields: Map<String, Any>? = null,
        callback: ((Boolean) -> Unit)? = null
    ) {
        testUtils.triggerCampaignViaAPI(campaignId, recipientEmail, dataFields, callback)
    }

    /**
     * Trigger a push campaign via Iterable API
     */
    protected fun triggerPushCampaignViaAPI(
        campaignId: Int,
        recipientEmail: String = TestConstants.TEST_USER_EMAIL,
        dataFields: Map<String, Any>? = null,
        callback: ((Boolean) -> Unit)? = null
    ) {
        testUtils.triggerPushCampaignViaAPI(campaignId, recipientEmail, dataFields, callback)
    }


    /**
     * Reset URL handler tracking
     */
    protected fun resetUrlHandlerTracking() {
        urlHandlerCalled.set(false)
        lastHandledUrl.set(null)
    }

    /**
     * Get the last URL handled by the URL handler
     */
    protected fun getLastHandledUrl(): String? {
        return lastHandledUrl.get()
    }

    /**
     * Wait for URL handler to be called
     */
    protected fun waitForUrlHandler(timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        return waitForCondition({
            urlHandlerCalled.get()
        }, timeoutSeconds)
    }
} 