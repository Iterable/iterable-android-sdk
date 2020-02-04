package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.support.v4.app.FragmentActivity;

class IterableInAppDisplayer {

    private final IterableActivityMonitor activityMonitor;

    IterableInAppDisplayer(IterableActivityMonitor activityMonitor) {
        this.activityMonitor = activityMonitor;
    }

    boolean isShowingInApp() {
        return IterableInAppHTMLNotification.getInstance() != null;
    }

    boolean showMessage(IterableInAppMessage message, IterableInAppLocation location, final IterableHelper.IterableUrlCallback clickCallback) {
        Activity currentActivity = activityMonitor.getCurrentActivity();
        // Prevent double display
        if (currentActivity != null) {
            return IterableInAppDisplayer.showIterableNotificationHTML(currentActivity,
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
    static boolean showIterableNotificationHTML(Context context, String htmlString, String messageId, final IterableHelper.IterableUrlCallback clickCallback, double backgroundAlpha, Rect padding, boolean callbackOnCancel, IterableInAppLocation location) {
        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            if (htmlString != null) {
                if (IterableInAppHTMLNotification.getInstance() != null) {
                    IterableLogger.w(IterableInAppManager.TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.createInstance(context, htmlString);
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setOwnerActivity(currentActivity);
                notification.setLocation(location);

                if (callbackOnCancel) {
                    notification.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            clickCallback.execute(null);
                        }
                    });
                }

                notification.show();
                return true;
            }
        } else {
            IterableLogger.w(IterableInAppManager.TAG, "To display in-app notifications, the context must be of an instance of: Activity");
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

                IterableInAppFragmentHTMLNotification notification = IterableInAppFragmentHTMLNotification.createInstance(context, htmlString, callbackOnCancel);
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setLocation(location);

                notification.show(currentActivity.getSupportFragmentManager(), "iterable_in_app");
                return true;
            }
        } else {
            IterableLogger.w(IterableInAppManager.TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
        return false;
    }


}
