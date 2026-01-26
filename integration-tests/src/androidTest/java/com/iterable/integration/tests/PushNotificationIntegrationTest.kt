package com.iterable.integration.tests

import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
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
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        super.setUp()
        
        IterableApi.getInstance().inAppManager.setAutoDisplayPaused(true)
        IterableApi.getInstance().inAppManager.messages.forEach {
            IterableApi.getInstance().inAppManager.removeMessage(it)
        }
        
        launchAppAndNavigateToPushNotificationTesting()
    }
    
    @After
    override fun tearDown() {
        super.tearDown()
    }
    
    private fun launchAppAndNavigateToPushNotificationTesting() {
        Log.d(TAG, "Step 1: Launching MainActivity and navigating to PushNotificationTestActivity")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until {
                mainActivityScenario.state == Lifecycle.State.RESUMED
            }
        
        Log.d(TAG, "Waiting for Push Notifications button to appear...")
        val pushButton = uiDevice.wait(
            Until.findObject(By.res("com.iterable.integration.tests", "btnPushNotifications")),
            10000 // 10 second timeout for slow CI
        )
        
        if (pushButton == null) {
            Log.e(TAG, "Push Notifications button not found after waiting 10 seconds!")
            Log.e(TAG, "Current activity: " + uiDevice.currentPackageName)
            Assert.fail("Push Notifications button not found in MainActivity")
        }
        
        pushButton.click()
        Log.d(TAG, "Clicked Push Notifications button successfully")
        Thread.sleep(2000)
    }
    
    @Test
    fun testPushNotificationMVP() {
        Assert.assertTrue("User should be signed in", testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL))
        Assert.assertTrue("Notification permission should be granted", hasNotificationPermission())
        
        // Test 1: Trigger campaign, minimize app, open notification, verify app opens
        Log.d(TAG, "Test 1: Push notification open action")
        triggerCampaignAndWait()
        uiDevice.pressHome()
        Thread.sleep(1000)
        
        uiDevice.openNotification()
        Thread.sleep(1000)
        val notification1 = findNotification()
        Assert.assertNotNull("Notification should be found", notification1)
        
        notification1?.click()
        Thread.sleep(2000) // Wait for app to open
        
        // Verify app is in foreground by checking current package name
        val isAppInForeground = waitForCondition({
            val currentPackage = uiDevice.currentPackageName
            currentPackage == "com.iterable.integration.tests"
        }, timeoutSeconds = 5)
        Assert.assertTrue("App should be in foreground after opening notification", isAppInForeground)
        navigateToPushNotificationTestActivity()
        
        // Test 2: Trigger campaign again, tap first action button (Google), verify URL handler
        Log.d(TAG, "Test 2: Action button with URL handler")
        triggerCampaignAndWait()
        uiDevice.pressHome()
        Thread.sleep(1000)
        
        uiDevice.openNotification()
        Thread.sleep(2000)
        val notification2 = findNotification()
        Assert.assertNotNull("Notification should be found", notification2)
        
        resetUrlHandlerTracking()
        val googleButton = uiDevice.findObject(By.text("Google"))
        Assert.assertNotNull("Google button should be found", googleButton)
        googleButton?.click()
        Thread.sleep(2000)
        
        Assert.assertTrue("URL handler should be called", waitForUrlHandler(timeoutSeconds = 5))
        Assert.assertNotNull("Handled URL should not be null", getLastHandledUrl())
        
        // Navigate back to PushNotificationTestActivity for next test (in case action button opened app)
        Thread.sleep(1000)
        navigateToPushNotificationTestActivity()
        
        // Test 3: Trigger campaign again, tap second action button (Deeplink), verify custom action handler
        Log.d(TAG, "Test 3: Action button with custom action handler")
        triggerCampaignAndWait()
        uiDevice.pressHome()
        Thread.sleep(1000)
        
        uiDevice.openNotification()
        Thread.sleep(2000)
        val notification3 = findNotification()
        Assert.assertNotNull("Notification should be found", notification3)
        
        resetCustomActionHandlerTracking()
        val deeplinkButton = uiDevice.findObject(By.text("Deeplink"))
        Assert.assertNotNull("Deeplink button should be found", deeplinkButton)
        deeplinkButton?.click()
        Thread.sleep(2000)
        
        Assert.assertTrue("Custom action handler should be called", waitForCustomActionHandler(timeoutSeconds = 5))
        Assert.assertNotNull("Action type should not be null", getLastHandledActionType())
        
        // Navigate back to PushNotificationTestActivity (in case action button opened app)
        Thread.sleep(1000)
        navigateToPushNotificationTestActivity()
        
        // Note: trackPushOpen() is called internally by the SDK when notifications are opened
        // It's automatically invoked by IterablePushNotificationUtil.executeAction() which is called
        // by the trampoline activity when handling push notification clicks
        Log.d(TAG, "Test completed successfully")
    }
    
    private fun triggerCampaignAndWait() {
        var campaignTriggered = false
        val latch = java.util.concurrent.CountDownLatch(1)
        triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TestConstants.TEST_USER_EMAIL, null) { success ->
            campaignTriggered = success
            latch.countDown()
        }
        Assert.assertTrue("Campaign trigger should complete", latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
        Assert.assertTrue("Campaign should be triggered successfully", campaignTriggered)
        Thread.sleep(5000) // Wait for FCM delivery
    }
    
    private fun findNotification(): UiObject2? {
        val searchTexts = listOf("BCIT", "iterable", "Test", TestConstants.TEST_USER_EMAIL)
        for (searchText in searchTexts) {
            val notification = uiDevice.findObject(By.textContains(searchText))
            if (notification != null) return notification
        }
        
        val allNotifications = uiDevice.findObjects(By.res("com.android.systemui:id/notification_text"))
        for (notif in allNotifications) {
            val text = notif.text ?: ""
            if (text.contains("Iterable", ignoreCase = true) || text.contains("iterable", ignoreCase = true)) {
                return notif.parent
            }
        }
        return null
    }
    
    private fun navigateToPushNotificationTestActivity() {
        // Wait a bit for the app to fully open
        Thread.sleep(1000)
        
        // Try to find and click the Push Notifications button in MainActivity
        Log.d(TAG, "Navigating back to Push Notification Test Activity...")
        val pushButton = uiDevice.wait(
            Until.findObject(By.res("com.iterable.integration.tests", "btnPushNotifications")),
            5000 // 5 second timeout
        )
        
        if (pushButton != null) {
            pushButton.click()
            Log.d(TAG, "Clicked Push Notifications button")
            Thread.sleep(2000) // Wait for navigation
        } else {
            // If button not found, try launching the activity directly
            Log.d(TAG, "Button not found, launching activity directly")
            val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, PushNotificationTestActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            InstrumentationRegistry.getInstrumentation().targetContext.startActivity(intent)
            Thread.sleep(2000)
        }
    }
}

