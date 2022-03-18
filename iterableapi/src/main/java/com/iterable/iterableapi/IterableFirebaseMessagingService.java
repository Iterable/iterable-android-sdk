package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
     *                      {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
     * @return Boolean indicating whether it was an Iterable message or not
     */
    public static boolean handleMessageReceived(@NonNull Context context, @NonNull RemoteMessage remoteMessage) {
        Map<String, String> messageData = remoteMessage.getData();

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
                if (isDuplicateMessageId(context, notificationBuilder.iterableNotificationData.getMessageId())) {
                    IterableLogger.d(TAG, "Notification is duplicate .Proceed to call dedeup API");
                    IterableApi.getInstance().trackDuplicateSend(notificationBuilder.iterableNotificationData.getMessageId());
                    notificationBuilder = null;
                    return false;
                }
//                if(BuildConfig.DEBUG && notificationBuilder.iterableNotificationData.getCampaignId() == 0) {
//                    IterableLogger.d(TAG, "Marking Push proof as duplicates. Proofs still continues to be shown on device. It wont be treated and skipped as duplicates");
//                    IterableApi.getInstance().trackDuplicateSend(notificationBuilder.iterableNotificationData.getMessageId());
//                }
                new IterableNotificationManager().execute(notificationBuilder);
            } else {
                IterableLogger.d(TAG, "Iterable OS notification push received");
            }
        } else {
            IterableLogger.d(TAG, "Iterable ghost silent push received");

            String notificationType = extras.getString("notificationType");
            if (notificationType != null && IterableApi.getInstance().getMainActivityContext() != null) {
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

    static boolean isDuplicateMessageId(Context context, String messageId) {
        SharedPreferences prefs = context.getSharedPreferences("com.iterable.iterableapi", Context.MODE_PRIVATE);
        String recentMessageIds = prefs.getString("IterableRecentPushMessageIds", null);

        ArrayList<String> recentMessageIdArrayList = new ArrayList<>();
        if (recentMessageIds != null) {
            //Only if there are previous messageIds, check for duplicates
            recentMessageIdArrayList = new ArrayList<>(Arrays.asList(recentMessageIds.split(",")));
            if (recentMessageIdArrayList.contains(messageId)) {
                IterableLogger.d(TAG, "Duplicate message id found matching " + messageId);
                return true;
            }
            while (recentMessageIdArrayList.size() > 9) {
                IterableLogger.d(TAG, "Removing old messageId..");
                recentMessageIdArrayList.remove(0);
            }
        }
        //Add new messageId to list
        recentMessageIdArrayList.add(messageId);

        // Traversing the ArrayList
        StringBuilder str = new StringBuilder();
        for (String messageIdString : recentMessageIdArrayList) {
            str.append(messageIdString).append(",");
        }

        // Condition check to remove the last comma
        String commaseparatedMessageIds = str.toString();
        if (commaseparatedMessageIds.length() > 0)
            commaseparatedMessageIds = commaseparatedMessageIds.substring(0, commaseparatedMessageIds.length() - 1);

        // Writing to shared preference
        SharedPreferences.Editor e = prefs.edit();
        e.putString("IterableRecentPushMessageIds", commaseparatedMessageIds);
        e.apply();

        return false;
    }

    /**
     * Handles token refresh
     * Call this from a custom {@link FirebaseMessagingService} to register the new token with Iterable
     */
    public static void handleTokenRefresh() {
        String registrationToken = getFirebaseToken();
        IterableLogger.d(TAG, "New Firebase Token generated: " + registrationToken);
        IterableApi.getInstance().registerForPush();
    }

    public static String getFirebaseToken() {
        String registrationToken = null;
        try {
            registrationToken = Tasks.await(FirebaseMessaging.getInstance().getToken());
        } catch (ExecutionException e) {
            IterableLogger.e(TAG, e.getLocalizedMessage());
        } catch (InterruptedException e) {
            IterableLogger.e(TAG, e.getLocalizedMessage());
        } catch (Exception e) {
            IterableLogger.e(TAG, "Failed to fetch firebase token");
        }
        return registrationToken;
    }

    /**
     * Checks if the message is an Iterable ghost push or silent push message
     * @param remoteMessage Remote message received from Firebase in
     *                      {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
     * @return Boolean indicating whether the message is an Iterable ghost push or silent push
     */
    public static boolean isGhostPush(RemoteMessage remoteMessage) {
        Map<String, String> messageData = remoteMessage.getData();

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
        if (params != null && params[0] != null) {
            IterableNotificationBuilder notificationBuilder = params[0];
            IterableNotificationHelper.postNotificationOnDevice(notificationBuilder.context, notificationBuilder);
        }
        return null;
    }
}