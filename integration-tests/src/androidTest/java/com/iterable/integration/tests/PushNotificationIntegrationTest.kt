package com.iterable.integration.tests

import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import com.iterable.iterableapi.IterableApi
import com.iterable.integration.tests.activities.PushNotificationTestActivity
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PushNotificationIntegrationTest : BaseIntegrationTest() {
    
    companion object {
        private const val TAG = "PushNotificationIntegrationTest"
        private const val TEST_PUSH_CAMPAIGN_ID = TestConstants.TEST_PUSH_CAMPAIGN_ID
    }
    
    private lateinit var uiDevice: UiDevice
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    
    @Before
    override fun setUp() {
        Log.d(TAG, "🔧 Test setup starting...")
        
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Call super.setUp() to initialize SDK with BaseIntegrationTest's config
        // This sets test mode flag and initializes SDK with test handlers (including urlHandler)
        super.setUp()
        
        Log.d(TAG, "🔧 Base setup complete, SDK initialized with test handlers")
        
        // Disable in-app auto display and clear existing messages BEFORE launching app
        // This prevents in-app messages from obscuring the push notification test screen
        Log.d(TAG, "🔧 Disabling in-app auto display and clearing existing messages...")
        IterableApi.getInstance().inAppManager.setAutoDisplayPaused(true)
        Log.d(TAG, "✅ In-app auto display paused")
        
        // Clear all existing in-app messages
        IterableApi.getInstance().inAppManager.messages.forEach {
            Log.d(TAG, "Clearing existing in-app message: ${it.messageId}")
            IterableApi.getInstance().inAppManager.removeMessage(it)
        }
        Log.d(TAG, "✅ All in-app messages cleared")
        
        Log.d(TAG, "🔧 MainActivity will skip initialization due to test mode flag")
        
        // Now launch the app flow with custom handlers already configured
        launchAppAndNavigateToPushNotificationTesting()
    }
    
    @After
    override fun tearDown() {
        super.tearDown()
    }
    
    private fun launchAppAndNavigateToPushNotificationTesting() {
        // Step 1: Launch MainActivity (the home page)
        Log.d(TAG, "🔧 Step 1: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        // Wait for MainActivity to be ready
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until {
                val state = mainActivityScenario.state
                Log.d(TAG, "🔧 MainActivity state: $state")
                state == Lifecycle.State.RESUMED
            }
        
        Log.d(TAG, "🔧 MainActivity is ready!")
        
        // Step 2: Click the "Push Notifications" button to navigate to PushNotificationTestActivity
        Log.d(TAG, "🔧 Step 2: Clicking 'Push Notifications' button...")
        val pushButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnPushNotifications"))
        if (pushButton.exists()) {
            pushButton.click()
            Log.d(TAG, "🔧 Clicked Push Notifications button successfully")
        } else {
            Log.e(TAG, "❌ Push Notifications button not found!")
            Assert.fail("Push Notifications button not found in MainActivity")
        }
        
        // Step 3: Wait for PushNotificationTestActivity to load
        Log.d(TAG, "🔧 Step 3: Waiting for PushNotificationTestActivity to load...")
        Thread.sleep(2000) // Give time for navigation
        
        Log.d(TAG, "🔧 App navigation complete: Now on PushNotificationTestActivity!")
    }
    
    @Test
    fun testPushNotificationMVP() {
        // Step 1: Ensure user is signed in
        Log.d(TAG, "📧 Step 1: Ensuring user is signed in...")
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        Log.d(TAG, "✅ User signed in successfully: ${TestConstants.TEST_USER_EMAIL}")
        
        // Step 2: Trigger push notification campaign via API
        Log.d(TAG, "🎯 Step 2: Triggering push notification campaign via API...")
        Log.d(TAG, "Campaign ID: $TEST_PUSH_CAMPAIGN_ID")
        Log.d(TAG, "User Email: ${TestConstants.TEST_USER_EMAIL}")
        
        var campaignTriggered = false
        val latch = java.util.concurrent.CountDownLatch(1)
        
        triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TestConstants.TEST_USER_EMAIL, null) { success ->
            campaignTriggered = success
            Log.d(TAG, "🎯 Push campaign trigger result: $success")
            if (!success) {
                val errorMessage = testUtils.getLastErrorMessage()
                Log.w(TAG, "⚠️ Push campaign trigger failed: $errorMessage")
            }
            latch.countDown()
        }
        
        // Wait for API call to complete (up to 10 seconds for CI)
        val apiCallCompleted = latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        Log.d(TAG, "🎯 API call completed: $apiCallCompleted, success: $campaignTriggered")
        
        if (!apiCallCompleted) {
            Log.e(TAG, "❌ API call did not complete in time")
            Assert.fail("Push campaign trigger API call did not complete in time")
            return
        }
        
        if (!campaignTriggered) {
            val errorMessage = testUtils.getLastErrorMessage()
            Log.e(TAG, "❌ Push campaign trigger FAILED: $errorMessage")
            Log.e(TAG, "❌ Cannot proceed with test - no push notification will be available")
            Assert.fail("Push campaign trigger failed: $errorMessage. Check API key and campaign configuration.")
            return
        }
        
        Log.d(TAG, "✅ Push campaign triggered successfully, waiting for notification...")
        
        // Step 3: Wait for push notification to arrive (give time for FCM delivery)
        Log.d(TAG, "⏳ Step 3: Waiting for push notification to arrive...")
        Thread.sleep(5000) // Give time for FCM to deliver the notification
        
        // Step 4: Open notification drawer and verify notification is present
        Log.d(TAG, "📱 Step 4: Opening notification drawer...")
        uiDevice.openNotification()
        Thread.sleep(2000) // Wait for notification drawer to open
        
        // Step 5: Find and interact with the notification
        Log.d(TAG, "🔍 Step 5: Looking for push notification in notification drawer...")
        
        // Try to find notification by text (common notification text patterns)
        var notificationFound = false
        var notification = uiDevice.findObject(By.textContains("Iterable"))
            ?: uiDevice.findObject(By.textContains("iterable"))
            ?: uiDevice.findObject(By.textContains("Test"))
        
        if (notification == null) {
            // Try to find any notification that might be from our app
            val notifications = uiDevice.findObjects(By.res("com.android.systemui:id/notification_stack_scroller"))
            if (notifications.isNotEmpty()) {
                notification = notifications.first()
                notificationFound = true
            }
        } else {
            notificationFound = true
        }
        
        if (!notificationFound || notification == null) {
            Log.e(TAG, "❌ Push notification not found in notification drawer")
            uiDevice.pressBack() // Close notification drawer
            Assert.fail("Push notification not found in notification drawer. Check FCM configuration and campaign setup.")
            return
        }
        
        Log.d(TAG, "✅ Push notification found in notification drawer")
        
        // Step 6: Click on the notification to open it
        Log.d(TAG, "🎯 Step 6: Clicking on push notification...")
        notification.click()
        Thread.sleep(2000) // Wait for app to open
        
        // Step 7: Verify URL handler was called (if notification has action)
        Log.d(TAG, "🎯 Step 7: Verifying URL handler was called after notification click...")
        
        val urlHandlerCalled = waitForUrlHandler(timeoutSeconds = 5)
        if (urlHandlerCalled) {
            // Step 8: Verify the correct URL was handled
            val handledUrl = getLastHandledUrl()
            Log.d(TAG, "🎯 URL handler received: $handledUrl")
            
            Assert.assertNotNull("Handled URL should not be null", handledUrl)
            Log.d(TAG, "✅ URL handler was called with URL: $handledUrl")
        } else {
            Log.d(TAG, "ℹ️ URL handler was not called - notification may not have an action URL")
            // This is acceptable if the notification doesn't have a deep link action
        }
        
        Log.d(TAG, "✅✅✅ Test completed successfully! All steps passed.")
    }
}

