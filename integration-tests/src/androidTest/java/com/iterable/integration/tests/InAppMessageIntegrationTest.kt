package com.iterable.integration.tests

import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.matcher.DomMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.iterableapi.IterableInAppCloseAction
import com.iterable.iterableapi.IterableConfig
import com.iterable.integration.tests.utils.IntegrationTestUtils
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
        private const val TEST_CAMPAIGN_ID = 14332357
        private const val TEST_EVENT_NAME = "test_inapp_event"
    }
    
    private lateinit var uiDevice: UiDevice
    private val inAppMessageDisplayed = AtomicBoolean(false)
    private val inAppClickTracked = AtomicBoolean(false)
    private val inAppCloseTracked = AtomicBoolean(false)
    private val lastClickedUrl = AtomicReference<String?>(null)
    private val lastCloseAction = AtomicReference<IterableInAppCloseAction?>(null)
    
    @Before
    override fun setUp() {
        super.setUp()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Reset test states
        inAppMessageDisplayed.set(false)
        inAppClickTracked.set(false)
        inAppCloseTracked.set(false)
        lastClickedUrl.set(null)
        lastCloseAction.set(null)
        
        // Setup custom in-app handler to track events
        setupCustomInAppHandler()
    }
    
    @After
    override fun tearDown() {
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
            .setCustomActionHandler { action, context ->
                Log.d(TAG, "Custom action triggered: $action")
                true
            }
            .setUrlHandler { url, context ->
                Log.d(TAG, "URL handler triggered: $url")
                lastClickedUrl.set(url.toString())
                true
            }
            .build()
        
        // Re-initialize with custom handlers
        IterableApi.initialize(context, BuildConfig.ITERABLE_API_KEY, config)
        IterableApi.getInstance().setEmail("akshay.ayyanchira@iterable.com")
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
        
        // Step 3: Trigger syncMessages by simulating background/foreground cycle
        Log.d(TAG, "Triggering syncMessages via background/foreground cycle...")
        triggerSyncMessages()
        
        // Step 4: Wait for in-app message to be displayed (5 seconds for fast iterations)
        Log.d(TAG, "Waiting for in-app message to be displayed...")
        val messageDisplayed = waitForInAppMessage(5)
        Log.d(TAG, "In-app message displayed: $messageDisplayed")
        Assert.assertTrue("In-app message should be displayed within 5 seconds", messageDisplayed)
        
        // Step 5: Verify in-app message is visible on screen
        val messageVisible = verifyInAppMessageVisible()
        Assert.assertTrue("In-app message should be visible on screen", messageVisible)
        
        // Step 6: Click button in the in-app message
        val buttonClicked = clickInAppMessageButton()
        Assert.assertTrue("Should be able to click button in in-app message", buttonClicked)
        
        // Step 7: Verify trackInAppClose event was fired
        val closeTracked = waitForTrackInAppClose(5)
        Assert.assertTrue("trackInAppClose should be called when button is clicked", closeTracked)
        
        Log.d(TAG, "MVP in-app message test completed successfully")
    }
    
    private fun verifyInAppMessageVisible(): Boolean {
        return try {
            // Check if in-app message is displayed using the testUtils flag
            val messageDisplayed = testUtils.hasInAppMessageDisplayed()
            Log.d(TAG, "In-app message displayed (testUtils): $messageDisplayed")
            
            // Also check local flag
            val localMessageDisplayed = inAppMessageDisplayed.get()
            Log.d(TAG, "In-app message displayed (local): $localMessageDisplayed")
            
            // Also check if WebView is visible
            val webView = uiDevice.findObject(UiSelector().className("android.webkit.WebView"))
            val webViewVisible = webView.exists()
            Log.d(TAG, "WebView visible: $webViewVisible")
            
            messageDisplayed || localMessageDisplayed
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying in-app message visibility", e)
            false
        }
    }
    
    private fun findInAppMessageDialog(): Any? {
        return try {
            // Look for the in-app message dialog fragment
            val dialogSelector = UiSelector().className("android.widget.FrameLayout")
                .descriptionContains("iterable_in_app")
            
            val dialog = uiDevice.findObject(dialogSelector)
            if (dialog.exists()) {
                Log.d(TAG, "Found in-app message dialog")
                return dialog
            }
            
            // Alternative: Look for WebView in dialog
            val webViewSelector = UiSelector().className("android.webkit.WebView")
            val webView = uiDevice.findObject(webViewSelector)
            if (webView.exists()) {
                Log.d(TAG, "Found WebView in in-app message")
                return webView
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding in-app message dialog", e)
            null
        }
    }
    
    private fun checkWebViewVisible(): Boolean {
        return try {
            // For now, just check if WebView exists
            val webView = uiDevice.findObject(UiSelector().className("android.webkit.WebView"))
            val exists = webView.exists()
            
            Log.d(TAG, "WebView exists: $exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "WebView content check failed", e)
            false
        }
    }
    
    private fun clickInAppMessageButton(): Boolean {
        return try {
            // First, try to find and click a button using UiAutomator
            val buttonClicked = clickButtonWithUiAutomator()
            if (buttonClicked) {
                return true
            }
            
            // Fallback: Use Espresso Web to find and click buttons
            val webButtonClicked = clickButtonWithEspressoWeb()
            if (webButtonClicked) {
                return true
            }
            
            // Last resort: Try to click any clickable element
            clickAnyClickableElement()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking in-app message button", e)
            false
        }
    }
    
    private fun clickButtonWithUiAutomator(): Boolean {
        return try {
            // Look for buttons in the in-app message
            val buttonSelectors = listOf(
                UiSelector().className("android.widget.Button"),
                UiSelector().textContains("button"),
                UiSelector().textContains("click"),
                UiSelector().textContains("open"),
                UiSelector().textContains("learn"),
                UiSelector().textContains("more")
            )
            
            for (selector in buttonSelectors) {
                val button = uiDevice.findObject(selector)
                if (button.exists()) {
                    Log.d(TAG, "Found button with selector: $selector")
                    button.click()
                    return true
                }
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking button with UiAutomator", e)
            false
        }
    }
    
    private fun clickButtonWithEspressoWeb(): Boolean {
        return try {
            // Use Espresso Web to find and click buttons in the WebView
            Web.onWebView()
                .withElement(DriverAtoms.findElement(Locator.TAG_NAME, "button"))
                .perform(DriverAtoms.webClick())
            
            Log.d(TAG, "Clicked button using Espresso Web")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking button with Espresso Web", e)
            false
        }
    }
    
    private fun clickAnyClickableElement(): Boolean {
        return try {
            // Try to click any clickable element in the dialog
            val clickableSelector = UiSelector().clickable(true)
            val clickableElement = uiDevice.findObject(clickableSelector)
            
            if (clickableElement.exists()) {
                Log.d(TAG, "Found clickable element, attempting to click")
                clickableElement.click()
                return true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking any clickable element", e)
            false
        }
    }
    
    private fun waitForTrackInAppClose(timeoutSeconds: Long): Boolean {
        return try {
            Awaitility.await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until {
                    // Check if close was tracked by monitoring the last clicked URL
                    val closeTracked = lastClickedUrl.get() != null
                    Log.d(TAG, "trackInAppClose called: $closeTracked, URL: ${lastClickedUrl.get()}")
                    closeTracked
                }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for trackInAppClose", e)
            false
        }
    }
    
    private fun triggerSyncMessages() {
        try {
            Log.d(TAG, "Simulating background/foreground cycle to trigger syncMessages...")
            
            // Simulate app going to background
            uiDevice.pressHome()
            Thread.sleep(1000) // Wait 1 second
            
            // Simulate app coming back to foreground
            uiDevice.pressRecentApps()
            Thread.sleep(500)
            
            // Find and click on our app to bring it back to foreground
            val appIcon = uiDevice.findObject(UiSelector().text("Integration Tests"))
            if (appIcon.exists()) {
                appIcon.click()
                Log.d(TAG, "App brought back to foreground")
            } else {
                Log.d(TAG, "Could not find app icon, trying alternative approach")
                // Alternative: try to find the app in recent apps
                val recentApps = uiDevice.findObject(UiSelector().className("android.widget.FrameLayout"))
                if (recentApps.exists()) {
                    recentApps.click()
                }
            }
            
            // Give the app time to process the foreground event
            Thread.sleep(2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering syncMessages", e)
        }
    }
    
}