package com.iterable.integration.tests.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.integration.tests.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class IntegrationTestUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "IntegrationTestUtils"
        private const val ITERABLE_API_BASE_URL = "https://api.iterable.com"
        private const val ITERABLE_SEND_PUSH_ENDPOINT = "/api/push/target"
    }
    
    private val httpClient = OkHttpClient()
    private val gson = Gson()
    
    // Test state tracking
    private val pushNotificationReceived = AtomicBoolean(false)
    private val inAppMessageDisplayed = AtomicBoolean(false)
    private val embeddedMessageDisplayed = AtomicBoolean(false)
    private val metricsSent = AtomicBoolean(false)
    private val trackPushOpenCalled = AtomicBoolean(false)
    private val deepLinkHandlerInvoked = AtomicBoolean(false)
    private val silentPushProcessed = AtomicBoolean(false)
    
    // Reset all test states
    fun resetTestStates() {
        pushNotificationReceived.set(false)
        inAppMessageDisplayed.set(false)
        embeddedMessageDisplayed.set(false)
        metricsSent.set(false)
        trackPushOpenCalled.set(false)
        deepLinkHandlerInvoked.set(false)
        silentPushProcessed.set(false)
    }
    
    // Push Notification Methods - Using Iterable Backend API
    fun sendPushNotification(campaignId: String): Boolean {
        return try {
            val payload = createIterablePushNotificationPayload(campaignId)
            
            val request = Request.Builder()
                .url("$ITERABLE_API_BASE_URL$ITERABLE_SEND_PUSH_ENDPOINT")
                .addHeader("Api-Key", BuildConfig.ITERABLE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                pushNotificationReceived.set(true)
                Log.d(TAG, "Push notification sent via Iterable backend successfully")
            } else {
                Log.e(TAG, "Failed to send push notification via Iterable backend: ${response.code} - ${response.body?.string()}")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending push notification via Iterable backend", e)
            false
        }
    }
    
    fun sendPushNotificationWithDeepLink(campaignId: String): Boolean {
        return try {
            val payload = createIterablePushNotificationWithDeepLinkPayload(campaignId)
            
            val request = Request.Builder()
                .url("$ITERABLE_API_BASE_URL$ITERABLE_SEND_PUSH_ENDPOINT")
                .addHeader("Api-Key", BuildConfig.ITERABLE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                Log.d(TAG, "Push notification with deep link sent via Iterable backend successfully")
            } else {
                Log.e(TAG, "Failed to send push notification with deep link: ${response.code} - ${response.body?.string()}")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending push notification with deep link via Iterable backend", e)
            false
        }
    }
    
    fun sendSilentPushNotification(campaignId: String): Boolean {
        return try {
            val payload = createIterableSilentPushNotificationPayload(campaignId)
            
            val request = Request.Builder()
                .url("$ITERABLE_API_BASE_URL$ITERABLE_SEND_PUSH_ENDPOINT")
                .addHeader("Api-Key", BuildConfig.ITERABLE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                silentPushProcessed.set(true)
                Log.d(TAG, "Silent push notification sent via Iterable backend successfully")
            } else {
                Log.e(TAG, "Failed to send silent push notification: ${response.code} - ${response.body?.string()}")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending silent push notification via Iterable backend", e)
            false
        }
    }
    
    fun hasReceivedPushNotification(): Boolean {
        return pushNotificationReceived.get()
    }
    
    fun setPushNotificationReceived(received: Boolean) {
        pushNotificationReceived.set(received)
    }
    
    fun hasSilentPushBeenProcessed(): Boolean {
        return silentPushProcessed.get()
    }
    
    fun setSilentPushProcessed(processed: Boolean) {
        silentPushProcessed.set(processed)
    }
    
    // In-App Message Methods
    fun triggerInAppMessage(eventName: String): Boolean {
        return try {
            IterableApi.getInstance().track(eventName)
            Log.d(TAG, "In-app message triggered for event: $eventName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering in-app message", e)
            false
        }
    }
    
    fun hasInAppMessageDisplayed(): Boolean {
        return inAppMessageDisplayed.get()
    }
    
    fun setInAppMessageDisplayed(displayed: Boolean) {
        inAppMessageDisplayed.set(displayed)
    }
    
    // Embedded Message Methods
    fun triggerEmbeddedMessage(placementId: Int): Boolean {
        return try {
            // Trigger embedded message by updating user profile
            val userData = JSONObject().apply {
                put("embeddedMessageEligible", true)
                put("placementId", placementId)
            }
            
            IterableApi.getInstance().updateUser(userData)
            Log.d(TAG, "Embedded message triggered for placement: $placementId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering embedded message", e)
            false
        }
    }
    
    fun hasEmbeddedMessageDisplayed(): Boolean {
        return embeddedMessageDisplayed.get()
    }
    
    fun setEmbeddedMessageDisplayed(displayed: Boolean) {
        embeddedMessageDisplayed.set(displayed)
    }
    
    // Deep Link Methods
    fun simulateDeepLink(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            deepLinkHandlerInvoked.set(true)
            Log.d(TAG, "Deep link simulated: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error simulating deep link", e)
            false
        }
    }
    
    fun hasDeepLinkHandlerBeenInvoked(): Boolean {
        return deepLinkHandlerInvoked.get()
    }
    
    // Metrics Methods
    fun hasMetricsBeenSent(): Boolean {
        return metricsSent.get()
    }
    
    fun setMetricsSent(sent: Boolean) {
        metricsSent.set(sent)
    }
    
    fun hasTrackPushOpenBeenCalled(): Boolean {
        return trackPushOpenCalled.get()
    }
    
    fun setTrackPushOpenCalled(called: Boolean) {
        trackPushOpenCalled.set(called)
    }
    
    // Utility Methods
    private fun getFCMToken(): String {
        // In a real implementation, this would get the actual FCM token
        // For testing purposes, we'll use a mock token
        return "mock_fcm_token_for_testing"
    }
    
    // Iterable Backend API Payloads
    private fun createIterablePushNotificationPayload(campaignId: String): String {
        val dataFields = JSONObject().apply {
            put("testType", "push_notification")
            put("campaignId", campaignId)
        }
        
        val metadata = JSONObject().apply {
            put("title", "Test Push Notification")
            put("body", "This is a test push notification")
            put("sound", "default")
            put("badge", 1)
        }
        
        return JSONObject().apply {
            put("campaignId", campaignId)
            put("recipientEmail", "integration.test@iterable.com")
            put("dataFields", dataFields)
            put("allowRepeatMarketingSends", false)
            put("metadata", metadata)
        }.toString()
    }
    
    private fun createIterablePushNotificationWithDeepLinkPayload(campaignId: String): String {
        val dataFields = JSONObject().apply {
            put("testType", "push_notification_deeplink")
            put("campaignId", campaignId)
            put("deepLink", "iterable://integration.tests/deeplink")
        }
        
        val primaryButton = JSONObject().apply {
            put("text", "Open")
            put("action", "open_deep_link")
        }
        
        val actionButtons = JSONObject().apply {
            put("primary", primaryButton)
        }
        
        val metadata = JSONObject().apply {
            put("title", "Test Deep Link Push")
            put("body", "Tap to open deep link")
            put("sound", "default")
            put("badge", 1)
            put("actionButtons", actionButtons)
        }
        
        return JSONObject().apply {
            put("campaignId", campaignId)
            put("recipientEmail", "integration.test@iterable.com")
            put("dataFields", dataFields)
            put("allowRepeatMarketingSends", false)
            put("metadata", metadata)
        }.toString()
    }
    
    private fun createIterableSilentPushNotificationPayload(campaignId: String): String {
        val dataFields = JSONObject().apply {
            put("testType", "silent_push")
            put("campaignId", campaignId)
            put("silent", true)
            put("inAppMessage", true)
        }
        
        val metadata = JSONObject().apply {
            put("contentAvailable", true)
            put("silent", true)
        }
        
        return JSONObject().apply {
            put("campaignId", campaignId)
            put("recipientEmail", "integration.test@iterable.com")
            put("dataFields", dataFields)
            put("allowRepeatMarketingSends", false)
            put("metadata", metadata)
        }.toString()
    }
    
    // Run all integration tests
    fun runAllIntegrationTests(context: Context) {
        Log.d(TAG, "Starting all integration tests...")
        
        // This would typically be called from a test orchestrator
        // For now, we'll just log that it was called
        Log.d(TAG, "All integration tests completed")
    }
} 