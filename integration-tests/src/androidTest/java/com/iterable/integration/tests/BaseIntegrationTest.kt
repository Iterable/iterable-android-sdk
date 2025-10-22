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
        // Cleanup test environment
        cleanupTestEnvironment()
        
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
        
        // Setup test data
        setupTestData()
    }
    
    private fun cleanupTestEnvironment() {
        // Clear any test data
        clearTestData()
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
    
    private fun setupTestData() {
        // Setup any test-specific data
    }
    
    private fun clearTestData() {
        // Clear any test-specific data
    }
    
    /**
     * Wait for a condition to be true with timeout
     */
    protected fun waitForCondition(condition: () -> Boolean, timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
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
     * Wait for an in-app message to be displayed
     */
    protected fun waitForInAppMessage(timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        return waitForCondition({
            testUtils.hasInAppMessageDisplayed()
        }, timeoutSeconds)
    }
    
    /**
     * Trigger a campaign via Iterable API
     */
    protected fun triggerCampaignViaAPI(campaignId: Int, recipientEmail: String = TestConstants.TEST_USER_EMAIL, dataFields: Map<String, Any>? = null, callback: ((Boolean) -> Unit)? = null) {
        testUtils.triggerCampaignViaAPI(campaignId, recipientEmail, dataFields, callback)
    }
    
    /**
     * Trigger a push campaign via Iterable API
     */
    protected fun triggerPushCampaignViaAPI(campaignId: Int, recipientEmail: String = TestConstants.TEST_USER_EMAIL, dataFields: Map<String, Any>? = null, callback: ((Boolean) -> Unit)? = null) {
        testUtils.triggerPushCampaignViaAPI(campaignId, recipientEmail, dataFields, callback)
    }
    
    /**
     * Wait for a push notification to be received
     */
    protected fun waitForPushNotification(timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        return waitForCondition({
            testUtils.hasReceivedPushNotification()
        }, timeoutSeconds)
    }
    
    /**
     * Send a test push notification
     */
    protected fun sendTestPushNotification(campaignId: String = "test_campaign"): Boolean {
        return testUtils.sendPushNotification(campaignId)
    }
    
    /**
     * Trigger an in-app message
     */
    protected fun triggerInAppMessage(eventName: String = "test_event"): Boolean {
        return testUtils.triggerInAppMessage(eventName)
    }
    
    /**
     * Wait for a campaign to be triggered and processed
     */
    protected fun waitForCampaignTrigger(campaignId: Int, timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        // Trigger the campaign with callback
        var triggered = false
        val latch = CountDownLatch(1)
        
        triggerCampaignViaAPI(campaignId, TestConstants.TEST_USER_EMAIL, null) { success ->
            triggered = success
            latch.countDown()
        }
        
        // Wait for callback
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            return false
        }
        
        if (!triggered) {
            return false
        }
        
        // Wait for the campaign to be processed (in-app message or push notification)
        return waitForCondition({
            testUtils.hasInAppMessageDisplayed() || testUtils.hasReceivedPushNotification()
        }, timeoutSeconds)
    }
    
    /**
     * Reset URL handler tracking
     */
    protected fun resetUrlHandlerTracking() {
        urlHandlerCalled.set(false)
        lastHandledUrl.set(null)
    }
    
    /**
     * Check if URL handler was called
     */
    protected fun wasUrlHandlerCalled(): Boolean {
        return urlHandlerCalled.get()
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