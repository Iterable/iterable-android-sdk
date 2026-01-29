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

/**
 * Manages scheduling of notification processing work using WorkManager.
 * This class is responsible for:
 * - Creating WorkManager requests for notification processing
 * - Enqueueing work with expedited execution for high-priority notifications
 * - Providing callback interface for success/failure handling
 * - Comprehensive logging of scheduling operations
 */
class IterableNotificationWorkScheduler {

    private static final String TAG = "IterableNotificationWorkScheduler";

    private final Context context;
    private final WorkManager workManager;

    /**
     * Callback interface for work scheduling results.
     * Allows caller to handle success/failure appropriately.
     */
    public interface SchedulerCallback {
        /**
         * Called when work is successfully scheduled.
         * @param workId UUID of the scheduled work
         */
        void onScheduleSuccess(UUID workId);

        /**
         * Called when work scheduling fails.
         * @param exception The exception that caused the failure
         * @param notificationData The original notification data (for fallback)
         */
        void onScheduleFailure(Exception exception, Bundle notificationData);
    }

    /**
     * Constructor for production use.
     * Initializes with application context and default WorkManager instance.
     * 
     * @param context Application or service context
     */
    public IterableNotificationWorkScheduler(@NonNull Context context) {
        this(context, WorkManager.getInstance(context));
    }

    /**
     * Constructor for testing.
     * Allows injection of mock WorkManager for unit testing.
     * 
     * @param context Application or service context
     * @param workManager WorkManager instance (can be mocked for tests)
     */
    @VisibleForTesting
    IterableNotificationWorkScheduler(@NonNull Context context, @NonNull WorkManager workManager) {
        this.context = context.getApplicationContext();
        this.workManager = workManager;
    }

    /**
     * Schedules notification processing work using WorkManager.
     * 
     * Creates an expedited OneTimeWorkRequest and enqueues it with WorkManager.
     * Expedited execution ensures high-priority notifications are processed promptly,
     * with quota exemption when called from FCM onMessageReceived.
     * 
     * @param notificationData Bundle containing notification data
     * @param isGhostPush Whether this is a ghost/silent push
     * @param callback Optional callback for success/failure (can be null)
     */
    public void scheduleNotificationWork(
            @NonNull Bundle notificationData,
            boolean isGhostPush,
            @Nullable SchedulerCallback callback) {

        IterableLogger.d(TAG, "========================================");
        IterableLogger.d(TAG, "Scheduling notification work");
        IterableLogger.d(TAG, "Bundle keys: " + notificationData.keySet().size());
        IterableLogger.d(TAG, "Is ghost push: " + isGhostPush);

        try {
            IterableLogger.d(TAG, "Step 1: Creating Worker input data");
            androidx.work.Data inputData = IterableNotificationWorker.createInputData(
                    notificationData, 
                    isGhostPush
            );
            IterableLogger.d(TAG, "  ✓ Worker input data created successfully");

            IterableLogger.d(TAG, "Step 2: Building expedited WorkRequest");
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(IterableNotificationWorker.class)
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();
            IterableLogger.d(TAG, "  ✓ WorkRequest built with expedited execution");

            IterableLogger.d(TAG, "Step 3: Enqueueing work with WorkManager");
            workManager.enqueue(workRequest);

            UUID workId = workRequest.getId();
            IterableLogger.d(TAG, "  ✓ Work enqueued successfully");
            IterableLogger.d(TAG, "");
            IterableLogger.d(TAG, "✓ NOTIFICATION WORK SCHEDULED");
            IterableLogger.d(TAG, "  Work ID: " + workId);
            IterableLogger.d(TAG, "  Priority: EXPEDITED (high-priority notification)");
            IterableLogger.d(TAG, "  Worker: " + IterableNotificationWorker.class.getSimpleName());
            IterableLogger.d(TAG, "========================================");

            if (callback != null) {
                callback.onScheduleSuccess(workId);
            }

        } catch (Exception e) {
            IterableLogger.e(TAG, "========================================");
            IterableLogger.e(TAG, "✗ FAILED TO SCHEDULE NOTIFICATION WORK");
            IterableLogger.e(TAG, "Error type: " + e.getClass().getSimpleName());
            IterableLogger.e(TAG, "Error message: " + e.getMessage());
            IterableLogger.e(TAG, "========================================");

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
