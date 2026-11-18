package com.iterable.iterableapi;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.util.UUID;

class IterableNotificationWorkScheduler {

    private static final String TAG = "IterableNotificationWorkScheduler";
    private static final String IMAGE_RETRY_WORK_NAME_PREFIX = "iterable_notification_image_retry_";
    private static final OutOfQuotaPolicy OUT_OF_QUOTA_POLICY = OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST;

    private final Context context;
    private final WorkManager workManager;

    interface SchedulerCallback {
        void onScheduleSuccess(UUID workId);
        void onScheduleFailure(Exception exception, Bundle notificationData);
    }

    IterableNotificationWorkScheduler(@NonNull Context context) {
        this(context, WorkManager.getInstance(context));
    }

    @VisibleForTesting
    IterableNotificationWorkScheduler(@NonNull Context context, @NonNull WorkManager workManager) {
        this.context = context.getApplicationContext();
        this.workManager = workManager;
    }

    void scheduleNotificationWork(
            @NonNull Bundle notificationData,
            @Nullable SchedulerCallback callback
    ) {

        try {
            Data inputData = IterableNotificationWorker.createInputData(
                    notificationData
            );

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(IterableNotificationWorker.class)
                    .setInputData(inputData)
                    .setExpedited(OUT_OF_QUOTA_POLICY)
                    .build();

            workManager.enqueue(workRequest);

            UUID workId = workRequest.getId();
            IterableLogger.d(TAG, "Notification work scheduled: " + workId);

            if (callback != null) {
                callback.onScheduleSuccess(workId);
            }

        } catch (Exception e) {
            IterableLogger.e(TAG, "Failed to schedule notification work", e);

            if (callback != null) {
                callback.onScheduleFailure(e, notificationData);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    void scheduleImageRetry(@NonNull Bundle notificationData, int notificationId, long originalPostTime) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(IterableNotificationImageRetryWorker.class)
                .setInputData(IterableNotificationImageRetryWorker.createInputData(
                        notificationData, notificationId, originalPostTime))
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setExpedited(OUT_OF_QUOTA_POLICY)
                .build();

        workManager.enqueueUniqueWork(getImageRetryWorkName(notificationId), ExistingWorkPolicy.KEEP, workRequest);
        IterableLogger.d(TAG, "Notification image retry scheduled: " + workRequest.getId());
    }

    static String getImageRetryWorkName(int notificationId) {
        return IMAGE_RETRY_WORK_NAME_PREFIX + notificationId;
    }

    @VisibleForTesting
    Context getContext() {
        return context;
    }

    @VisibleForTesting
    WorkManager getWorkManager() {
        return workManager;
    }
}
