package com.iterable.iterableapi;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.UUID;

class IterableNotificationWorkScheduler {

    private static final String TAG = "IterableNotificationWorkScheduler";

    private final Context context;
    private final WorkManager workManager;

    interface SchedulerCallback {
        void onScheduleSuccess(UUID workId);
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
                    .build();

            workManager.enqueue(workRequest);

            UUID workId = workRequest.getId();
            IterableLogger.d(TAG, "Notification work scheduled: " + workId);

            if (callback != null) {
                callback.onScheduleSuccess(workId);
            }

        } catch (Exception e) {
            IterableLogger.e(TAG, "Failed to schedule notification work", e);
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
