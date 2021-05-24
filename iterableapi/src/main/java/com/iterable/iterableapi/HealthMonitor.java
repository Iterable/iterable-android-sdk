package com.iterable.iterableapi;

public class HealthMonitor implements IterableTaskStorage.IterableDatabaseStatusListeners {
    private static final String TAG = "HealthMonitor";

    private boolean errored = false;

    private IterableTaskStorage iterableTaskStorage;

    public HealthMonitor(IterableTaskStorage storage) {
        this.iterableTaskStorage = storage;
        this.iterableTaskStorage.addDataBaseListener(this);
    }

    public boolean canSchedule() {
        IterableLogger.d(TAG, "canSchedule");
        try {
            return !(iterableTaskStorage.numberOfTasks() >= IterableConstants.MAX_OFFLINE_OPERATION);
        } catch (Exception e) {
            IterableLogger.e(TAG, e.getLocalizedMessage());
            errored = true;
        }
        return false;
    }

    public boolean canProcess() {
        IterableLogger.d(TAG, "Health monitor can process: " + !errored);
        return !errored;
    }

    public void onScheduleError() {
        IterableLogger.w(TAG, "onScheduleError");
    }

    void onNextTaskError() {
        IterableLogger.w(TAG, "onNextTaskError");
        onDBError();
    }

    void onDeleteAllTasksError() {
        IterableLogger.w(TAG, "onDeleteAllTasksError");
        onDBError();
    }

    @Override
    public void isNotReady() {
        IterableLogger.v(TAG, "DB Not ready notified to healthMonitor");
        errored = true;
    }

    @Override
    public void onDBError() {
        IterableLogger.v(TAG, "DB Error notified to healthMonitor");
        errored = true;
    }

    @Override
    public void isClosed() {
        IterableLogger.v(TAG, "DB Closed notified to healthMonitor");
        errored = true;
    }

    @Override
    public void isReady() {
        IterableLogger.v(TAG, "DB Ready notified to healthMonitor");
        errored = false;
    }
}
