package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Worker class for processing FCM notifications that require additional work (e.g., image loading).
 * This extends the application lifecycle to ensure notifications are fully processed even if
 * the app process is under pressure.
 */
public class IterableNotificationWorker extends Worker {
    private static final String TAG = "IterableNotificationWorker";
    private static final String KEY_MESSAGE_PRIORITY = "message_priority";
    private static final String KEY_HAS_IMAGE = "has_image";

    public IterableNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Data inputData = getInputData();
            Map<String, String> messageData = new HashMap<>();

            // Extract all message data from WorkManager input
            Map<String, Object> keyValueMap = inputData.getKeyValueMap();
            for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
                if (entry.getValue() instanceof String) {
                    messageData.put(entry.getKey(), (String) entry.getValue());
                }
            }

            Bundle extras = IterableNotificationHelper.mapToBundle(messageData);

            if (!IterableNotificationHelper.isIterablePush(extras)) {
                IterableLogger.d(TAG, "Not an Iterable push message in worker");
                return Result.failure();
            }

            if (IterableNotificationHelper.isGhostPush(extras)) {
                IterableLogger.d(TAG, "Ghost push received in worker - should not happen");
                return Result.failure();
            }

            IterableLogger.d(TAG, "Processing notification with image in worker");

            // Load image if present
            Bitmap notificationImage = null;
            boolean hasImage = inputData.getBoolean(KEY_HAS_IMAGE, false);
            if (hasImage) {
                String imageUrl = IterableNotificationHelper.getImageUrl(extras);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    notificationImage = loadImageFromUrl(imageUrl);
                }
            }

            // Create notification builder
            IterableNotificationBuilder notificationBuilder = IterableNotificationHelper.createNotification(
                    getApplicationContext(), extras);

            if (notificationBuilder == null) {
                IterableLogger.e(TAG, "Failed to create notification builder");
                return Result.failure();
            }

            // Set the loaded image if available
            if (notificationImage != null) {
                notificationBuilder.setLargeIconBitmap(notificationImage);
                notificationBuilder.setBigPictureStyle(notificationImage);
            }

            // Display notification immediately
            IterableNotificationHelper.postNotificationOnDevice(
                    getApplicationContext(), notificationBuilder);

            IterableLogger.d(TAG, "Notification displayed successfully from worker");
            return Result.success();

        } catch (Exception e) {
            IterableLogger.e(TAG, "Error processing notification in worker", e);
            return Result.failure();
        }
    }

    /**
     * Loads an image from a URL synchronously.
     * This is safe to do in a WorkManager worker as it extends the process lifecycle.
     */
    private Bitmap loadImageFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
            if (bitmap != null) {
                IterableLogger.d(TAG, "Successfully loaded image from URL: " + imageUrl);
            } else {
                IterableLogger.e(TAG, "Failed to decode image from URL: " + imageUrl);
            }
            return bitmap;
        } catch (MalformedURLException e) {
            IterableLogger.e(TAG, "Malformed URL: " + imageUrl, e);
        } catch (IOException e) {
            IterableLogger.e(TAG, "IOException loading image from URL: " + imageUrl, e);
        }
        return null;
    }
}


