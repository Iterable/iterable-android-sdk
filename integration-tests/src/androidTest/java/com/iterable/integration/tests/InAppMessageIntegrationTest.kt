package com.iterable.integration.tests

import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.iterableapi.IterableInAppCloseAction
import com.iterable.iterableapi.IterableConfig
import com.iterable.integration.tests.activities.InAppMessageTestActivity
import com.iterable.iterableapi.IterableApiHelper
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class InAppMessageIntegrationTest : BaseIntegrationTest() {
    
    companion object {
        private const val TAG = "InAppMessageIntegrationTest"
        private const val TEST_CAMPAIGN_ID = TestConstants.TEST_INAPP_CAMPAIGN_ID
        private const val TEST_EVENT_NAME = "test_inapp_event"
    }
    
    private lateinit var uiDevice: UiDevice
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    private lateinit var inAppActivityScenario: ActivityScenario<InAppMessageTestActivity>
    
    @Before
    override fun setUp() {
        Log.d(TAG, "🔧 Test setup starting...")
        
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Call super.setUp() to initialize SDK with BaseIntegrationTest's config
        // This sets test mode flag and initializes SDK with test handlers (including urlHandler)
        super.setUp()
        
        Log.d(TAG, "🔧 Base setup complete, SDK initialized with test handlers")
        
        // Add small delay to ensure SDK is fully ready after background initialization
        Thread.sleep(500)
        
        Log.d(TAG, "🔧 MainActivity will skip initialization due to test mode flag")
        
        // Now launch the app flow with custom handlers already configured
        launchAppAndNavigateToInAppTesting()
    }
    
    @After
    override fun tearDown() {

        super.tearDown()
    }
    
    private fun launchAppAndNavigateToInAppTesting() {
        
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
        
        // Step 2: Click the "In-App Messages" button to navigate to InAppMessageTestActivity
        Log.d(TAG, "🔧 Step 2: Waiting for and clicking 'In-App Messages' button...")
        
        // Use UiDevice.wait() with generous timeout for slow emulators
        val inAppButton = uiDevice.wait(
            Until.findObject(By.res("com.iterable.integration.tests", "btnInAppMessages")),
            10000 // 10 second timeout for slow CI
        )
        
        if (inAppButton != null) {
            inAppButton.click()
            Log.d(TAG, "🔧 Clicked In-App Messages button successfully")
        } else {
            Log.e(TAG, "❌ In-App Messages button not found after waiting 10 seconds!")
            Log.e(TAG, "Current activity: " + uiDevice.currentPackageName)
            Assert.fail("In-App Messages button not found in MainActivity")
        }
        
        // Step 3: Wait for InAppMessageTestActivity to load
        Log.d(TAG, "🔧 Step 3: Waiting for InAppMessageTestActivity to load...")
        Thread.sleep(2000) // Give time for navigation
        
        Log.d(TAG, "🔧 App navigation complete: Now on InAppMessageTestActivity (same as manual flow)!")
    }
    
    @Test
    fun testInAppMessageMVP() {
        Log.d(TAG, "🚀 Starting MVP in-app message test - GitHub Actions optimized")

        // Step 1: Ensure user is signed in
        Log.d(TAG, "📧 Step 1: Ensuring user is signed in...")
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        Log.d(TAG, "✅ User signed in successfully: ${TestConstants.TEST_USER_EMAIL}")

        // Step 2: Debug API key configuration
        Log.d(TAG, "🔍 Debug: ITERABLE_API_KEY = ${BuildConfig.ITERABLE_API_KEY}")
        Log.d(TAG, "🔍 Debug: ITERABLE_SERVER_API_KEY = ${BuildConfig.ITERABLE_SERVER_API_KEY}")
        Log.d(TAG, "🔍 Debug: ITERABLE_TEST_USER_EMAIL = ${BuildConfig.ITERABLE_TEST_USER_EMAIL}")

        // Step 3: Try to trigger campaign via API (but don't fail if it doesn't work)
        Log.d(TAG, "🎯 Step 3: Attempting to trigger campaign via API...")
        Log.d(TAG, "Campaign ID: $TEST_CAMPAIGN_ID")
        Log.d(TAG, "User Email: ${TestConstants.TEST_USER_EMAIL}")

        //TODO: Check if any inapp is being displayed right now and close it if so

        //TODO: Make sure InApp messages are cleared before triggering new one
        IterableApi.getInstance().inAppManager.messages.forEach {
            Log.d(TAG, "Clearing existing message: ${it.messageId}")
            IterableApi.getInstance().inAppManager.removeMessage(it)
        }

        // Get initial message count before triggering campaign
        val initialMessageCount = IterableApi.getInstance().inAppManager.messages.count()
        Log.d(TAG, "📊 Initial message count: $initialMessageCount")
        
        // Reset silent push tracking to detect InAppUpdate push
        testUtils.setSilentPushProcessed(false)

        var campaignTriggered = false
        val latch = java.util.concurrent.CountDownLatch(1)

        triggerCampaignViaAPI(TEST_CAMPAIGN_ID, TestConstants.TEST_USER_EMAIL, null) { success ->
            campaignTriggered = success
            Log.d(TAG, "🎯 Campaign trigger result: $success")
            if (!success) {
                val errorMessage = testUtils.getLastErrorMessage()
                Assert.fail("Server call failed. Retry after some time")
                Log.w(TAG, "⚠️ Campaign trigger failed: $errorMessage")
                Log.w(TAG, "⚠️ This is expected in CI if API keys are not configured")
            }
            latch.countDown()
        }

        // Wait for API call to complete (up to 10 seconds for CI)
        val apiCallCompleted = latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        Log.d(TAG, "🎯 API call completed: $apiCallCompleted, success: $campaignTriggered")

        if (!apiCallCompleted) {
            Log.e(TAG, "❌ API call did not complete in time")
            Assert.fail("Campaign trigger API call did not complete in time")
            return
        }

        if (!campaignTriggered) {
            val errorMessage = testUtils.getLastErrorMessage()
            Log.e(TAG, "❌ Campaign trigger FAILED: $errorMessage")
            Log.e(TAG, "❌ Cannot proceed with test - no in-app message will be available")
            Assert.fail("Campaign trigger failed: $errorMessage. Check API key and campaign configuration.")
            return
        }

        Log.d(TAG, "✅ Campaign triggered successfully, waiting for push-triggered sync...")
        
        // Step 4: Wait for push-triggered sync (primary path)
        Log.d(TAG, "🔄 Step 4: Waiting for InAppUpdate push to trigger automatic sync...")
        val syncViaPush = waitForInAppSyncViaPush(initialMessageCount, pushTimeoutSeconds = 10)
        
        var messageCount = IterableApi.getInstance().inAppManager.messages.count()
        
        // Step 4b: Fallback to manual sync if push didn't work
        if (!syncViaPush || messageCount == 0) {
            Log.d(TAG, "⚠️ Push-triggered sync did not complete, falling back to manual sync...")
            Log.d(TAG, "🔄 Step 4b: Manually syncing in-app messages...")
            Thread.sleep(2000) // Give a bit more time in case push is still arriving
            
            // Check again before manual sync
            messageCount = IterableApi.getInstance().inAppManager.messages.count()
            if (messageCount == 0) {
                IterableApiHelper().syncInAppMessages()
                Thread.sleep(2000) // Wait for sync to complete
                messageCount = IterableApi.getInstance().inAppManager.messages.count()
                Log.d(TAG, "🔄 Message count after manual sync: $messageCount")
            } else {
                Log.d(TAG, "✅ Messages synced via push (delayed), message count: $messageCount")
            }
        } else {
            Log.d(TAG, "✅ Messages synced via push-triggered automatic sync, message count: $messageCount")
        }
        
        Assert.assertTrue(
            "Message count should be 1, but was $messageCount",
            messageCount == 1
        )

        IterableApi.getInstance().inAppManager.showMessage(
            IterableApi.getInstance().inAppManager.messages.first()
        )

        //wait for 3 seconds to let the inapp show
        Thread.sleep(3000)

        // Get the top activity using ActivityLifecycleMonitorRegistry
        var topActivity: android.app.Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            topActivity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
        }

        // Check if topActivity contains IterableInAppFragmentHTMLNotification
        var isIterableInAppFragmentView = false
        topActivity?.let { activity ->
            val fragmentManager = try {
                (activity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            } catch (e: Exception) {
                null
            }
            fragmentManager?.fragments?.forEach { fragment ->
                if (fragment != null &&
                    (fragment.javaClass.simpleName == "IterableInAppFragmentHTMLNotification" ||
                        fragment.javaClass.canonicalName?.endsWith("IterableInAppFragmentHTMLNotification") == true)
                ) {
                    isIterableInAppFragmentView = true
                }
            }
        }

        Assert.assertTrue(
            "Top activity should be IterableInAppFragmentHTMLNotification or contain IterableWebView",
            isIterableInAppFragmentView
        )
        
        Log.d(TAG, "✅ In-app message is displayed, now interacting with button...")
        
        // Step 5: Click the "No Thanks" button in the WebView
        Log.d(TAG, "🎯 Step 5: Clicking 'No Thanks' button in the in-app message...")

        // Try to find and click the "No Thanks" button with retry logic
        var noThanksButton: androidx.test.uiautomator.UiObject2? = null
        var attempts = 0
        val maxAttempts = 5
        
        while (noThanksButton == null && attempts < maxAttempts) {
            attempts++
            Log.d(TAG, "Attempt $attempts: Looking for 'No Thanks' button...")
            
            // Try different text variations
            noThanksButton = uiDevice.findObject(By.textContains("No Thanks"))
                ?: uiDevice.findObject(By.text("No Thanks"))
                ?: uiDevice.findObject(By.textContains("no thanks"))
                ?: uiDevice.findObject(By.textContains("NO THANKS"))
            
            if (noThanksButton == null) {
                Log.d(TAG, "Button not found, waiting 1 second before retry...")
                Thread.sleep(1000)
            }
        }
        
        if (noThanksButton != null) {
            noThanksButton.click()
            Log.d(TAG, "✅ Clicked 'No Thanks' button")
        } else {
            Assert.fail("'No Thanks' button not found in the in-app message WebView after $maxAttempts attempts")
        }
        
        // Step 6: Verify URL handler was called
        Log.d(TAG, "🎯 Step 6: Verifying URL handler was called after button click...")
        
        val urlHandlerCalled = waitForUrlHandler(timeoutSeconds = 5)
        Assert.assertTrue(
            "URL handler should have been called after clicking the button",
            urlHandlerCalled
        )
        
        // Step 7: Verify the correct URL was handled
        val handledUrl = getLastHandledUrl()
        Log.d(TAG, "🎯 URL handler received: $handledUrl")
        
        Assert.assertNotNull("Handled URL should not be null", handledUrl)
        Log.d(TAG, "✅ URL handler was called with URL: $handledUrl")
        
        Log.d(TAG, "✅✅✅ Test completed successfully! All steps passed.")
    }
}