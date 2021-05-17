package com.iterable.iterableapi;

import java.util.ArrayList;

public class HealthMonitor implements HealthMonitorHandler{
    private static final String TAG = "HealthMonitor";

    private IterableTaskStorage database;
    private boolean errored = false;
    private ArrayList<HealthMonitorHandler> healthMonitorListeners = new ArrayList<>();
    public HealthMonitor(IterableTaskStorage database) {
        this.database = database;
    }

    public boolean canSchedule() {
        IterableLogger.d(TAG, "canSchedule");

        return database.isDatabaseReady() && database.numberOfTasks() < IterableConstants.MAX_OFFLINE_OPERATION;
        //TODO: errored has to be set by DB errors being found during Taskstorage initialization or during other DB operations.
//        if (errored) {
//            return false;
//        }
//
//        return database.numberOfTasks() < IterableConstants.MAX_OFFLINE_OPERATION;
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

    //TODO: Is it similar to DBError? how should the logic work
    private void onError() {
        errored = true;

    }

    synchronized void addHealthMonitorListener(HealthMonitorHandler listener) {
        healthMonitorListeners.add(listener);
    }

    synchronized void removeNetworkListener(HealthMonitorHandler listener) {
        healthMonitorListeners.remove(listener);
    }

    //TODO: Might have to reconsider changing this
    @Override
    public void onDBError() {
        ArrayList<HealthMonitorHandler> healthMonitorHandlersCopy = new ArrayList<>(healthMonitorListeners);
        for (HealthMonitorHandler listener : healthMonitorHandlersCopy) {
            listener.onDBError();
        }
    }
}
