package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

class IterableInAppDisplayer {

    private final IterableActivityMonitor activityMonitor;

    IterableInAppDisplayer(IterableActivityMonitor activityMonitor) {
        this.activityMonitor = activityMonitor;
    }

    boolean isShowingInApp() {
        return IterableInAppFragmentHTMLNotification.getInstance() != null;
    }

    boolean showMessage(@NonNull IterableInAppMessage message, IterableInAppLocation location, @NonNull final IterableHelper.IterableUrlCallback clickCallback) {
        Activity currentActivity = activityMonitor.getCurrentActivity();
        // Prevent double display
        if (currentActivity != null) {
            return IterableInAppDisplayer.showIterableFragmentNotificationHTML(currentActivity,
                    message.getContent().html,
                    message.getMessageId(),
                    clickCallback,
                    message.getContent().backgroundAlpha,
                    message.getContent().padding,
                    true, location);
        }
        return false;
    }

    /**
     * Displays an html rendered InApp Notification
     * @param context
     * @param htmlString
     * @param messageId
     * @param clickCallback
     * @param backgroundAlpha
     * @param padding
     */
    static boolean showIterableFragmentNotificationHTML(Context context, String htmlString, String messageId, final IterableHelper.IterableUrlCallback clickCallback, double backgroundAlpha, Rect padding, boolean callbackOnCancel, IterableInAppLocation location) {
        if (context instanceof FragmentActivity) {
            FragmentActivity currentActivity = (FragmentActivity) context;
            if (htmlString != null) {
                if (IterableInAppFragmentHTMLNotification.getInstance() != null) {
                    IterableLogger.w(IterableInAppManager.TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppFragmentHTMLNotification notification = IterableInAppFragmentHTMLNotification.createInstance(htmlString, callbackOnCancel, clickCallback, location, messageId, backgroundAlpha, padding);
                notification.show(currentActivity.getSupportFragmentManager(), "iterable_in_app");
                return true;
            }
        } else {
            IterableLogger.w(IterableInAppManager.TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
        return false;
    }


}
