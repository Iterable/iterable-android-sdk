package com.iterable.iterableapi

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import androidx.annotation.NonNull
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.ExecutionException

class IterableFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        handleMessageReceived(this, remoteMessage)
    }

    override fun onNewToken(s: String) {
        handleTokenRefresh()
    }

    companion object {
        const val TAG = "itblFCMMessagingService"

        /**
         * Handles receiving an incoming push notification from the intent.
         *
         * Call this from a custom [FirebaseMessagingService] to pass Iterable push messages to
         * Iterable SDK for tracking and rendering
         * @param remoteMessage Remote message received from Firebase in
         *        [FirebaseMessagingService.onMessageReceived]
         * @return Boolean indicating whether it was an Iterable message or not
         */
        @JvmStatic
        fun handleMessageReceived(@NonNull context: Context, @NonNull remoteMessage: RemoteMessage): Boolean {
            val messageData = remoteMessage.data

            if (messageData.isEmpty()) {
                return false
            }

            IterableLogger.d(TAG, "Message data payload: " + remoteMessage.data)
            // Check if message contains a notification payload.
            if (remoteMessage.notification != null) {
                IterableLogger.d(TAG, "Message Notification Body: " + remoteMessage.notification!!.body)
            }

            val extras = IterableNotificationHelper.mapToBundle(messageData)

            if (!IterableNotificationHelper.isIterablePush(extras)) {
                IterableLogger.d(TAG, "Not an Iterable push message")
                return false
            }

            if (!IterableNotificationHelper.isGhostPush(extras)) {
                if (!IterableNotificationHelper.isEmptyBody(extras)) {
                    IterableLogger.d(TAG, "Iterable push received $messageData")
                    val notificationBuilder = IterableNotificationHelper.createNotification(
                        context.applicationContext, extras
                    )
                    IterableNotificationManager().execute(notificationBuilder)
                } else {
                    IterableLogger.d(TAG, "Iterable OS notification push received")
                }
            } else {
                IterableLogger.d(TAG, "Iterable ghost silent push received")

                val notificationType = extras.getString("notificationType")
                if (notificationType != null && IterableApi.getInstance().mainActivityContext != null) {
                    when (notificationType) {
                        "InAppUpdate" -> {
                            IterableApi.getInstance().inAppManager?.syncInApp()
                        }
                        "InAppRemove" -> {
                            val messageId = extras.getString("messageId")
                            if (messageId != null) {
                                IterableApi.getInstance().inAppManager?.removeMessage(messageId)
                            }
                        }
                        "UpdateEmbedded" -> {
                            IterableApi.getInstance().embeddedManager?.syncMessages()
                        }
                    }
                }
            }
            return true
        }

        /**
         * Handles token refresh
         * Call this from a custom [FirebaseMessagingService] to register the new token with Iterable
         */
        @JvmStatic
        fun handleTokenRefresh() {
            val registrationToken = getFirebaseToken()
            IterableLogger.d(TAG, "New Firebase Token generated: $registrationToken")
            IterableApi.getInstance().registerForPush()
        }

        @JvmStatic
        fun getFirebaseToken(): String? {
            var registrationToken: String? = null
            try {
                registrationToken = Tasks.await(FirebaseMessaging.getInstance().token)
            } catch (e: ExecutionException) {
                IterableLogger.e(TAG, e.localizedMessage)
            } catch (e: InterruptedException) {
                IterableLogger.e(TAG, e.localizedMessage)
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Failed to fetch firebase token")
            }
            return registrationToken
        }

        /**
         * Checks if the message is an Iterable ghost push or silent push message
         * @param remoteMessage Remote message received from Firebase in
         *        [FirebaseMessagingService.onMessageReceived]
         * @return Boolean indicating whether the message is an Iterable ghost push or silent push
         */
        @JvmStatic
        fun isGhostPush(remoteMessage: RemoteMessage): Boolean {
            val messageData = remoteMessage.data

            if (messageData.isEmpty()) {
                return false
            }

            val extras = IterableNotificationHelper.mapToBundle(messageData)
            return IterableNotificationHelper.isGhostPush(extras)
        }
    }
}

internal class IterableNotificationManager : AsyncTask<IterableNotificationBuilder?, Void?, Void?>() {

    override fun doInBackground(vararg params: IterableNotificationBuilder?): Void? {
        if (params.isNotEmpty() && params[0] != null) {
            val notificationBuilder = params[0]!!
            IterableNotificationHelper.postNotificationOnDevice(notificationBuilder.context, notificationBuilder)
        }
        return null
    }
}