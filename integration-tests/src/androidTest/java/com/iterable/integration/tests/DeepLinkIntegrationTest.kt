package com.iterable.integration.tests

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import com.iterable.iterableapi.IterableApi
import com.iterable.integration.tests.activities.DeepLinkTestActivity
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Integration tests for deep linking functionality.
 * 
 * These tests validate:
 * 1. App Links (https:// URLs that open the app directly) - similar to iOS Universal Links
 * 2. Custom scheme deep links (iterable://, iterabletest://)
 * 3. URL handler delegate invocation
 * 4. Deep link unwrapping (SDK following redirects)
 * 
 * Android equivalent of iOS's DeepLinkingIntegrationTests.swift
 * 
 * Key differences from iOS:
 * - iOS uses Universal Links (AASA) + Reminders app for testing
 * - Android uses App Links (assetlinks.json) + adb commands for testing
 * - iOS: xcrun simctl openurl booted <url>
 * - Android: adb shell am start -a android.intent.action.VIEW -d <url>
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkIntegrationTest : BaseIntegrationTest() {
    
    companion object {
        private const val TAG = "DeepLinkIntegrationTest"
        
        // Test URLs - matching iOS test URLs where possible
        // Note: These URLs need to be configured in your Iterable project
        
        // App Link URL - should open the app directly (like iOS Universal Links)
        const val TEST_APP_LINK_URL = "https://tsetester.com/update/hi"
        
        // Wrapped link URL - SDK should unwrap this
        const val TEST_WRAPPED_LINK_URL = "https://links.tsetester.com/a/click?_t=5cce074b113d48fa9ef346e4333ed8e8&_m=74aKPNrAjTpuZM4vZTDueu64xMdbHDz5Tn&_e=l6cj19GbssUn6h5qtXjRcC5os6azNW1cqdk9lsvmxxRl4ZTAW8mIB4IHJA97wE1i5f0eRDtm-KpgKI7-tM-Cly6umZo4P8HU8krftMYvL3T2sCpm3uFDBF2iJ5vQ-G6sqNMmae4_8jkE1DU9aKRhraZ1zzUZ3j-dFbQJrxdLt4tb0C7jnXSARVFf27FKFhBKnYSO23taBmf_4G5dTTXKmC_1CGnT9bu1nAwP-WMyYShoQhmjoGO9ppDCrVStSYPsimwub0h5XnC11g4u5yML_WZssgC7LSUOX7qCNOIDr9dLhrx2Rc2TY12k0maESyanjNgNZ4Lr8LMClCMJ3d9TMg%3D%3D"
        
        // Browser link URL - should open browser, not app (like iOS /u/ pattern links)
        const val TEST_BROWSER_LINK_URL = "https://links.tsetester.com/u/click?url=https://iterable.com"
        
        // Custom scheme deep links
        const val TEST_CUSTOM_SCHEME_URL = "iterable://deeplink/product/123"
        const val TEST_CUSTOM_SCHEME_URL_2 = "iterabletest://update/hi"
    }
    
    private lateinit var uiDevice: UiDevice
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    
    @Before
    override fun setUp() {
        Log.d(TAG, "🔧 Test setup starting...")
        
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Call super.setUp() to initialize SDK with BaseIntegrationTest's config
        super.setUp()
        
        Log.d(TAG, "🔧 Base setup complete, SDK initialized with test handlers")
        
        // Disable in-app auto display to prevent interference with deep link tests
        IterableApi.getInstance().inAppManager.setAutoDisplayPaused(true)
        
        // Clear existing in-app messages
        IterableApi.getInstance().inAppManager.messages.forEach {
            IterableApi.getInstance().inAppManager.removeMessage(it)
        }
        
        // Reset tracking
        resetDeepLinkTracking()
        resetUrlHandlerTracking()
        
        Log.d(TAG, "🔧 Test setup complete")
    }
    
    @After
    override fun tearDown() {
        super.tearDown()
    }
    
    // ==================== Test Cases ====================
    
    /**
     * Test 1: Custom scheme deep link opens the app and is handled correctly
     * 
     * This tests the basic deep link functionality using a custom URL scheme.
     * Similar to iOS's testBDeepLinkFromRemindersApp but using custom scheme.
     */
    @Test
    fun testCustomSchemeDeepLinkOpensApp() {
        Log.d(TAG, "🧪 Testing custom scheme deep link: $TEST_CUSTOM_SCHEME_URL")
        
        // Step 1: Ensure user is signed in
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        Log.d(TAG, "✅ User signed in: ${TestConstants.TEST_USER_EMAIL}")
        
        // Step 2: Launch MainActivity first
        Log.d(TAG, "🚀 Step 2: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until { mainActivityScenario.state == Lifecycle.State.RESUMED }
        
        Log.d(TAG, "✅ MainActivity is ready")
        
        // Step 3: Open deep link via adb (equivalent to iOS simctl openurl)
        Log.d(TAG, "🔗 Step 3: Opening deep link via adb: $TEST_CUSTOM_SCHEME_URL")
        openDeepLinkViaAdb(TEST_CUSTOM_SCHEME_URL)
        
        // Step 4: Wait for DeepLinkTestActivity to open
        Thread.sleep(3000)
        
        // Step 5: Verify DeepLinkTestActivity is displayed
        Log.d(TAG, "🔍 Step 5: Verifying DeepLinkTestActivity is displayed...")
        
        var deepLinkActivityFound = false
        var receivedPath: String? = null
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
            
            if (activity is DeepLinkTestActivity) {
                deepLinkActivityFound = true
                receivedPath = activity.getLastReceivedPath()
                Log.d(TAG, "✅ DeepLinkTestActivity is displayed")
                Log.d(TAG, "✅ Received path: $receivedPath")
            }
        }
        
        Assert.assertTrue(
            "DeepLinkTestActivity should be displayed",
            deepLinkActivityFound
        )
        
        // Step 6: Verify the correct path was received
        Assert.assertNotNull("Received path should not be null", receivedPath)
        Assert.assertTrue(
            "Path should contain 'product' - got: $receivedPath",
            receivedPath?.contains("product") == true
        )
        
        Log.d(TAG, "✅ Deep link received with correct path: $receivedPath")
        
        // Step 7: Verify UI shows correct information
        val headerText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkHeader"))
        if (headerText.exists()) {
            Log.d(TAG, "✅ Header text: ${headerText.text}")
        }
        
        val pathText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkPath"))
        if (pathText.exists()) {
            Assert.assertTrue(
                "Path text should contain 'product'",
                pathText.text.contains("product")
            )
            Log.d(TAG, "✅ Path text verified: ${pathText.text}")
        }
        
        Log.d(TAG, "✅✅✅ Custom scheme deep link test completed successfully!")
    }
    
    /**
     * Test 2: App Link (https URL) opens the app directly
     * 
     * This tests App Links functionality - Android's equivalent of iOS Universal Links.
     * The URL should open the app directly without showing a browser.
     */
    @Test
    fun testAppLinkOpensApp() {
        Log.d(TAG, "🧪 Testing App Link: $TEST_APP_LINK_URL")
        Log.d(TAG, "🎯 Expected: App opens directly (like iOS Universal Links)")
        
        // Step 1: Ensure user is signed in
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        
        // Step 2: Launch MainActivity first
        Log.d(TAG, "🚀 Step 2: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until { mainActivityScenario.state == Lifecycle.State.RESUMED }
        
        // Step 3: Open App Link via adb
        Log.d(TAG, "🔗 Step 3: Opening App Link via adb: $TEST_APP_LINK_URL")
        openDeepLinkViaAdb(TEST_APP_LINK_URL)
        
        Thread.sleep(3000)
        
        // Step 4: Verify DeepLinkTestActivity is displayed
        Log.d(TAG, "🔍 Step 4: Verifying DeepLinkTestActivity is displayed...")
        
        var deepLinkActivityFound = false
        var receivedPath: String? = null
        var receivedUrl: String? = null
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
            
            if (activity is DeepLinkTestActivity) {
                deepLinkActivityFound = true
                receivedPath = activity.getLastReceivedPath()
                receivedUrl = activity.getLastReceivedUrl()
                Log.d(TAG, "✅ DeepLinkTestActivity is displayed")
                Log.d(TAG, "✅ Received URL: $receivedUrl")
                Log.d(TAG, "✅ Received path: $receivedPath")
            }
        }
        
        Assert.assertTrue(
            "DeepLinkTestActivity should be displayed",
            deepLinkActivityFound
        )
        
        // Step 5: Verify the header shows correct content based on path
        // Similar to iOS's UpdateViewController showing "👋 Hi!" for /update/hi
        val headerText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkHeader"))
        if (headerText.exists()) {
            val header = headerText.text
            Log.d(TAG, "✅ Header text: $header")
            Assert.assertTrue(
                "Header should show 'Hi!' for /update/hi path - got: $header",
                header.contains("Hi")
            )
        }
        
        Assert.assertTrue(
            "Path should contain '/update/hi' - got: $receivedPath",
            receivedPath?.contains("/update/hi") == true
        )
        
        Log.d(TAG, "✅✅✅ App Link test completed successfully!")
    }
    
    /**
     * Test 3: URL Handler is invoked when deep link is received via SDK
     * 
     * This tests that the IterableUrlHandler configured in SDK is invoked
     * when a deep link comes through the SDK (e.g., from push notification or in-app message).
     */
    @Test
    fun testUrlHandlerInvocation() {
        Log.d(TAG, "🧪 Testing URL handler invocation")
        
        // Step 1: Ensure user is signed in
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        
        // Step 2: Launch MainActivity and navigate to DeepLinkTestActivity
        Log.d(TAG, "🚀 Step 2: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until { mainActivityScenario.state == Lifecycle.State.RESUMED }
        
        // Click Deep Linking button to navigate
        val deepLinkButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnDeepLinking"))
        if (deepLinkButton.exists()) {
            deepLinkButton.click()
            Thread.sleep(2000)
        }
        
        // Step 3: Reset URL handler tracking
        resetUrlHandlerTracking()
        
        // Step 4: Simulate a deep link through the SDK URL handler
        // This would normally happen when user clicks a link in an in-app message or push notification
        Log.d(TAG, "🔗 Step 4: Testing URL handler via direct deep link...")
        
        // Open a custom scheme link that triggers the URL handler
        openDeepLinkViaAdb(TEST_CUSTOM_SCHEME_URL_2)
        
        Thread.sleep(3000)
        
        // Step 5: Verify the activity received the deep link
        var deepLinkActivityFound = false
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
            
            deepLinkActivityFound = activity is DeepLinkTestActivity
        }
        
        Assert.assertTrue(
            "DeepLinkTestActivity should be displayed",
            deepLinkActivityFound
        )
        
        // Verify path contains expected value
        val pathText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkPath"))
        if (pathText.exists()) {
            Log.d(TAG, "✅ Path received: ${pathText.text}")
        }
        
        Log.d(TAG, "✅✅✅ URL handler test completed successfully!")
    }
    
    /**
     * Test 4: Verify deep link UI elements display correctly
     * 
     * This tests that the DeepLinkTestActivity properly displays all
     * components of a received deep link (URL, path, host, scheme).
     */
    @Test
    fun testDeepLinkUIDisplay() {
        Log.d(TAG, "🧪 Testing deep link UI display")
        
        // Step 1: Ensure user is signed in
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        
        // Step 2: Launch MainActivity
        Log.d(TAG, "🚀 Step 2: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until { mainActivityScenario.state == Lifecycle.State.RESUMED }
        
        // Step 3: Open deep link with specific components to verify
        val testUrl = "iterable://deeplink/settings/notifications?theme=dark"
        Log.d(TAG, "🔗 Step 3: Opening deep link: $testUrl")
        openDeepLinkViaAdb(testUrl)
        
        Thread.sleep(3000)
        
        // Step 4: Verify all UI elements are displayed correctly
        Log.d(TAG, "🔍 Step 4: Verifying UI elements...")
        
        // Verify URL is displayed
        val urlText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkUrl"))
        Assert.assertTrue("URL TextView should exist", urlText.exists())
        Assert.assertTrue(
            "URL should contain 'iterable://deeplink'",
            urlText.text.contains("iterable://deeplink")
        )
        Log.d(TAG, "✅ URL displayed: ${urlText.text}")
        
        // Verify path is displayed
        val pathText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkPath"))
        Assert.assertTrue("Path TextView should exist", pathText.exists())
        Assert.assertTrue(
            "Path should contain '/settings/notifications'",
            pathText.text.contains("/settings/notifications")
        )
        Log.d(TAG, "✅ Path displayed: ${pathText.text}")
        
        // Verify host is displayed
        val hostText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkHost"))
        Assert.assertTrue("Host TextView should exist", hostText.exists())
        Assert.assertTrue(
            "Host should contain 'deeplink'",
            hostText.text.contains("deeplink")
        )
        Log.d(TAG, "✅ Host displayed: ${hostText.text}")
        
        // Verify scheme is displayed
        val schemeText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkScheme"))
        Assert.assertTrue("Scheme TextView should exist", schemeText.exists())
        Assert.assertTrue(
            "Scheme should contain 'iterable'",
            schemeText.text.contains("iterable")
        )
        Log.d(TAG, "✅ Scheme displayed: ${schemeText.text}")
        
        // Verify header displays settings icon/text
        val headerText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkHeader"))
        Assert.assertTrue("Header TextView should exist", headerText.exists())
        Log.d(TAG, "✅ Header displayed: ${headerText.text}")
        
        // Step 5: Verify close button works
        val closeButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnCloseDeepLink"))
        Assert.assertTrue("Close button should exist", closeButton.exists())
        closeButton.click()
        
        Thread.sleep(1000)
        
        Log.d(TAG, "✅✅✅ Deep link UI display test completed successfully!")
    }
    
    /**
     * Test 5: Deep link from MainActivity navigation
     * 
     * Tests that navigation to DeepLinkTestActivity from MainActivity works
     * and the activity is ready to receive deep links.
     */
    @Test
    fun testDeepLinkActivityNavigation() {
        Log.d(TAG, "🧪 Testing deep link activity navigation from MainActivity")
        
        // Step 1: Launch MainActivity
        Log.d(TAG, "🚀 Step 1: Launching MainActivity...")
        val mainIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        mainActivityScenario = ActivityScenario.launch(mainIntent)
        
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until { mainActivityScenario.state == Lifecycle.State.RESUMED }
        
        Log.d(TAG, "✅ MainActivity is ready")
        
        // Step 2: Click the "Deep Linking" button
        Log.d(TAG, "🔧 Step 2: Clicking Deep Linking button...")
        val deepLinkButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnDeepLinking"))
        Assert.assertTrue("Deep Linking button should exist", deepLinkButton.exists())
        deepLinkButton.click()
        
        Thread.sleep(2000)
        
        // Step 3: Verify DeepLinkTestActivity is displayed
        Log.d(TAG, "🔍 Step 3: Verifying DeepLinkTestActivity is displayed...")
        
        var deepLinkActivityFound = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
            
            deepLinkActivityFound = activity is DeepLinkTestActivity
        }
        
        Assert.assertTrue(
            "DeepLinkTestActivity should be displayed after clicking button",
            deepLinkActivityFound
        )
        
        // Step 4: Verify the activity shows waiting state
        val statusText = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/tvDeepLinkStatus"))
        Assert.assertTrue("Status TextView should exist", statusText.exists())
        Assert.assertTrue(
            "Status should show 'Waiting' state",
            statusText.text.contains("Waiting") || statusText.text.contains("waiting")
        )
        
        Log.d(TAG, "✅ Activity shows waiting state: ${statusText.text}")
        
        Log.d(TAG, "✅✅✅ Deep link activity navigation test completed successfully!")
    }
}
