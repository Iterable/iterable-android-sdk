package com.iterable.iterableapi

import org.json.JSONException
import org.json.JSONObject

internal class InAppTrackingService internal constructor(
    private val iterableApi: IterableApi?
){
    fun trackInAppOpen(messageId: String, location: IterableInAppLocation?) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi != null) {
            iterableApi.trackInAppOpen(messageId, loc)
            IterableLogger.d(TAG, "Tracked in-app open: $messageId at location: $loc")
        } else {
            IterableLogger.w(TAG, "Cannot track in-app open: IterableApi not initialized")
        }
    }

    fun trackInAppClick(messageId: String, url: String, location: IterableInAppLocation?) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi != null) {
            iterableApi.trackInAppClick(messageId, url, loc)
            IterableLogger.d(
                TAG,
                "Tracked in-app click: $messageId url: $url at location: $loc"
            )
        } else {
            IterableLogger.w(TAG, "Cannot track in-app click: IterableApi not initialized")
        }
    }

    fun trackInAppClose(
        messageId: String,
        url: String,
        closeAction: IterableInAppCloseAction,
        location: IterableInAppLocation?
    ) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi != null) {
            iterableApi.trackInAppClose(messageId, url, closeAction, loc)
            IterableLogger.d(
                TAG,
                "Tracked in-app close: $messageId action: $closeAction at location: $loc"
            )
        } else {
            IterableLogger.w(TAG, "Cannot track in-app close: IterableApi not initialized")
        }
    }

    fun removeMessage(messageId: String, location: IterableInAppLocation?) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi == null) {
            IterableLogger.w(TAG, "Cannot remove message: IterableApi not initialized")
            return
        }

        val inAppManager = try {
            iterableApi.inAppManager
        } catch (e: Exception) {
            null
        }
        
        if (inAppManager == null) {
            IterableLogger.w(TAG, "Cannot remove message: InAppManager not initialized")
            return
        }

        val message: IterableInAppMessage? = try {
            inAppManager.messages.firstOrNull { msg ->
                msg != null && messageId == msg.messageId
            }
        } catch (e: Exception) {
            null
        }

        if (message != null) {
            inAppManager.removeMessage(
                message,
                IterableInAppDeleteActionType.INBOX_SWIPE,
                loc
            )
            IterableLogger.d(TAG, "Removed message: $messageId at location: $loc")
        } else {
            IterableLogger.w(TAG, "Message not found for removal: $messageId")
        }
    }

    fun trackScreenView(screenName: String) {
        if (iterableApi != null) {
            try {
                val data = JSONObject()
                data.put("screenName", screenName)
                iterableApi.track("Screen Viewed", data)
                IterableLogger.d(TAG, "Tracked screen view: $screenName")
            } catch (e: JSONException) {
                IterableLogger.w(TAG, "Failed to track screen view", e)
            }
        }
    }

    companion object {
        private const val TAG = "InAppTrackingService"
    }
}

