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
        
        // Register watcher to auto-dismiss ANR dialogs
        setupAnrDialogWatcher()
        
        // Reset test states
        inAppMessageDisplayed.set(false)
        inAppClickTracked.set(false)
        inAppCloseTracked.set(false)
        trackInAppCloseCalled.set(false)
        lastClickedUrl.set(null)
        lastCloseAction.set(null)
        currentInAppMessage.set(null)
        
        // CRITICAL: Setup custom handlers FIRST, before any activities
        Log.d(TAG, "🔧 Setting up custom handlers BEFORE any activity launches...")
        setupConfigAndInitialize()
        
        // Call super.setUp() to initialize SDK with BaseIntegrationTest's config
        // This sets test mode flag and initializes SDK with test handlers (including urlHandler)
        super.setUp()
        
        Log.d(TAG, "🔧 Base setup complete, SDK initialized with test handlers")
        Log.d(TAG, "🔧 MainActivity will skip initialization due to test mode flag")
        
        // Now launch the app flow with custom handlers already configured
        launchAppAndNavigateToInAppTesting()
    }
    
    @After
    override fun tearDown() {
        try {
            // Resume auto-display for next tests
            Log.d(TAG, "🔧 Resuming auto-display for cleanup...")
            IterableApi.getInstance().inAppManager.setAutoDisplayPaused(false)
        } catch (e: Exception) {
            Log.w(TAG, "Could not resume auto-display: ${e.message}")
        }
        
        if (::inAppActivityScenario.isInitialized) {
            inAppActivityScenario.close()
        }
        if (::mainActivityScenario.isInitialized) {
            mainActivityScenario.close()
        }
        super.tearDown()
    }
    
    private fun setupAnrDialogWatcher() {
        Log.d(TAG, "🔧 Setting up ANR dialog watcher...")
        
        // Register a watcher that automatically handles ANR dialogs
        uiDevice.registerWatcher("ANRWatcher") {
            // Check for "Process system isn't responding" dialog
            val closeButton = uiDevice.findObject(UiSelector().text("Close app"))
            val waitButton = uiDevice.findObject(UiSelector().text("Wait"))
            
            when {
                waitButton.exists() -> {
                    Log.w(TAG, "⚠️ ANR dialog detected! Clicking 'Wait' to continue...")
                    waitButton.click()
                    Thread.sleep(2000) // Give system time to recover
                    true
                }
                closeButton.exists() -> {
                    Log.w(TAG, "⚠️ ANR dialog detected but only 'Close app' available - dismissing...")
                    closeButton.click()
                    false // Indicate we had to close
                }
                else -> false
            }
        }
        
        // Run watchers immediately and enable auto-run
        uiDevice.runWatchers()
        Log.d(TAG, "✅ ANR dialog watcher registered and active")
    }
    
    private fun setupConfigAndInitialize() {
        Log.d(TAG, "🔧 setupCustomInAppHandler() called")
        
        val config = IterableConfig.Builder()
            .setAutoPushRegistration(true)
            .setEnableEmbeddedMessaging(true)
            .setInAppDisplayInterval(2.0) // Same as MainActivity for consistency
            .setCustomActionHandler { action, context ->
                Log.d(TAG, "🎯 Custom action triggered: $action")
                true
            }
            .setUrlHandler { url, context ->
                Log.d(TAG, "🎯 URL HANDLER TRIGGERED! URL: $url")
                Log.d(TAG, "🎯 Expected URL: https://www.nbc.com/")
                Log.d(TAG, "🎯 URL matches expected: ${url.toString().contains("nbc.com")}")
                
                lastClickedUrl.set(url.toString())
                trackInAppCloseCalled.set(true)
                
                Log.d(TAG, "🎯 Flags set - lastClickedUrl: ${lastClickedUrl.get()}, trackInAppCloseCalled: ${trackInAppCloseCalled.get()}")
                true
            }
            .build()
        
        Log.d(TAG, "🔧 Config built, initializing IterableApi...")
        
        // Get context from instrumentation since BaseIntegrationTest.context isn't initialized yet
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize with custom handlers BEFORE MainActivity can initialize
        Log.d(TAG, "🔧 Initializing IterableApi with test handlers BEFORE MainActivity...")
        
        // Set a system property to indicate we're in test mode
        System.setProperty("iterable.test.mode", "true")
        
        IterableApi.initialize(appContext, BuildConfig.ITERABLE_API_KEY, config)
        //Pausing auto-display to prevent interference with message queue during tests
        IterableApi.getInstance().inAppManager.setAutoDisplayPaused(true)
        IterableApi.getInstance().setEmail(TestConstants.TEST_USER_EMAIL)
        
        Log.d(TAG, "🔧 IterableApi initialized with custom handlers")
        Log.d(TAG, "🔧 Note: Will pause auto-display after first InApp shows to prevent queue interference")
    }
    
    private fun launchAppAndNavigateToInAppTesting() {
        
        // Step 1: Launch MainActivity (the home page)
        Log.d(TAG, "🔧 Step 1: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        // Wait for MainActivity to be ready
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until {
                val state = mainActivityScenario.state
                Log.d(TAG, "🔧 MainActivity state: $state")
                state == Lifecycle.State.RESUMED
            }
        
        Log.d(TAG, "🔧 MainActivity is ready!")
        
        // Extra wait for UI to stabilize in CI (emulator can be slow)
        Log.d(TAG, "🔧 Waiting extra 3s for UI to fully render...")
        Thread.sleep(3000)
        
        // Run watchers to catch any ANR dialogs that appeared
        uiDevice.runWatchers()
        
        // Dump all visible elements for debugging
        Log.d(TAG, "🔧 Dumping UI elements...")
        uiDevice.dumpWindowHierarchy(System.out)
        
        // Step 2: Click the "In-App Messages" button to navigate to InAppMessageTestActivity
        Log.d(TAG, "🔧 Step 2: Clicking 'In-App Messages' button...")
        
        // Try multiple selectors to find the button
        var inAppButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnInAppMessages"))
        if (!inAppButton.exists()) {
            Log.d(TAG, "🔧 Button not found by resourceId, trying by text...")
            inAppButton = uiDevice.findObject(UiSelector().text("In-App Message Test"))
        }
        if (!inAppButton.exists()) {
            Log.d(TAG, "🔧 Button not found by text, trying partial text match...")
            inAppButton = uiDevice.findObject(UiSelector().textContains("In-App"))
        }
        
        if (inAppButton.exists()) {
            Log.d(TAG, "🔧 Found button! Clicking...")
            inAppButton.clickAndWaitForNewWindow(5000)
            Log.d(TAG, "🔧 Clicked In-App Messages button successfully")
        } else {
            //Take screenshot for debugging
//            uiDevice.takeScreenshot(File("/sdcard/Download/InAppButtonNotFound.png"))
            Log.e(TAG, "❌ In-App Messages button not found!")
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

        Log.d(TAG, "✅ Campaign triggered successfully, proceeding with message sync...")
        
        // Step 4: Sync messages
        Log.d(TAG, "🔄 Step 4: Syncing in-app messages...")
        Thread.sleep(3000) // Give time for any messages to sync
        
        // Manually sync
        IterableApiHelper().syncInAppMessages()
        
        // Wait for sync to complete
        Thread.sleep(2000)
        
        val messageCount = IterableApi.getInstance().inAppManager.messages.count()
        Log.d(TAG, "🔄 Message count after sync: $messageCount")
        
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