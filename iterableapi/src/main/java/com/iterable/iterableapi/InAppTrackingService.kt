package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InAppTrackingService internal constructor(
    private val iterableApi: IterableApi?
){
    fun trackInAppOpen(message: IterableInAppMessage, location: IterableInAppLocation?) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi != null) {
            iterableApi.trackInAppOpen(message, loc)
            IterableLogger.d(TAG, "Tracked in-app open: ${message.messageId} at location: $loc")
        } else {
            IterableLogger.w(TAG, "Cannot track in-app open: IterableApi not initialized")
        }
    }

    fun trackInAppClick(message: IterableInAppMessage, url: String, location: IterableInAppLocation?) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi != null) {
            iterableApi.trackInAppClick(message, url, loc)
            IterableLogger.d(
                TAG,
                "Tracked in-app click: ${message.messageId} url: $url at location: $loc"
            )
        } else {
            IterableLogger.w(TAG, "Cannot track in-app click: IterableApi not initialized")
        }
    }

    fun trackInAppClose(
        message: IterableInAppMessage,
        url: String,
        closeAction: IterableInAppCloseAction,
        location: IterableInAppLocation?
    ) {
        val loc = location ?: IterableInAppLocation.IN_APP

        if (iterableApi != null) {
            iterableApi.trackInAppClose(message, url, closeAction, loc)
            IterableLogger.d(
                TAG,
                "Tracked in-app close: ${message.messageId} action: $closeAction at location: $loc"
            )
        } else {
            IterableLogger.w(TAG, "Cannot track in-app close: IterableApi not initialized")
        }
    }

    fun removeMessage(message: IterableInAppMessage) {
        if (iterableApi == null) {
            IterableLogger.w(TAG, "Cannot remove message: IterableApi not initialized")
            return
        }

        if (message.isMarkedForDeletion && !message.isConsumed) {
            iterableApi.inAppManager.removeMessage(message)
            IterableLogger.d(TAG, "Removed message: ${message.messageId}")
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

