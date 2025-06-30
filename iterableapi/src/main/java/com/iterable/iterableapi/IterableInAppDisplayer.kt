package com.iterable.iterableapi

import android.app.Activity
import android.content.Context
import android.graphics.Rect

import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity

internal class IterableInAppDisplayer(
    private val activityMonitor: IterableActivityMonitor
) {

    fun isShowingInApp(): Boolean {
        return IterableInAppFragmentHTMLNotification.getInstance() != null
    }

    fun showMessage(@NonNull message: IterableInAppMessage, location: IterableInAppLocation?, @NonNull clickCallback: IterableHelper.IterableUrlCallback): Boolean {
        // Early return for JSON-only messages
        if (message.isJsonOnly()) {
            return false
        }

        val currentActivity = activityMonitor.getCurrentActivity()
        // Prevent double display
        if (currentActivity != null) {
            return showIterableFragmentNotificationHTML(currentActivity,
                    message.content.html ?: "",
                    message.messageId,
                    clickCallback,
                    message.content.backgroundAlpha,
                    message.content.padding,
                    message.content.inAppDisplaySettings.shouldAnimate,
                    message.content.inAppDisplaySettings.inAppBgColor ?: IterableInAppMessage.InAppBgColor(null, 0.0),
                    true, location ?: IterableInAppLocation.IN_APP)
        }
        return false
    }

    companion object {
        /**
         * Displays an html rendered InApp Notification
         * @param context
         * @param htmlString
         * @param messageId
         * @param clickCallback
         * @param backgroundAlpha
         * @param padding
         */
        @JvmStatic
        fun showIterableFragmentNotificationHTML(@NonNull context: Context, @NonNull htmlString: String, @NonNull messageId: String, @NonNull clickCallback: IterableHelper.IterableUrlCallback, backgroundAlpha: Double, @NonNull padding: Rect, shouldAnimate: Boolean, bgColor: IterableInAppMessage.InAppBgColor?, callbackOnCancel: Boolean, location: IterableInAppLocation?): Boolean {
            if (context is FragmentActivity) {
                val currentActivity = context as FragmentActivity
                if (htmlString.isNotEmpty()) {
                    if (IterableInAppFragmentHTMLNotification.getInstance() != null) {
                        IterableLogger.w(IterableInAppManager.TAG, "Skipping the in-app notification: another notification is already being displayed")
                        return false
                    }

                    val notification = IterableInAppFragmentHTMLNotification.createInstance(htmlString, callbackOnCancel, clickCallback, location ?: IterableInAppLocation.IN_APP, messageId, backgroundAlpha, padding, shouldAnimate, bgColor ?: IterableInAppMessage.InAppBgColor(null, 0.0))
                    notification.show(currentActivity.supportFragmentManager, "iterable_in_app")
                    return true
                }
            } else {
                IterableLogger.w(IterableInAppManager.TAG, "To display in-app notifications, the context must be of an instance of: FragmentActivity")
            }
            return false
        }
    }


}
