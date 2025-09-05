package com.iterable.integration.tests

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PushNotificationIntegrationTest : BaseIntegrationTest() {
    
    private lateinit var uiDevice: UiDevice
    private lateinit var notificationManager: NotificationManager
    
    override fun setUp() {
        super.setUp()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    @Test
    fun testPushNotificationConfiguration() {
        // Test 1: Verify push notification configuration
        assertTrue("Push notification should be configured", isPushNotificationConfigured())
    }
    
    @Test
    fun testDeviceReceivesPushNotification() {
        // Test 2: Device receives push notification
        val campaignId = "test_push_campaign"
        
        // Send test push notification
        assertTrue("Should send push notification", sendTestPushNotification(campaignId))
        
        // Wait for notification to be received
        assertTrue("Device should receive push notification", waitForPushNotification())
    }
    
    @Test
    fun testNotificationPermissionGranted() {
        // Test 3: Device has permission granted to show notification
        assertTrue("Notification permission should be granted", hasNotificationPermission())
    }
    
    @Test
    fun testPushNotificationDisplayed() {
        // Test 4: Push notification is displayed
        val campaignId = "test_display_campaign"
        
        // Send test push notification
        sendTestPushNotification(campaignId)
        
        // Wait for notification to be displayed
        assertTrue("Push notification should be displayed", waitForNotificationDisplayed())
    }
    
    @Test
    fun testPushDeliveryMetricsCaptured() {
        // Test 5: Push delivery metrics are captured
        val campaignId = "test_metrics_campaign"
        
        // Send test push notification
        sendTestPushNotification(campaignId)
        
        // Wait for metrics to be captured
        assertTrue("Push delivery metrics should be captured", waitForMetricsCaptured())
    }
    
    @Test
    fun testTappingPushNotificationTracksOpen() {
        // Test 6: Tapping on Push notification leads to trackPush opens
        val campaignId = "test_tap_campaign"
        
        // Send test push notification
        sendTestPushNotification(campaignId)
        
        // Wait for notification to appear
        assertTrue("Notification should appear", waitForNotificationDisplayed())
        
        // Tap on the notification
        tapOnNotification()
        
        // Verify trackPush open is called
        assertTrue("trackPush open should be called", waitForTrackPushOpen())
    }
    
    @Test
    fun testTappingButtonWithDeepLinkInvokesHandlers() {
        // Test 7: Tapping on buttons with deep link invokes SDK handlers
        val campaignId = "test_deeplink_campaign"
        
        // Send test push notification with deep link button
        sendTestPushNotificationWithDeepLink(campaignId)
        
        // Wait for notification to appear
        assertTrue("Notification should appear", waitForNotificationDisplayed())
        
        // Tap on the deep link button
        tapOnDeepLinkButton()
        
        // Verify deep link handler is invoked
        assertTrue("Deep link handler should be invoked", waitForDeepLinkHandlerInvoked())
    }
    
    @Test
    fun testSilentPushWorks() {
        // Test 8: Silent push works (for in-app messages)
        val campaignId = "test_silent_campaign"
        
        // Send silent push notification
        sendSilentPushNotification(campaignId)
        
        // Verify silent push is processed
        assertTrue("Silent push should be processed", waitForSilentPushProcessed())
    }
    
    // Helper methods
    private fun isPushNotificationConfigured(): Boolean {
        return try {
            // Check if Firebase is configured
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            firebaseApp != null
        } catch (e: Exception) {
            false
        }
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Pre-Android 13, notifications are enabled by default
        }
    }
    
    private fun waitForNotificationDisplayed(): Boolean {
        return waitForCondition({
            // Check if notification is in the notification shade
            uiDevice.findObject(UiSelector().textContains("Iterable")).exists()
        })
    }
    
    private fun waitForMetricsCaptured(): Boolean {
        return waitForCondition({
            // Check if metrics were sent to Iterable backend
            testUtils.hasMetricsBeenSent()
        })
    }
    
    private fun tapOnNotification() {
        // Find and tap on the notification
        val notification = uiDevice.findObject(UiSelector().textContains("Iterable"))
        if (notification.exists()) {
            notification.click()
        }
    }
    
    private fun waitForTrackPushOpen(): Boolean {
        return waitForCondition({
            // Check if trackPush open event was sent
            testUtils.hasTrackPushOpenBeenCalled()
        })
    }
    
    private fun sendTestPushNotificationWithDeepLink(campaignId: String): Boolean {
        return testUtils.sendPushNotificationWithDeepLink(campaignId)
    }
    
    private fun tapOnDeepLinkButton() {
        // Find and tap on the deep link button in notification
        val deepLinkButton = uiDevice.findObject(UiSelector().textContains("Open"))
        if (deepLinkButton.exists()) {
            deepLinkButton.click()
        }
    }
    
    private fun waitForDeepLinkHandlerInvoked(): Boolean {
        return waitForCondition({
            // Check if deep link handler was invoked
            testUtils.hasDeepLinkHandlerBeenInvoked()
        })
    }
    
    private fun sendSilentPushNotification(campaignId: String): Boolean {
        var success = false
        val latch = CountDownLatch(1)
        
        val result = testUtils.sendSilentPushNotification(campaignId)
        success = result
        latch.countDown()
        
        // Wait for callback
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            return false
        }
        
        return success
    }
    
    private fun waitForSilentPushProcessed(): Boolean {
        return waitForCondition({
            // Check if silent push was processed
            testUtils.hasSilentPushBeenProcessed()
        })
    }
} 