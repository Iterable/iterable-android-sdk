package com.iterable.iterableapi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class IterableFirebaseMessagingService extends FirebaseMessagingService {

    static final String TAG = "itblFCMMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Map<String,String> messageData = remoteMessage.getData();
            handlePushReceived(messageData);
            IterableLogger.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            IterableLogger.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    /**
     * Handles receiving an incoming push notification from the intent.
     * @param messageData
     */
    private void handlePushReceived(Map<String,String> messageData) {
        Bundle extras = new Bundle();
        for (Map.Entry<String, String> entry : messageData.entrySet()) {
            extras.putString(entry.getKey(), entry.getValue());
        }

        if (!IterableNotificationBuilder.isGhostPush(extras)) {
            if (!IterableNotificationBuilder.isEmptyBody(extras)) {
                IterableLogger.d(TAG, "Iterable push received " + messageData);
                IterableNotificationBuilder notificationBuilder = IterableNotificationBuilder.createNotification(
                        getApplicationContext(), extras);
                new IterableNotificationManager().execute(notificationBuilder);
            } else {
                IterableLogger.d(TAG, "Iterable OS notification push received");
            }
        } else {
            IterableLogger.d(TAG, "Iterable ghost silent push received");
        }
    }
}


class IterableNotificationManager extends AsyncTask<IterableNotificationBuilder, Void, Void> {

    @Override
    protected Void doInBackground(IterableNotificationBuilder... params) {
        if ( params != null && params[0] != null) {
            IterableNotificationBuilder notificationBuilder = params[0];
            IterableNotificationBuilder.postNotificationOnDevice(notificationBuilder.mContext, notificationBuilder);
        }
        return null;
    }
}