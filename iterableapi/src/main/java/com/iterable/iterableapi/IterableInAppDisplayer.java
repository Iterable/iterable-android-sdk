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
        return IterableInAppFragmentHTMLNotification.getInstance() != null ||
               IterableInAppDialogNotification.getInstance() != null;
    }

    boolean showMessage(@NonNull IterableInAppMessage message, IterableInAppLocation location, @NonNull final IterableHelper.IterableUrlCallback clickCallback) {
        // Early return for JSON-only messages
        if (message.isJsonOnly()) {
            return false;
        }

        Activity currentActivity = activityMonitor.getCurrentActivity();
        if (currentActivity != null) {
            // Try FragmentActivity path first (backward compatibility)
            if (currentActivity instanceof FragmentActivity) {
                return showIterableFragmentNotificationHTML(currentActivity,
                        message.getContent().html,
                        message.getMessageId(),
                        clickCallback,
                        message.getContent().backgroundAlpha,
                        message.getContent().padding,
                        message.getContent().inAppDisplaySettings.shouldAnimate,
                        message.getContent().inAppDisplaySettings.inAppBgColor,
                        true, location);
            } 
            // Fall back to Dialog path for ComponentActivity (Compose support)
            else {
                return showIterableDialogNotificationHTML(currentActivity,
                        message.getContent().html,
                        message.getMessageId(),
                        clickCallback,
                        message.getContent().backgroundAlpha,
                        message.getContent().padding,
                        message.getContent().inAppDisplaySettings.shouldAnimate,
                        message.getContent().inAppDisplaySettings.inAppBgColor,
                        true, location);
            }
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
    static boolean showIterableFragmentNotificationHTML(@NonNull Context context, @NonNull String htmlString, @NonNull String messageId, @NonNull final IterableHelper.IterableUrlCallback clickCallback, double backgroundAlpha,  @NonNull Rect padding, boolean shouldAnimate, IterableInAppMessage.InAppBgColor bgColor, boolean callbackOnCancel, @NonNull IterableInAppLocation location) {
        if (context instanceof FragmentActivity currentActivity) {
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
            IterableLogger.w(IterableInAppManager.TAG, "Received context that is not FragmentActivity. Attempting dialog-based display.");
        }
        return false;
    }

    /**
     * Displays an HTML rendered InApp Notification using Dialog (for ComponentActivity/Compose support)
     * @param context
     * @param htmlString
     * @param messageId
     * @param clickCallback
     * @param backgroundAlpha
     * @param padding
     * @param shouldAnimate
     * @param bgColor
     * @param callbackOnCancel
     * @param location
     */
    static boolean showIterableDialogNotificationHTML(@NonNull Context context, @NonNull String htmlString, @NonNull String messageId, @NonNull final IterableHelper.IterableUrlCallback clickCallback, double backgroundAlpha, @NonNull Rect padding, boolean shouldAnimate, IterableInAppMessage.InAppBgColor bgColor, boolean callbackOnCancel, @NonNull IterableInAppLocation location) {
        if (!(context instanceof Activity)) {
            IterableLogger.w(IterableInAppManager.TAG, "To display in-app notifications, the context must be an Activity");
            return false;
        }
        
        Activity activity = (Activity) context;
        
        if (htmlString == null) {
            IterableLogger.w(IterableInAppManager.TAG, "HTML string is null");
            return false;
        }
        
        // Check if already showing
        if (IterableInAppDialogNotification.getInstance() != null) {
            IterableLogger.w(IterableInAppManager.TAG, "Skipping the in-app notification: another notification is already being displayed");
            return false;
        }
        
        // Create and show dialog (Kotlin interop)
        IterableInAppDialogNotification dialog = IterableInAppDialogNotification.createInstance(
            activity, htmlString, callbackOnCancel, clickCallback, location, 
            messageId, backgroundAlpha, padding, shouldAnimate, bgColor
        );
        dialog.show();
        
        IterableLogger.d(IterableInAppManager.TAG, "Displaying in-app notification via Dialog for ComponentActivity");
        
        return true;
    }

}
