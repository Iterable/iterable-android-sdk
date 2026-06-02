package com.iterable.integration.tests

import android.content.Intent
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
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.integration.tests.activities.EmbeddedMessageTestActivity
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedView
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewType
import org.awaitility.Awaitility
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmbeddedMessageIntegrationTest : BaseIntegrationTest() {
    
    companion object {
        private const val TAG = "EmbeddedMessageIntegrationTest"
        private const val TEST_PLACEMENT_ID = TestConstants.TEST_EMBEDDED_PLACEMENT_ID
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
        // This prevents in-app messages from obscuring the embedded message test screen
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
        launchAppAndNavigateToEmbeddedTesting()
    }
    
    @After
    override fun tearDown() {
        super.tearDown()
    }
    
    private fun launchAppAndNavigateToEmbeddedTesting() {
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
        
        // Step 2: Click the "Embedded Messages" button to navigate to EmbeddedMessageTestActivity
        Log.d(TAG, "🔧 Step 2: Clicking 'Embedded Messages' button...")
        val embeddedButton = uiDevice.findObject(UiSelector().resourceId("com.iterable.integration.tests:id/btnEmbeddedMessages"))
        if (embeddedButton.exists()) {
            embeddedButton.click()
            Log.d(TAG, "🔧 Clicked Embedded Messages button successfully")
        } else {
            Log.e(TAG, "❌ Embedded Messages button not found!")
            Assert.fail("Embedded Messages button not found in MainActivity")
        }
        
        // Step 3: Wait for EmbeddedMessageTestActivity to load
        Log.d(TAG, "🔧 Step 3: Waiting for EmbeddedMessageTestActivity to load...")
        Thread.sleep(2000) // Give time for navigation
        
        Log.d(TAG, "🔧 App navigation complete: Now on EmbeddedMessageTestActivity!")
    }
    
    @Test
    @Ignore(
        "SDK-115 follow-up: BCIT backend currently returns `\"placements\": []` from " +
        "/api/embedded-messaging/messages for the CI user (bcituser@iterable.com) even " +
        "after updateUser({isPremium:true}) propagates. The campaign / user configuration " +
        "in the BCIT Iterable project needs review before re-enabling — the test logic is " +
        "fine, the backend just isn't delivering an eligible message here."
    )
    fun testEmbeddedMessageMVP() {
        // Step 1: Ensure user is signed in
        Log.d(TAG, "📧 Step 1: Ensuring user is signed in...")
        val userSignedIn = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        Assert.assertTrue("User should be signed in", userSignedIn)
        Log.d(TAG, "✅ User signed in successfully: ${TestConstants.TEST_USER_EMAIL}")
        
        // Step 2: Preliminary check - verify view is ready with placement ID
        Log.d(TAG, "🔍 Step 2: Checking view readiness with placement ID...")
        var viewReady = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull() as? EmbeddedMessageTestActivity
            
            activity?.let {
                val fragmentContainer = it.findViewById<androidx.fragment.app.FragmentContainerView>(R.id.embedded_message_container)
                viewReady = fragmentContainer != null
                if (viewReady) {
                    Log.d(TAG, "✅ View is ready with placementID - $TEST_PLACEMENT_ID")
                }
            }
        }
        Assert.assertTrue("FragmentContainerView should exist in EmbeddedMessageTestActivity", viewReady)
        
        // Step 3: Get initial placement IDs before updating user properties
        val initialPlacementIds = IterableApi.getInstance().embeddedManager.getPlacementIds().toSet()
        Log.d(TAG, "📊 Initial placement IDs: $initialPlacementIds")
        
        // Reset embedded push tracking to detect UpdateEmbedded push
        testUtils.setEmbeddedPushProcessed(false)
        
        // Step 4: Update user properties to make user eligible
        Log.d(TAG, "📝 Step 4: Updating user properties (isPremium = true)...")
        val dataFields = JSONObject().apply {
            put("isPremium", true)
        }
        IterableApi.getInstance().updateUser(dataFields)
        Log.d(TAG, "✅ User properties updated")
        
        // Step 5: Wait for backend to process and make user eligible
        Log.d(TAG, "⏳ Step 5: Waiting for backend to process user update...")
        Thread.sleep(3000)
        
        // Step 6: Wait for push-triggered sync (primary path)
        Log.d(TAG, "🔄 Step 6: Waiting for UpdateEmbedded push to trigger automatic sync...")
        val syncViaPush = waitForEmbeddedSyncViaPush(
            initialPlacementIds = initialPlacementIds,
            expectedPlacementId = TEST_PLACEMENT_ID,
            pushTimeoutSeconds = 10
        )
        
        var placementIds = IterableApi.getInstance().embeddedManager.getPlacementIds()
        var hasExpectedPlacement = placementIds.contains(TEST_PLACEMENT_ID)
        
        // Step 6b: Fallback to manual sync if push didn't work
        if (!syncViaPush || !hasExpectedPlacement) {
            Log.d(TAG, "⚠️ Push-triggered sync did not complete, falling back to manual sync...")
            Log.d(TAG, "🔄 Step 6b: Manually syncing embedded messages...")
            Thread.sleep(2000) // Give a bit more time in case push is still arriving
            
            // Check again before manual sync
            placementIds = IterableApi.getInstance().embeddedManager.getPlacementIds()
            hasExpectedPlacement = placementIds.contains(TEST_PLACEMENT_ID)
            
            if (!hasExpectedPlacement) {
                IterableApi.getInstance().embeddedManager.syncMessages()
                Thread.sleep(2000) // Wait for sync to complete
                placementIds = IterableApi.getInstance().embeddedManager.getPlacementIds()
                hasExpectedPlacement = placementIds.contains(TEST_PLACEMENT_ID)
                Log.d(TAG, "🔄 Placement IDs after manual sync: $placementIds")
            } else {
                Log.d(TAG, "✅ Messages synced via push (delayed), placement IDs: $placementIds")
            }
        } else {
            Log.d(TAG, "✅ Messages synced via push-triggered automatic sync, placement IDs: $placementIds")
        }
        
        // Step 7: Verify expected placement ID exists
        Log.d(TAG, "🔍 Step 7: Verifying placement ID exists...")
        Log.d(TAG, "📋 Found placement IDs: $placementIds")
        
        Assert.assertTrue(
            "Placement ID $TEST_PLACEMENT_ID should exist, but found: $placementIds",
            hasExpectedPlacement
        )
        Log.d(TAG, "✅ Placement ID $TEST_PLACEMENT_ID found")
        
        // Step 8: Get messages for the placement ID
        Log.d(TAG, "📨 Step 8: Getting messages for placement ID $TEST_PLACEMENT_ID...")
        val messages = IterableApi.getInstance().embeddedManager.getMessages(TEST_PLACEMENT_ID)
        Assert.assertTrue("Should have at least 1 message for placement $TEST_PLACEMENT_ID", messages!!.isNotEmpty())
        
        val message = messages.first()
        Log.d(TAG, "✅ Found message: ${message.metadata.messageId}")
        
        // Step 9: Display message using IterableEmbeddedView
        Log.d(TAG, "🎨 Step 9: Displaying message using IterableEmbeddedView...")
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull() as? EmbeddedMessageTestActivity
            
            if (activity != null) {
                val fragment =
                    IterableEmbeddedView.newInstance(IterableEmbeddedViewType.BANNER, message, null)
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.embedded_message_container, fragment)
                    .commitNow()
                Log.d(TAG, "✅ Fragment added to FragmentManager")
            } else {
                Assert.fail("EmbeddedMessageTestActivity not found in RESUMED stage")
            }
        }
        
        // Wait for fragment to be displayed
        Thread.sleep(1000)
        
        // Step 10: Verify display - check fragment exists
        Log.d(TAG, "✅ Step 10: Verifying embedded message is displayed...")
        var isEmbeddedFragmentDisplayed = false
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull() as? EmbeddedMessageTestActivity
            
            activity?.let { act ->
                val fragmentManager = act.supportFragmentManager
                fragmentManager.fragments.forEach { fragment ->
                    if (fragment is IterableEmbeddedView) {
                        isEmbeddedFragmentDisplayed = true
                        Log.d(TAG, "✅ Found IterableEmbeddedView fragment")
                    }
                }
            }
        }
        
        Assert.assertTrue(
            "IterableEmbeddedView fragment should be displayed",
            isEmbeddedFragmentDisplayed
        )
        
        Log.d(TAG, "✅ Embedded message is displayed, now interacting with button...")
        
        // Step 11: Interact with button - find and click first button
        Log.d(TAG, "🎯 Step 11: Clicking button in the embedded message...")
        
        // Try to find button by resource ID first
        val button = uiDevice.findObject(UiSelector().resourceId("com.iterable.iterableapi.ui:id/embedded_message_first_button"))
        
        if (button.exists()) {
            button.click()
            Log.d(TAG, "✅ Clicked embedded message button")
        } else {
            // Try to find by button text if available
            val buttonText = message.elements?.buttons?.firstOrNull()?.title
            if (buttonText != null) {
                val buttonByText = uiDevice.findObject(By.text(buttonText))
                if (buttonByText != null) {
                    buttonByText.click()
                    Log.d(TAG, "✅ Clicked embedded message button by text: $buttonText")
                } else {
                    Assert.fail("Button not found in the embedded message (tried resource ID and text: $buttonText)")
                }
            } else {
                Assert.fail("Button not found in the embedded message (tried resource ID, but no button text available)")
            }
        }
        
        // Step 12: Verify URL handler was called
        Log.d(TAG, "🎯 Step 12: Verifying URL handler was called after button click...")
        
        val urlHandlerCalled = waitForUrlHandler(timeoutSeconds = 3)
        Assert.assertTrue(
            "URL handler should have been called after clicking the button",
            urlHandlerCalled
        )
        
        // Step 13: Verify the correct URL was handled
        val handledUrl = getLastHandledUrl()
        Log.d(TAG, "🎯 URL handler received: $handledUrl")
        
        Assert.assertNotNull("Handled URL should not be null", handledUrl)
        Log.d(TAG, "✅ URL handler was called with URL: $handledUrl")
        
        Log.d(TAG, "✅✅✅ Test completed successfully! All steps passed.")
    }
}

