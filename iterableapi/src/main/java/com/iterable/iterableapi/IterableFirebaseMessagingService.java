package com.iterable.iterableapi;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class IterableFirebaseMessagingService extends FirebaseMessagingService {

    static final String TAG = "itblFCMMessagingService";
    private static final String KEY_MESSAGE_PRIORITY = "message_priority";
    private static final String KEY_HAS_IMAGE = "has_image";

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

                // Check if notification has an image that needs to be loaded
                boolean hasImage = IterableNotificationHelper.hasImageUrl(extras);

                if (hasImage) {
                    // Use WorkManager for image loading as per Firebase best practices
                    int priority = remoteMessage.getPriority();
                    scheduleNotificationWork(context, messageData, priority, true);
                } else {
                    // Display notification immediately - no async work needed
                    IterableNotificationBuilder notificationBuilder = IterableNotificationHelper.createNotification(
                            context.getApplicationContext(), extras);
                    if (notificationBuilder != null) {
                        IterableNotificationHelper.postNotificationOnDevice(
                                context.getApplicationContext(), notificationBuilder);
                    }
                }
            } else {
                IterableLogger.d(TAG, "Iterable OS notification push received");
            }
        } else {
            IterableLogger.d(TAG, "Iterable ghost silent push received");

            String notificationType = extras.getString("notificationType");
            if (notificationType != null && IterableApi.getInstance().getMainActivityContext() != null) {
                switch (notificationType) {
                    case "InAppUpdate":
                        IterableApi.getInstance().getInAppManager().syncInApp();
                        break;
                    case "InAppRemove":
                        String messageId = extras.getString("messageId");
                        if (messageId != null) {
                            IterableApi.getInstance().getInAppManager().removeMessage(messageId);
                        }
                        break;
                    case "UpdateEmbedded":
                        IterableApi.getInstance().getEmbeddedManager().syncMessages();
                        break;
                    default:
                        break;
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
     *        {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
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

    /**
     * Schedules WorkManager work for notification processing with image loading
     * @param context Context
     * @param messageData Message data map
     * @param priority FCM message priority (RemoteMessage.PRIORITY_HIGH or PRIORITY_NORMAL)
     * @param hasImage Whether the notification has an image URL
     */
    private static void scheduleNotificationWork(@NonNull Context context,
                                                  @NonNull Map<String, String> messageData,
                                                  int priority,
                                                  boolean hasImage) {
        Data.Builder dataBuilder = new Data.Builder();
        for (Map.Entry<String, String> entry : messageData.entrySet()) {
            dataBuilder.putString(entry.getKey(), entry.getValue());
        }
        dataBuilder.putInt(KEY_MESSAGE_PRIORITY, priority);
        dataBuilder.putBoolean(KEY_HAS_IMAGE, hasImage);

        OneTimeWorkRequest.Builder workRequestBuilder =
                new OneTimeWorkRequest.Builder(IterableNotificationWorker.class)
                .setInputData(dataBuilder.build());

        // Use expedited work for high priority messages as per Firebase best practices
        // Expedited jobs have a brief exemption when scheduled immediately after high priority FCM
        if (priority == RemoteMessage.PRIORITY_HIGH) {
            workRequestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
            IterableLogger.d(TAG, "Scheduling expedited work for high priority notification");
        } else {
            IterableLogger.d(TAG, "Scheduling regular work for normal priority notification");
        }

        try {
            WorkManager.getInstance(context.getApplicationContext())
                    .enqueue(workRequestBuilder.build());
        } catch (IllegalStateException e) {
            // WorkManager not initialized - fallback to immediate display without image
            IterableLogger.e(TAG, "WorkManager not initialized, displaying notification without image", e);
            Bundle extras = IterableNotificationHelper.mapToBundle(messageData);
            IterableNotificationBuilder notificationBuilder = IterableNotificationHelper.createNotification(
                    context.getApplicationContext(), extras);
            if (notificationBuilder != null) {
                IterableNotificationHelper.postNotificationOnDevice(
                        context.getApplicationContext(), notificationBuilder);
            }
        }
    }
}