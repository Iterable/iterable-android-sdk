package com.iterable.iterableapi;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class IterableFirebaseMessagingService extends FirebaseMessagingService {

    static final String TAG = "itblFCMMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        handleMessageReceived(this, remoteMessage);
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
    public static boolean handleMessageReceived(Context context, RemoteMessage remoteMessage) {
        Map<String,String> messageData = remoteMessage.getData();

        if (messageData == null || messageData.size() == 0) {
            return false;
        }

        IterableLogger.d(TAG, "Message data payload: " + remoteMessage.getData());
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            IterableLogger.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        Bundle extras = new Bundle();
        for (Map.Entry<String, String> entry : messageData.entrySet()) {
            extras.putString(entry.getKey(), entry.getValue());
        }

        if (!IterableNotificationBuilder.isIterablePush(extras)) {
            IterableLogger.d(TAG, "Not an Iterable push message");
            return false;
        }

        if (!IterableNotificationBuilder.isGhostPush(extras)) {
            if (!IterableNotificationBuilder.isEmptyBody(extras)) {
                IterableLogger.d(TAG, "Iterable push received " + messageData);
                IterableNotificationBuilder notificationBuilder = IterableNotificationBuilder.createNotification(
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
}


class IterableNotificationManager extends AsyncTask<IterableNotificationBuilder, Void, Void> {

    @Override
    protected Void doInBackground(IterableNotificationBuilder... params) {
        if ( params != null && params[0] != null) {
            IterableNotificationBuilder notificationBuilder = params[0];
            IterableNotificationBuilder.postNotificationOnDevice(notificationBuilder.context, notificationBuilder);
        }
        return null;
    }
}