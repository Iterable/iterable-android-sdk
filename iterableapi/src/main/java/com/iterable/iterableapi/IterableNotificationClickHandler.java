package com.iterable.iterableapi;

import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * Handler interface for push notification click events.
 * Register this handler through {@link IterableConfig.Builder#setNotificationClickHandler}
 * to be notified when a user taps on a push notification.
 */
public interface IterableNotificationClickHandler {

    /**
     * Called when a push notification is clicked by the user.
     *
     * @param notificationData The parsed notification metadata (campaign ID, template ID, message ID, etc.)
     * @param extras           The full Intent extras Bundle including the raw Iterable JSON payload
     *                         under the key {@link IterableConstants#ITERABLE_DATA_KEY}
     */
    void onNotificationClicked(@NonNull IterableNotificationData notificationData, @NonNull Bundle extras);
}
