package com.iterable.iterableapi;

public class HealthMonitor implements IterableTaskStorage.IterableDatabaseStatusListeners {
    private static final String TAG = "HealthMonitor";

    private boolean databaseErrored = false;

    private IterableTaskStorage iterableTaskStorage;

    public HealthMonitor(IterableTaskStorage storage) {
        this.iterableTaskStorage = storage;
        this.iterableTaskStorage.addDatabaseStatusListener(this);
    }

    public boolean canSchedule() {
        IterableLogger.d(TAG, "canSchedule");
        try {
            return !(iterableTaskStorage.getNumberOfTasks() >= IterableConstants.OFFLINE_TASKS_LIMIT);
        } catch (IllegalStateException e) {
            IterableLogger.e(TAG, e.getLocalizedMessage());
            databaseErrored = true;
        }
        return false;
    }

    public boolean canProcess() {
        IterableLogger.d(TAG, "Health monitor can process: " + !databaseErrored);
        return !databaseErrored;
    }

    @Override
    public void onDBError() {
        IterableLogger.e(TAG, "DB Error notified to healthMonitor");
        databaseErrored = true;
    }

    @Override
    public void isReady() {
        IterableLogger.v(TAG, "DB Ready notified to healthMonitor");
        databaseErrored = false;
    }
}
