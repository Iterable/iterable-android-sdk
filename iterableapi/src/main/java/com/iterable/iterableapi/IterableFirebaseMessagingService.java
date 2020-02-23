package com.iterable.iterableapi;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class IterableFirebaseMessagingService extends FirebaseMessagingService {

    static final String TAG = "itblFCMMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        handleMessageReceived(this, remoteMessage);
    }

    @Override
    public void onNewToken(String s) {
        handleTokenRefresh();
    }

    /**
     * Handles receiving an incoming push notification from the intent.
     *
     * Call this from a custom {@link FirebaseMessagingService} to pass Iterable push messages to
     * Iterable SDK for tracking and rendering
     * @param remoteMessage Remote message received from Firebase in
     *        {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
     * @return Boolean indicating whether it was an Iterable message or not
     */
    public static boolean handleMessageReceived(@NonNull Context context, @NonNull RemoteMessage remoteMessage) {
        Map<String,String> messageData = remoteMessage.getData();

        if (messageData == null || messageData.size() == 0) {
            return false;
        }

        IterableLogger.d(TAG, "Message data payload: " + remoteMessage.getData());
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            IterableLogger.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        Bundle extras = IterableNotificationHelper.mapToBundle(messageData);

        if (!IterableNotificationHelper.isIterablePush(extras)) {
            IterableLogger.d(TAG, "Not an Iterable push message");
            return false;
        }

        if (!IterableNotificationHelper.isGhostPush(extras)) {
            if (!IterableNotificationHelper.isEmptyBody(extras)) {
                IterableLogger.d(TAG, "Iterable push received " + messageData);
                IterableNotificationBuilder notificationBuilder = IterableNotificationHelper.createNotification(
                        context.getApplicationContext(), extras);
                new IterableNotificationManager().execute(notificationBuilder);
            } else {
                IterableLogger.d(TAG, "Iterable OS notification push received");
            }
        } else {
            IterableLogger.d(TAG, "Iterable ghost silent push received");

            String notificationType = extras.getString("notificationType");
            if (notificationType != null) {
                if (notificationType.equals("InAppUpdate")) {
                    IterableApi.getInstance().getInAppManager().syncInApp();
                } else if (notificationType.equals("InAppRemove")) {
                    String messageId = extras.getString("messageId");
                    if (messageId != null) {
                        IterableApi.getInstance().getInAppManager().removeMessage(messageId);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Handles token refresh
     * Call this from a custom {@link FirebaseMessagingService} to register the new token with Iterable
     */
    public static void handleTokenRefresh() {
        String registrationToken = FirebaseInstanceId.getInstance().getToken();
        IterableLogger.d(TAG, "New Firebase Token generated: " + registrationToken);
        IterableApi.getInstance().registerForPush();
    }

    /**
     * Checks if the message is an Iterable ghost push or silent push message
     * @param remoteMessage Remote message received from Firebase in
     *        {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
     * @return Boolean indicating whether the message is an Iterable ghost push or silent push
     */
    public static boolean isGhostPush(RemoteMessage remoteMessage) {
        Map<String,String> messageData = remoteMessage.getData();

        if (messageData == null || messageData.isEmpty()) {
            return false;
        }

        Bundle extras = IterableNotificationHelper.mapToBundle(messageData);
        return IterableNotificationHelper.isGhostPush(extras);
    }
}


class IterableNotificationManager extends AsyncTask<IterableNotificationBuilder, Void, Void> {

    @Override
    protected Void doInBackground(IterableNotificationBuilder... params) {
        if ( params != null && params[0] != null) {
            IterableNotificationBuilder notificationBuilder = params[0];
            IterableNotificationHelper.postNotificationOnDevice(notificationBuilder.context, notificationBuilder);
        }
        return null;
    }
}