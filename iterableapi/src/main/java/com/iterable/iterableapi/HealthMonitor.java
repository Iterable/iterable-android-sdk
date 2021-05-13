package com.iterable.iterableapi;

public class HealthMonitor {
    private static final String TAG = "HealthMonitor";

    public HealthMonitor(IterableTaskStorage database) {
        this.database = database;
    }

    public boolean canSchedule() {
        IterableLogger.d(TAG, "canSchedule");

        if (errored) {
            return false;
        }

        return database.numberOfTasks() < IterableConstants.MAX_OFFLINE_OPERATION;
    }

    public boolean canProcess() {
        IterableLogger.d(TAG, "canProcess");
        return !errored;
    }

    public void onScheduleError() {
        IterableLogger.w(TAG, "onScheduleError");

    }

    void onNextTaskError() {
        IterableLogger.w(TAG, "onNextTaskError");
        onError();
    }

    void onDeleteAllTasksError() {
        IterableLogger.w(TAG, "onDeleteAllTasksError");
        onError();
    }

    private void onError() {
        errored = true;

    }

    private IterableTaskStorage database;
    private boolean errored = false;
}
