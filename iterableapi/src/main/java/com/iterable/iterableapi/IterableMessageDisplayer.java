package com.iterable.iterableapi;

import android.app.Activity;

import androidx.annotation.NonNull;

import org.json.JSONObject;

class IterableMessageDisplayer {
    private final IterableActivityMonitor activityMonitor;

    IterableMessageDisplayer(IterableActivityMonitor activityMonitor) {
        this.activityMonitor = activityMonitor;
    }

    boolean showMessage(@NonNull IterableInAppMessage message, IterableInAppLocation location, @NonNull final IterableHelper.IterableUrlCallback clickCallback) {
        Activity currentActivity = activityMonitor.getCurrentActivity();
        JSONObject messageCustomPayload = message.getCustomPayload();
        String messageHtml = message.getContent().html;

        // JSON-only web in-app messages have a payload but no html
        if (messageCustomPayload.length() > 0 && messageHtml.length() == 0) {
            return IterableRenderJsonDisplayer.showMessage(messageCustomPayload, clickCallback);
        }

        // Prevent double display
        if (currentActivity != null) {
            return IterableInAppDisplayer.showIterableFragmentNotificationHTML(currentActivity,
                    messageHtml,
                    message.getMessageId(),
                    clickCallback,
                    message.getContent().backgroundAlpha,
                    message.getContent().padding,
                    message.getContent().inAppDisplaySettings.shouldAnimate,
                    message.getContent().inAppDisplaySettings.inAppBgColor,
                    true, location);
        }
        return false;
    }
}
