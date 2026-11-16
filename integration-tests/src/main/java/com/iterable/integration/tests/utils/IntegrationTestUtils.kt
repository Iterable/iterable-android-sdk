package com.iterable.integration.tests.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.integration.tests.TestConstants
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class IntegrationTestUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "IntegrationTestUtils"
        // Relative to BcitProject.serverBaseUrl, which carries the project's region.
        private const val ITERABLE_SEND_PUSH_ENDPOINT = "push/target"
        private const val ITERABLE_INAPP_TARGET_ENDPOINT = "inApp/target"
        private const val ITERABLE_GET_USER_ENDPOINT = "users/getByEmail"
        private const val ITERABLE_UPDATE_USER_ENDPOINT = "users/update"
    }
    
    private val httpClient = OkHttpClient()
    
    // Test state tracking
    private val pushNotificationReceived = AtomicBoolean(false)
    private val inAppMessageDisplayed = AtomicBoolean(false)
    private val embeddedMessageDisplayed = AtomicBoolean(false)
    private val metricsSent = AtomicBoolean(false)
    private val trackPushOpenCalled = AtomicBoolean(false)
    private val deepLinkHandlerInvoked = AtomicBoolean(false)
    private val silentPushProcessed = AtomicBoolean(false)
    private val embeddedPushProcessed = AtomicBoolean(false)
    
    // Deep link tracking
    private var lastDeepLinkUrl: String? = null
    private var lastDeepLinkPath: String? = null
    private val deepLinkReceived = AtomicBoolean(false)
    
    // Error tracking
    private var lastErrorMessage: String? = null
    
    // Reset all test states
    fun resetTestStates() {
        pushNotificationReceived.set(false)
        inAppMessageDisplayed.set(false)
        embeddedMessageDisplayed.set(false)
        metricsSent.set(false)
        trackPushOpenCalled.set(false)
        deepLinkHandlerInvoked.set(false)
        silentPushProcessed.set(false)
        embeddedPushProcessed.set(false)
        lastErrorMessage = null
        resetDeepLinkTracking()
    }
    
    // Deep link tracking methods
    fun resetDeepLinkTracking() {
        deepLinkReceived.set(false)
        lastDeepLinkUrl = null
        lastDeepLinkPath = null
    }
    
    fun setDeepLinkReceived(url: String?, path: String?) {
        deepLinkReceived.set(true)
        lastDeepLinkUrl = url
        lastDeepLinkPath = path
        Log.d(TAG, "Deep link received - URL: $url, Path: $path")
    }
    
    fun isDeepLinkReceived(): Boolean = deepLinkReceived.get()
    
    fun getLastDeepLinkUrl(): String? = lastDeepLinkUrl
    
    fun getLastDeepLinkPath(): String? = lastDeepLinkPath
    
    // Get last error message
    fun getLastErrorMessage(): String? {
        return lastErrorMessage
    }
    
    // User management methods
    fun ensureUserSignedIn(email: String = TestConstants.TEST_USER_EMAIL): Boolean {
        return try {
            val currentEmail = com.iterable.iterableapi.IterableApi.getInstance().getEmail()
            if (currentEmail != email) {
                com.iterable.iterableapi.IterableApi.getInstance().setEmail(email)
                Log.d(TAG, "User signed in with email: ${maskEmail(email)}")
            } else {
                Log.d(TAG, "User already signed in with email: ${maskEmail(currentEmail)}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in user", e)
            false
        }
    }

    fun setPushNotificationReceived(received: Boolean) {
        pushNotificationReceived.set(received)
    }
    
    fun setSilentPushProcessed(processed: Boolean) {
        silentPushProcessed.set(processed)
    }
    
    fun isSilentPushProcessed(): Boolean {
        return silentPushProcessed.get()
    }
    
    fun setEmbeddedPushProcessed(processed: Boolean) {
        embeddedPushProcessed.set(processed)
    }
    
    fun isEmbeddedPushProcessed(): Boolean {
        return embeddedPushProcessed.get()
    }

    fun setInAppMessageDisplayed(displayed: Boolean) {
        inAppMessageDisplayed.set(displayed)
    }

    // Campaign Triggering Methods - Using Server-Side API
    fun triggerCampaignViaAPI(campaignId: Int, recipientEmail: String = TestConstants.TEST_USER_EMAIL, dataFields: Map<String, Any>? = null, project: BcitProject = BcitProjects.A, callback: ((Boolean) -> Unit)? = null) {
        // Execute on background thread to avoid NetworkOnMainThreadException
        Thread {
            try {
                val payload = createCampaignTriggerPayload(campaignId, recipientEmail, dataFields)
                
                val request = Request.Builder()
                    .url("${project.serverBaseUrl}$ITERABLE_INAPP_TARGET_ENDPOINT")
                    .addHeader("Api-Key", project.serverApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val success = response.isSuccessful
                
                            if (success) {
                Log.d(TAG, "Campaign triggered successfully via API: campaignId=$campaignId, recipientEmail=$recipientEmail")
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "Failed to trigger campaign via API: ${response.code} - $errorBody")
                // Store error message for UI display
                lastErrorMessage = "HTTP ${response.code}: $errorBody"
            }
                
                callback?.invoke(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering campaign via API", e)
                callback?.invoke(false)
            }
        }.start()
    }
    
    fun triggerPushCampaignViaAPI(campaignId: Int, recipientEmail: String = TestConstants.TEST_USER_EMAIL, dataFields: Map<String, Any>? = null, project: BcitProject = BcitProjects.A, callback: ((Boolean) -> Unit)? = null) {
        // Execute on background thread to avoid NetworkOnMainThreadException
        Thread {
            try {
                val payload = createPushCampaignTriggerPayload(campaignId, recipientEmail, dataFields)
                
                val request = Request.Builder()
                    .url("${project.serverBaseUrl}$ITERABLE_SEND_PUSH_ENDPOINT")
                    .addHeader("Api-Key", project.serverApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val success = response.isSuccessful
                
                            if (success) {
                Log.d(TAG, "Push campaign triggered successfully via API: campaignId=$campaignId, recipientEmail=$recipientEmail")
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "Failed to trigger push campaign via API: ${response.code} - $errorBody")
                // Store error message for UI display
                lastErrorMessage = "HTTP ${response.code}: $errorBody"
            }
                //TODO: Move callback success inside if(success)
                callback?.invoke(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering push campaign via API", e)
                callback?.invoke(false)
            }
        }.start()
    }

    /**
     * Creates [email] in [project] with the project's server key unless it already exists there.
     * Blocking, so call it off the main thread.
     */
    fun ensureUserExists(email: String, project: BcitProject): Boolean {
        if (project.serverApiKey.isBlank()) {
            Log.w(TAG, "No server key for project ${project.label}, cannot ensure ${maskEmail(email)} exists")
            return false
        }
        return try {
            val lookup = Request.Builder()
                .url("${project.serverBaseUrl}$ITERABLE_GET_USER_ENDPOINT".toHttpUrl().newBuilder()
                    .addQueryParameter("email", email)
                    .build())
                .addHeader("Api-Key", project.serverApiKey)
                .get()
                .build()
            val exists = httpClient.newCall(lookup).execute().use { response ->
                if (!response.isSuccessful) {
                    lastErrorMessage = "HTTP ${response.code}: ${response.body?.string()}"
                    Log.e(TAG, "User lookup failed on project ${project.label}: $lastErrorMessage")
                    return false
                }
                JSONObject(response.body?.string().orEmpty().ifBlank { "{}" }).has("user")
            }
            if (exists) {
                Log.d(TAG, "User ${maskEmail(email)} already exists on project ${project.label}")
                return true
            }

            val payload = JSONObject().put("email", email).put("dataFields", JSONObject()).toString()
            val create = Request.Builder()
                .url("${project.serverBaseUrl}$ITERABLE_UPDATE_USER_ENDPOINT")
                .addHeader("Api-Key", project.serverApiKey)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(create).execute().use { response ->
                if (!response.isSuccessful) {
                    lastErrorMessage = "HTTP ${response.code}: ${response.body?.string()}"
                    Log.e(TAG, "User create failed on project ${project.label}: $lastErrorMessage")
                } else {
                    Log.d(TAG, "Created user ${maskEmail(email)} on project ${project.label}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring user exists on project ${project.label}", e)
            false
        }
    }
    
    // Campaign Payload Creation Methods
    private fun createCampaignTriggerPayload(campaignId: Int, recipientEmail: String, dataFields: Map<String, Any>?): String {
        val jsonObject = JSONObject().apply {
            put("campaignId", campaignId)
            put("recipientEmail", recipientEmail)
            put("recipientUserId", "100710667387222399979") // Using the user ID from your example
            
            if (dataFields != null) {
                val dataFieldsJson = JSONObject()
                dataFields.forEach { (key, value) ->
                    dataFieldsJson.put(key, value)
                }
                put("dataFields", dataFieldsJson)
            }
        }
        
        return jsonObject.toString()
    }
    
    private fun createPushCampaignTriggerPayload(campaignId: Int, recipientEmail: String, dataFields: Map<String, Any>?): String {
        val jsonObject = JSONObject().apply {
            put("campaignId", campaignId)
            put("recipientEmail", recipientEmail)
            put("recipientUserId", "100710667387222399979") // Using the user ID from your example
            
            if (dataFields != null) {
                val dataFieldsJson = JSONObject()
                dataFields.forEach { (key, value) ->
                    dataFieldsJson.put(key, value)
                }
                put("dataFields", dataFieldsJson)
            }
        }
        
        return jsonObject.toString()
    }
    
    // Run all integration tests
    fun runAllIntegrationTests(context: Context) {
        Log.d(TAG, "Starting all integration tests...")
        
        // This would typically be called from a test orchestrator
        // For now, we'll just log that it was called
        Log.d(TAG, "All integration tests completed")
    }
} 