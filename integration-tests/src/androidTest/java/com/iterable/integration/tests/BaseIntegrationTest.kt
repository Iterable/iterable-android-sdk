package com.iterable.integration.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.integration.tests.utils.IntegrationTestUtils
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
abstract class BaseIntegrationTest {
    
    companion object {
        const val TIMEOUT_SECONDS = 30L
        const val POLL_INTERVAL_SECONDS = 1L
    }
    
    protected lateinit var context: Context
    protected lateinit var testUtils: IntegrationTestUtils
    
    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testUtils = IntegrationTestUtils(context)
        
        // Initialize Iterable SDK for testing
        initializeIterableSDK()
        
        // Setup test environment
        setupTestEnvironment()
    }
    
    @After
    open fun tearDown() {
        // Cleanup test environment
        cleanupTestEnvironment()
    }
    
    private fun initializeIterableSDK() {
        val config = IterableConfig.Builder()
            .setAutoPushRegistration(true)
            .setEnableEmbeddedMessaging(true)
            .setInAppHandler { message ->
                // Handle in-app messages during tests
                com.iterable.iterableapi.IterableInAppHandler.InAppResponse.SHOW
            }
            .setCustomActionHandler { action, context ->
                // Handle custom actions during tests
                true
            }
            .setUrlHandler { url, context ->
                // Handle URLs during tests
                true
            }
            .build()
        
        IterableApi.initialize(context, BuildConfig.ITERABLE_API_KEY, config)
        
        // Set the user email for integration testing
        val userEmail = "akshay.ayyanchira@iterable.com"
        IterableApi.getInstance().setEmail(userEmail)
        
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
     * Wait for a push notification to be received
     */
    protected fun waitForPushNotification(timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        return waitForCondition({
            testUtils.hasReceivedPushNotification()
        }, timeoutSeconds)
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
     * Wait for an embedded message to be displayed
     */
    protected fun waitForEmbeddedMessage(timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        return waitForCondition({
            testUtils.hasEmbeddedMessageDisplayed()
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
     * Trigger an embedded message
     */
    protected fun triggerEmbeddedMessage(placementId: Int = 0): Boolean {
        return testUtils.triggerEmbeddedMessage(placementId)
    }
    
    /**
     * Simulate a deep link
     */
    protected fun simulateDeepLink(url: String): Boolean {
        return testUtils.simulateDeepLink(url)
    }
    
    /**
     * Trigger a campaign via Iterable API
     */
    protected fun triggerCampaignViaAPI(campaignId: Int, recipientEmail: String = "akshay.ayyanchira@iterable.com", dataFields: Map<String, Any>? = null, callback: ((Boolean) -> Unit)? = null) {
        testUtils.triggerCampaignViaAPI(campaignId, recipientEmail, dataFields, callback)
    }
    
    /**
     * Trigger a push campaign via Iterable API
     */
    protected fun triggerPushCampaignViaAPI(campaignId: Int, recipientEmail: String = "akshay.ayyanchira@iterable.com", dataFields: Map<String, Any>? = null, callback: ((Boolean) -> Unit)? = null) {
        testUtils.triggerPushCampaignViaAPI(campaignId, recipientEmail, dataFields, callback)
    }
    
    /**
     * Wait for a campaign to be triggered and processed
     */
    protected fun waitForCampaignTrigger(campaignId: Int, timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        // Trigger the campaign
        val triggered = triggerCampaignViaAPI(campaignId)
        if (!triggered) {
            return false
        }
        
        // Wait for the campaign to be processed (in-app message or push notification)
        return waitForCondition({
            testUtils.hasInAppMessageDisplayed() || testUtils.hasReceivedPushNotification()
        }, timeoutSeconds)
    }
} 