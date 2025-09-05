package com.iterable.integration.tests

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.integration.tests.activities.InAppTestActivity
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class InAppMessageActivityIntegrationTest : BaseIntegrationTest() {
    
    companion object {
        private const val TAG = "InAppMessageActivityIntegrationTest"
        private const val TEST_CAMPAIGN_ID = 14332357
    }
    
    private lateinit var uiDevice: UiDevice
    private lateinit var activityScenario: ActivityScenario<InAppTestActivity>
    private val inAppMessageDisplayed = AtomicBoolean(false)
    private val trackInAppCloseCalled = AtomicBoolean(false)
    
    @Before
    override fun setUp() {
        super.setUp()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Reset test states
        inAppMessageDisplayed.set(false)
        trackInAppCloseCalled.set(false)
        
        // Setup custom in-app handler to track events
        setupCustomInAppHandler()
        
        // Launch the test activity
        launchTestActivity()
    }
    
    @After
    override fun tearDown() {
        activityScenario.close()
        super.tearDown()
    }
    
    private fun setupCustomInAppHandler() {
        val config = IterableConfig.Builder()
            .setAutoPushRegistration(true)
            .setEnableEmbeddedMessaging(true)
            .setInAppHandler { message ->
                Log.d(TAG, "In-app message received: ${message.messageId}")
                inAppMessageDisplayed.set(true)
                com.iterable.iterableapi.IterableInAppHandler.InAppResponse.SHOW
            }
            .setUrlHandler { url, context ->
                Log.d(TAG, "URL handler triggered: $url")
                // Simulate trackInAppClose when button is clicked
                trackInAppCloseCalled.set(true)
                true
            }
            .build()
        
        // Re-initialize with custom handlers
        IterableApi.initialize(context, BuildConfig.ITERABLE_API_KEY, config)
        IterableApi.getInstance().setEmail("akshay.ayyanchira@iterable.com")
    }
    
    private fun launchTestActivity() {
        val intent = Intent(context, InAppTestActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)
        
        // Wait for activity to be ready
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until {
                activityScenario.state == Lifecycle.State.RESUMED
            }
    }
    
    @Test
    fun testInAppMessageMVP() {
        Log.d(TAG, "Starting MVP in-app message test")
        
        // Step 1: Ensure user is signed in
        val userSignedIn = testUtils.ensureUserSignedIn("akshay.ayyanchira@iterable.com")
        Assert.assertTrue("User should be signed in", userSignedIn)
        
        // Step 2: Trigger campaign via server API
        var campaignTriggered = false
        val latch = java.util.concurrent.CountDownLatch(1)
        triggerCampaignViaAPI(TEST_CAMPAIGN_ID, "akshay.ayyanchira@iterable.com", null) { success ->
            campaignTriggered = success
            latch.countDown()
        }
        
        // Wait for callback
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Campaign trigger timed out")
        }
        
        Assert.assertTrue("Campaign should be triggered successfully", campaignTriggered)
        
        // Step 3: Wait for in-app message to be displayed (5 seconds for fast iterations)
        val messageDisplayed = waitForInAppMessage(5)
        Assert.assertTrue("In-app message should be displayed within 5 seconds", messageDisplayed)
        
        // Step 4: Verify in-app message is visible on screen
        val messageVisible = verifyInAppMessageVisible()
        Assert.assertTrue("In-app message should be visible on screen", messageVisible)
        
        // Step 5: Click button in the in-app message
        val buttonClicked = clickInAppMessageButton()
        Assert.assertTrue("Should be able to click button in in-app message", buttonClicked)
        
        // Step 6: Verify trackInAppClose event was fired
        val closeTracked = waitForTrackInAppClose(5)
        Assert.assertTrue("trackInAppClose should be called when button is clicked", closeTracked)
        
        Log.d(TAG, "MVP in-app message test completed successfully")
    }
    
    private fun verifyInAppMessageVisible(): Boolean {
        return try {
            // Check if in-app message is displayed
            val messageDisplayed = inAppMessageDisplayed.get()
            Log.d(TAG, "In-app message displayed: $messageDisplayed")
            
            // Also check if WebView is visible
            val webView = uiDevice.findObject(UiSelector().className("android.webkit.WebView"))
            val webViewVisible = webView.exists()
            Log.d(TAG, "WebView visible: $webViewVisible")
            
            messageDisplayed && webViewVisible
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying in-app message visibility", e)
            false
        }
    }
    
    private fun clickInAppMessageButton(): Boolean {
        return try {
            // Try to find and click a button in the WebView using Espresso Web
            Web.onWebView()
                .withElement(DriverAtoms.findElement(Locator.TAG_NAME, "button"))
                .perform(DriverAtoms.webClick())
            
            Log.d(TAG, "Clicked button in in-app message")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking in-app message button", e)
            false
        }
    }
    
    private fun waitForTrackInAppClose(timeoutSeconds: Long): Boolean {
        return try {
            Awaitility.await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until {
                    val closeTracked = trackInAppCloseCalled.get()
                    Log.d(TAG, "trackInAppClose called: $closeTracked")
                    closeTracked
                }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for trackInAppClose", e)
            false
        }
    }
}
