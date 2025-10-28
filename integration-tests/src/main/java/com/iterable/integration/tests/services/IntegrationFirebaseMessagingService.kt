package com.iterable.integration.tests.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.iterable.integration.tests.utils.IntegrationTestUtils

class IntegrationFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "IntegrationFCMService"
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Register the token with Iterable SDK
        try {
            com.iterable.iterableapi.IterableApi.getInstance().registerForPush()
            Log.d(TAG, "FCM token registered with Iterable SDK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token with Iterable SDK", e)
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Received FCM message: ${remoteMessage.messageId}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")
        Log.d(TAG, "Message notification: ${remoteMessage.notification}")
        
        // Check if this is a silent push for in-app messages
        val isSilent = remoteMessage.data["silent"] == "true"
        val isInAppMessage = remoteMessage.data["inAppMessage"] == "true"
        
        if (isSilent && isInAppMessage) {
            Log.d(TAG, "Received silent push for in-app message")
            // The Iterable SDK will handle the silent push automatically
            // We just need to track that it was received
            IntegrationTestUtils(this).setSilentPushProcessed(true)
        } else {
            Log.d(TAG, "Received regular push notification")
            // Regular push notification - Iterable SDK will handle display
            IntegrationTestUtils(this).setPushNotificationReceived(true)
        }
        
        // Let the Iterable SDK handle the message
        // The SDK will automatically process the message and display notifications
    }
    
    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "FCM message sent: $msgId")
    }
    
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "FCM send error for message $msgId", exception)
    }
} 