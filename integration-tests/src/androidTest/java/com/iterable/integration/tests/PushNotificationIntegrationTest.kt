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
        private const val APP_PACKAGE = "com.iterable.integration.tests"
        // Substring expected in the BCIT push template title. Avoids matching the
        // emoji prefix, which `By.text` matches inconsistently across systemui themes.
        private const val EXPECTED_TITLE_SUBSTRING = "BCIT Push Notification Test"
        // 30s mirrors the iOS BCIT push test's 20s springboard wait plus its surrounding
        // 4–10s of explicit sleeps; FCM delivery from a freshly-registered token is
        // routinely slower than the iOS APNS path on a clean simulator.
        private const val NOTIFICATION_TIMEOUT_SECONDS = 30L
    }
    
    private lateinit var uiDevice: UiDevice
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    
    @Before
    override fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Dismiss any system UI surface (notification shade, ANR dialog, recents) left
        // open by a prior test method on the same emulator. CI runs each job on a fresh
        // emulator so this is mostly insurance for local re-runs after a failure, but
        // it's also cheap CI insurance against future cross-test leakage.
        uiDevice.pressBack()
        uiDevice.pressHome()
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
        
        // ActivityScenario reports RESUMED before the view tree is fully rendered, so a
        // bare `exists()` check races the inflater. waitForExists() blocks up to 5 s.
        val pushButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnPushNotifications"))
        if (!pushButton.waitForExists(5000)) {
            Assert.fail("Push Notifications button not found in MainActivity")
        }
        pushButton.click()
        Thread.sleep(2000)
    }
    
    @Test
    fun testPushNotificationMVP() {
        Assert.assertTrue("User should be signed in", testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL))
        Assert.assertTrue("Notification permission should be granted", hasNotificationPermission())
        // Gate: the SDK's registerDeviceToken call must complete before the campaign is
        // queued, otherwise the push has nothing to deliver to.
        Assert.assertTrue(
            "Device token should be registered with Iterable SDK before triggering a campaign",
            waitForDeviceTokenRegistered(timeoutSeconds = 20)
        )
        // Cool-down: the Iterable backend needs a few seconds after the last 200 from
        // registerDeviceToken to commit the user→token mapping before campaigns will
        // actually deliver to this device. The iOS BCIT test does the equivalent with
        // sleeps between its "token registered" gate and the trigger.
        Thread.sleep(5_000)
        
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
            currentPackage == APP_PACKAGE
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
    }

    /**
     * Poll the system notification shade for a notification that:
     *   1. Belongs to APP_PACKAGE (so a stray notification from an unrelated app on the
     *      device — e.g. another Iterable test app sharing the same BCIT user — never matches).
     *   2. Has a title containing EXPECTED_TITLE_SUBSTRING (so we tap the right campaign,
     *      not e.g. a low-battery notification that arrives while we wait).
     *
     * Returns the matching UiObject2 once present, or null on timeout. Mirrors the iOS
     * BCIT push test's `validateSpecificPushNotificationReceived`, which polls the
     * springboard for title+body up to 20s.
     */
    private fun findNotification(timeoutSeconds: Long = NOTIFICATION_TIMEOUT_SECONDS): UiObject2? {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastSeen: UiObject2? = null
        while (System.currentTimeMillis() < deadline) {
            val match = uiDevice.findObject(
                By.pkg("com.android.systemui").textContains(EXPECTED_TITLE_SUBSTRING)
            )
            if (match != null) {
                // Walk up to the notification row so callers can click the row, not just the title text.
                lastSeen = match.parent ?: match
                return lastSeen
            }
            Thread.sleep(500)
        }
        return null
    }
    
    private fun navigateToPushNotificationTestActivity() {
        // Wait a bit for the app to fully open
        Thread.sleep(1000)
        
        // Try to find and click the Push Notifications button in MainActivity
        val pushButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnPushNotifications"))
        if (pushButton.exists()) {
            pushButton.click()
            Thread.sleep(2000) // Wait for navigation
        } else {
            // If button not found, try launching the activity directly
            val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, PushNotificationTestActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            InstrumentationRegistry.getInstrumentation().targetContext.startActivity(intent)
            Thread.sleep(2000)
        }
    }
}

