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
    
    // Custom action handler tracking for tests
    private val customActionHandlerCalled = AtomicBoolean(false)
    private val lastHandledAction = AtomicReference<com.iterable.iterableapi.IterableAction?>(null)
    private val lastHandledActionType = AtomicReference<String?>(null)

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testUtils = IntegrationTestUtils(context)

        // Reset tracking flags
        resetUrlHandlerTracking()
        resetCustomActionHandlerTracking()

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
            .setCustomActionHandler(object : com.iterable.iterableapi.IterableCustomActionHandler {
                override fun handleIterableCustomAction(
                    action: com.iterable.iterableapi.IterableAction,
                    actionContext: com.iterable.iterableapi.IterableActionContext
                ): Boolean {
                    // Handle custom actions during tests
                    val actionType = action.getType()
                    Log.d("BaseIntegrationTest", "Custom action triggered: type=$actionType, action=$action, source=${actionContext.source}")
                    customActionHandlerCalled.set(true)
                    lastHandledAction.set(action)
                    lastHandledActionType.set(actionType)
                    return false
                }
            })
            .setUrlHandler { url, context ->
                // Handle URLs during tests
                Log.d("BaseIntegrationTest", "URL handler triggered: $url")
                urlHandlerCalled.set(true)
                lastHandledUrl.set(url.toString())
                false
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
     * Check if notification permission is granted
     */
    protected fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
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

    /**
     * Reset custom action handler tracking
     */
    protected fun resetCustomActionHandlerTracking() {
        customActionHandlerCalled.set(false)
        lastHandledAction.set(null)
        lastHandledActionType.set(null)
    }

    /**
     * Get the last action handled by the custom action handler
     */
    protected fun getLastHandledAction(): com.iterable.iterableapi.IterableAction? {
        return lastHandledAction.get()
    }

    /**
     * Get the last action type handled by the custom action handler
     */
    protected fun getLastHandledActionType(): String? {
        return lastHandledActionType.get()
    }

    /**
     * Wait for custom action handler to be called
     */
    protected fun waitForCustomActionHandler(timeoutSeconds: Long = TIMEOUT_SECONDS): Boolean {
        return waitForCondition({
            customActionHandlerCalled.get()
        }, timeoutSeconds)
    }

    /**
     * Wait for InAppUpdate push notification to be processed and in-app messages to sync.
     * Returns true if messages were synced (either via push or manually), false if timeout.
     */
    protected fun waitForInAppSyncViaPush(
        initialMessageCount: Int,
        pushTimeoutSeconds: Long = 10
    ): Boolean {
        Log.d("BaseIntegrationTest", "Waiting for InAppUpdate push to trigger sync (timeout: ${pushTimeoutSeconds}s)...")
        
        // Wait for either:
        // 1. Silent push was processed (indicates InAppUpdate push arrived)
        // 2. Message count increased (indicates sync happened)
        val pushProcessed = waitForCondition({
            testUtils.isSilentPushProcessed() || 
            IterableApi.getInstance().inAppManager.messages.count() > initialMessageCount
        }, pushTimeoutSeconds)
        
        if (pushProcessed) {
            Log.d("BaseIntegrationTest", "InAppUpdate push processed or sync detected")
            // Give a bit more time for sync to complete if push was just processed
            Thread.sleep(2000)
        } else {
            Log.d("BaseIntegrationTest", "InAppUpdate push not received within timeout")
        }
        
        // Check if messages were synced
        val currentMessageCount = IterableApi.getInstance().inAppManager.messages.count()
        val syncHappened = currentMessageCount > initialMessageCount
        
        if (syncHappened) {
            Log.d("BaseIntegrationTest", "✅ In-app messages synced via push (message count: $currentMessageCount)")
        } else {
            Log.d("BaseIntegrationTest", "⚠️ In-app messages not synced yet (message count: $currentMessageCount)")
        }
        
        return syncHappened
    }

    /**
     * Wait for UpdateEmbedded push notification to be processed and embedded messages to sync.
     * Returns true if messages were synced (either via push or manually), false if timeout.
     * 
     * @param initialPlacementIds Initial set of placement IDs before sync
     * @param expectedPlacementId Optional placement ID to check for (if provided, waits for it to appear)
     * @param pushTimeoutSeconds Timeout in seconds for waiting for push
     */
    protected fun waitForEmbeddedSyncViaPush(
        initialPlacementIds: Set<Long>,
        expectedPlacementId: Long? = null,
        pushTimeoutSeconds: Long = 10
    ): Boolean {
        Log.d("BaseIntegrationTest", "Waiting for UpdateEmbedded push to trigger sync (timeout: ${pushTimeoutSeconds}s)...")
        
        // Wait for either:
        // 1. Embedded push was processed (indicates UpdateEmbedded push arrived)
        // 2. Placement IDs changed (indicates sync happened)
        // 3. Expected placement ID appeared (if provided)
        val pushProcessed = waitForCondition({
            val currentPlacementIds = IterableApi.getInstance().embeddedManager.getPlacementIds().toSet()
            val placementIdsChanged = currentPlacementIds != initialPlacementIds
            val expectedPlacementFound = expectedPlacementId?.let { currentPlacementIds.contains(it) } ?: false
            
            testUtils.isEmbeddedPushProcessed() || placementIdsChanged || expectedPlacementFound
        }, pushTimeoutSeconds)
        
        if (pushProcessed) {
            Log.d("BaseIntegrationTest", "UpdateEmbedded push processed or sync detected")
            // Give a bit more time for sync to complete if push was just processed
            Thread.sleep(2000)
        } else {
            Log.d("BaseIntegrationTest", "UpdateEmbedded push not received within timeout")
        }
        
        // Check if messages were synced
        val currentPlacementIds = IterableApi.getInstance().embeddedManager.getPlacementIds().toSet()
        val syncHappened = currentPlacementIds != initialPlacementIds || 
                          (expectedPlacementId != null && currentPlacementIds.contains(expectedPlacementId))
        
        if (syncHappened) {
            Log.d("BaseIntegrationTest", "✅ Embedded messages synced via push (placement IDs: $currentPlacementIds)")
        } else {
            Log.d("BaseIntegrationTest", "⚠️ Embedded messages not synced yet (placement IDs: $currentPlacementIds)")
        }
        
        return syncHappened
    }
} 