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
import org.json.JSONObject
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
        // Captured from a real BCIT delivery; only used in CI to populate `itbl.templateId`.
        private const val BCIT_TEMPLATE_ID = 20392358
        private const val APP_PACKAGE = "com.iterable.integration.tests"
        // Title substring; avoids the emoji prefix which `By.text` matches inconsistently.
        private const val EXPECTED_TITLE_SUBSTRING = "BCIT Push Notification Test"
        private const val NOTIFICATION_TIMEOUT_SECONDS = 30L
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
        
        // RESUMED fires before view inflation completes; waitForExists handles the race.
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

        if (!isRunningInCI) {
            // Local-mode only: wait for token registration + a backend cool-down before
            // triggering the real campaign. CI uses [injectPushMessage] and skips both.
            Assert.assertTrue(
                "Device token should be registered with Iterable SDK before triggering a campaign",
                waitForDeviceTokenRegistered(timeoutSeconds = 20)
            )
            Thread.sleep(5_000)
        }

        Log.d(TAG, "MVP: Push notification open action")
        triggerCampaignAndWait()
        uiDevice.pressHome()
        Thread.sleep(1000)

        uiDevice.openNotification()
        Thread.sleep(1000)
        val notification = findNotification()
        Assert.assertNotNull("Notification should be found", notification)

        notification?.click()
        Thread.sleep(2000)

        val isAppInForeground = waitForCondition({
            uiDevice.currentPackageName == APP_PACKAGE
        }, timeoutSeconds = 5)
        Assert.assertTrue("App should be in foreground after opening notification", isAppInForeground)
    }

    @Test
    fun testPushNotificationActionButtons() {
        Assert.assertTrue("User should be signed in", testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL))
        Assert.assertTrue("Notification permission should be granted", hasNotificationPermission())
        if (!isRunningInCI) {
            Assert.assertTrue(
                "Device token should be registered with Iterable SDK before triggering a campaign",
                waitForDeviceTokenRegistered(timeoutSeconds = 20)
            )
            Thread.sleep(5_000)
        }

        // URL handler via the "Google" action button
        Log.d(TAG, "Action button with URL handler")
        triggerCampaignAndWait()
        uiDevice.pressHome()
        Thread.sleep(1000)
        uiDevice.openNotification()
        Thread.sleep(2000)
        Assert.assertNotNull("Notification should be found", findNotification())

        resetUrlHandlerTracking()
        val googleButton = findActionButton("Google")
        Assert.assertNotNull("Google button should be found", googleButton)
        googleButton?.click()
        Thread.sleep(2000)
        Assert.assertTrue("URL handler should be called", waitForUrlHandler(timeoutSeconds = 5))
        Assert.assertNotNull("Handled URL should not be null", getLastHandledUrl())

        Thread.sleep(1000)
        navigateToPushNotificationTestActivity()

        // Custom action handler via the "Deeplink" action button
        Log.d(TAG, "Action button with custom action handler")
        triggerCampaignAndWait()
        uiDevice.pressHome()
        Thread.sleep(1000)
        uiDevice.openNotification()
        Thread.sleep(2000)
        Assert.assertNotNull("Notification should be found", findNotification())

        resetCustomActionHandlerTracking()
        val deeplinkButton = findActionButton("Deeplink")
        Assert.assertNotNull("Deeplink button should be found", deeplinkButton)
        deeplinkButton?.click()
        Thread.sleep(2000)
        Assert.assertTrue("Custom action handler should be called", waitForCustomActionHandler(timeoutSeconds = 5))
        Assert.assertNotNull("Action type should not be null", getLastHandledActionType())
    }
    
    private fun triggerCampaignAndWait() {
        if (isRunningInCI) {
            injectSimulatedBcitPush()
        } else {
            triggerCampaignViaBackendAndWait()
        }
    }

    // CI path: locally constructed payload mirroring the BCIT push template, injected
    // via IterableFirebaseMessagingService.handleMessageReceived. Bypasses FCM.
    private fun injectSimulatedBcitPush() {
        val itbl = JSONObject().apply {
            put("templateId", BCIT_TEMPLATE_ID)
            put("campaignId", TEST_PUSH_CAMPAIGN_ID)
            put("messageId", "ci-${System.currentTimeMillis()}")
            put("isGhostPush", false)
            put("defaultAction", JSONObject().apply {
                put("type", "openApp")
                put("data", "")
            })
            put("actionButtons", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("identifier", "google")
                    put("title", "Google")
                    put("buttonType", "default")
                    put("action", JSONObject().apply {
                        put("type", "openUrl")
                        put("data", "https://www.google.com")
                    })
                    put("openApp", true)
                })
                put(JSONObject().apply {
                    put("identifier", "deeplink")
                    put("title", "Deeplink")
                    put("buttonType", "default")
                    put("action", JSONObject().apply {
                        put("type", "cart-page")
                        put("data", "")
                    })
                    put("openApp", true)
                })
            })
        }
        val handled = injectPushMessage(
            itblPayload = itbl,
            title = "🔔 BCIT Push Notification Test",
            body = "🚀 BCIT Update: Here's what you need to know! Don't miss out."
        )
        Assert.assertTrue("Iterable SDK should accept the simulated BCIT push payload", handled)
    }

    private fun triggerCampaignViaBackendAndWait() {
        var campaignTriggered = false
        val latch = java.util.concurrent.CountDownLatch(1)
        triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TestConstants.TEST_USER_EMAIL, null) { success ->
            campaignTriggered = success
            latch.countDown()
        }
        Assert.assertTrue("Campaign trigger should complete", latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
        Assert.assertTrue("Campaign should be triggered successfully", campaignTriggered)
    }

    // Poll the systemui notification shade for the BCIT push by title; walk up to the
    // row so a click hits the whole notification, not just the title text view.
    private fun findNotification(timeoutSeconds: Long = NOTIFICATION_TIMEOUT_SECONDS): UiObject2? {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (System.currentTimeMillis() < deadline) {
            val match = uiDevice.findObject(
                By.pkg("com.android.systemui").textContains(EXPECTED_TITLE_SUBSTRING)
            )
            if (match != null) return match.parent ?: match
            Thread.sleep(500)
        }
        return null
    }
    
    // systemui pre-API-31 applied Material's textAllCaps to action-button labels
    // ("Google" → "GOOGLE"); API 31+ stopped doing so. Match either rendering.
    private fun findActionButton(label: String): UiObject2? =
        uiDevice.findObject(By.text(label))
            ?: uiDevice.findObject(By.text(label.uppercase()))

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

