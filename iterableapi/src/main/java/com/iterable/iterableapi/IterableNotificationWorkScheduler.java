package com.iterable.iterableapi;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.util.UUID;

class IterableNotificationWorkScheduler {

    private static final String TAG = "IterableNotificationWorkScheduler";

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

    public void scheduleNotificationWork(
            @NonNull Bundle notificationData,
            boolean isGhostPush,
            @Nullable SchedulerCallback callback) {

        try {
            androidx.work.Data inputData = IterableNotificationWorker.createInputData(
                    notificationData,
                    isGhostPush
            );

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(IterableNotificationWorker.class)
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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

    @VisibleForTesting
    Context getContext() {
        return context;
    }

    @VisibleForTesting
    WorkManager getWorkManager() {
        return workManager;
    }
}
