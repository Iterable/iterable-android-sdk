package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Service class for in-app message event tracking.
 * Centralizes tracking logic shared between Fragment and Dialog implementations.
 * Provides null-safe wrappers around IterableApi tracking methods.
 */
class InAppTrackingService {

    private static final String TAG = "InAppTrackingService";

    /**
     * Tracks when an in-app message is opened
     * @param messageId The message ID
     * @param location The location where the message was triggered (nullable, defaults to IN_APP)
     */
    public void trackInAppOpen(@NonNull String messageId, @Nullable IterableInAppLocation location) {
        IterableInAppLocation loc = location != null ? location : IterableInAppLocation.IN_APP;
        
        IterableApi api = IterableApi.sharedInstance;
        if (api != null) {
            api.trackInAppOpen(messageId, loc);
            IterableLogger.d(TAG, "Tracked in-app open: " + messageId + " at location: " + loc);
        } else {
            IterableLogger.w(TAG, "Cannot track in-app open: IterableApi not initialized");
        }
    }

    /**
     * Tracks when a user clicks on an in-app message
     * @param messageId The message ID
     * @param url The URL that was clicked (or special identifier like itbl://backButton)
     * @param location The location where the click occurred (nullable, defaults to IN_APP)
     */
    public void trackInAppClick(@NonNull String messageId, @NonNull String url, @Nullable IterableInAppLocation location) {
        IterableInAppLocation loc = location != null ? location : IterableInAppLocation.IN_APP;
        
        IterableApi api = IterableApi.sharedInstance;
        if (api != null) {
            api.trackInAppClick(messageId, url, loc);
            IterableLogger.d(TAG, "Tracked in-app click: " + messageId + " url: " + url + " at location: " + loc);
        } else {
            IterableLogger.w(TAG, "Cannot track in-app click: IterableApi not initialized");
        }
    }

    /**
     * Tracks when an in-app message is closed
     * @param messageId The message ID
     * @param url The URL associated with the close action (or special identifier)
     * @param closeAction The type of close action (LINK, BACK, etc.)
     * @param location The location where the close occurred (nullable, defaults to IN_APP)
     */
    public void trackInAppClose(@NonNull String messageId, @NonNull String url, @NonNull IterableInAppCloseAction closeAction, @Nullable IterableInAppLocation location) {
        IterableInAppLocation loc = location != null ? location : IterableInAppLocation.IN_APP;
        
        IterableApi api = IterableApi.sharedInstance;
        if (api != null) {
            api.trackInAppClose(messageId, url, closeAction, loc);
            IterableLogger.d(TAG, "Tracked in-app close: " + messageId + " action: " + closeAction + " at location: " + loc);
        } else {
            IterableLogger.w(TAG, "Cannot track in-app close: IterableApi not initialized");
        }
    }

    /**
     * Removes a message from the in-app queue after it has been displayed or dismissed
     * @param messageId The message ID to remove
     * @param location The location where the removal occurred (nullable, defaults to IN_APP)
     */
    public void removeMessage(@NonNull String messageId, @Nullable IterableInAppLocation location) {
        IterableInAppLocation loc = location != null ? location : IterableInAppLocation.IN_APP;
        
        IterableApi api = IterableApi.sharedInstance;
        if (api == null) {
            IterableLogger.w(TAG, "Cannot remove message: IterableApi not initialized");
            return;
        }

        IterableInAppManager inAppManager = api.getInAppManager();
        if (inAppManager == null) {
            IterableLogger.w(TAG, "Cannot remove message: InAppManager not available");
            return;
        }

        // Find the message by ID
        IterableInAppMessage message = null;
        if (inAppManager.getMessages() != null) {
            for (IterableInAppMessage msg : inAppManager.getMessages()) {
                if (msg != null && messageId.equals(msg.getMessageId())) {
                    message = msg;
                    break;
                }
            }
        }

        if (message != null) {
            // Remove with proper parameters (message, deleteType, location)
            inAppManager.removeMessage(
                message,
                IterableInAppDeleteActionType.INBOX_SWIPE,
                loc
            );
            IterableLogger.d(TAG, "Removed message: " + messageId + " at location: " + loc);
        } else {
            IterableLogger.w(TAG, "Message not found for removal: " + messageId);
        }
    }

    /**
     * Tracks a screen view event (useful for analytics)
     * @param screenName The name of the screen being viewed
     */
    public void trackScreenView(@NonNull String screenName) {
        IterableApi api = IterableApi.sharedInstance;
        if (api != null) {
            try {
                org.json.JSONObject data = new org.json.JSONObject();
                data.put("screenName", screenName);
                api.track("Screen Viewed", data);
                IterableLogger.d(TAG, "Tracked screen view: " + screenName);
            } catch (org.json.JSONException e) {
                IterableLogger.w(TAG, "Failed to track screen view", e);
            }
        }
    }
}

