package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

class IterableInAppDisplayer extends IterableMessageDisplayer {

    IterableInAppDisplayer(IterableActivityMonitor activityMonitor) {
        super(activityMonitor);
    }

    boolean isShowingInApp() {
        return IterableInAppFragmentHTMLNotification.getInstance() != null;
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
    static boolean showIterableFragmentNotificationHTML(@NonNull Context context, @NonNull String htmlString, @NonNull String messageId, @NonNull final IterableHelper.IterableUrlCallback clickCallback, double backgroundAlpha,  @NonNull Rect padding, boolean shouldAnimate, IterableInAppMessage.InAppBgColor bgColor, boolean callbackOnCancel, @NonNull IterableInAppLocation location) {
        if (context instanceof FragmentActivity) {
            FragmentActivity currentActivity = (FragmentActivity) context;
            if (htmlString != null) {
                if (IterableInAppFragmentHTMLNotification.getInstance() != null) {
                    IterableLogger.w(IterableInAppManager.TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppFragmentHTMLNotification notification = IterableInAppFragmentHTMLNotification.createInstance(htmlString, callbackOnCancel, clickCallback, location, messageId, backgroundAlpha, padding, shouldAnimate, bgColor);
                notification.show(currentActivity.getSupportFragmentManager(), "iterable_in_app");
                return true;
            }
        } else {
            IterableLogger.w(IterableInAppManager.TAG, "To display in-app notifications, the context must be of an instance of: FragmentActivity");
        }
        return false;
    }


}
