package com.iterable.iterableapi;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class IterableTaskRunner implements IterableTaskStorage.TaskCreatedListener, Handler.Callback, IterableNetworkConnectivityManager.IterableNetworkMonitorListener, IterableActivityMonitor.AppStateCallback {
    private static final String TAG = "IterableTaskRunner";
    private IterableTaskStorage taskStorage;
    private IterableActivityMonitor activityMonitor;
    private IterableNetworkConnectivityManager networkConnectivityManager;
    private HealthMonitor healthMonitor;

    private static final int RETRY_INTERVAL_SECONDS = 60;

    private static final int OPERATION_PROCESS_TASKS = 100;

    private final HandlerThread networkThread = new HandlerThread("NetworkThread");
    Handler handler;

    enum TaskResult {
        SUCCESS, FAILURE, RETRY
    }

    interface TaskCompletedListener {
        @MainThread
        void onTaskCompleted(String taskId, TaskResult result, IterableApiResponse response);
    }

    private ArrayList<TaskCompletedListener> taskCompletedListeners = new ArrayList<>();

    IterableTaskRunner(IterableTaskStorage taskStorage,
                       IterableActivityMonitor activityMonitor,
                       IterableNetworkConnectivityManager networkConnectivityManager,
                       HealthMonitor healthMonitor) {
        this.taskStorage = taskStorage;
        this.activityMonitor = activityMonitor;
        this.networkConnectivityManager = networkConnectivityManager;
        this.healthMonitor = healthMonitor;
        networkThread.start();
        handler = new Handler(networkThread.getLooper(), this);
        taskStorage.addTaskCreatedListener(this);
        networkConnectivityManager.addNetworkListener(this);
        activityMonitor.addCallback(this);
    }

    void addTaskCompletedListener(TaskCompletedListener listener) {
        taskCompletedListeners.add(listener);
    }

    void removeTaskCompletedListener(TaskCompletedListener listener) {
        taskCompletedListeners.remove(listener);
    }

    @Override
    public void onTaskCreated(IterableTask iterableTask) {
        runNow();
    }

    @Override
    public void onNetworkConnected() {
        runNow();
    }

    @Override
    public void onNetworkDisconnected() {

    }

    @Override
    public void onSwitchToForeground() {
        runNow();
    }

    @Override
    public void onSwitchToBackground() {

    }

    private void runNow() {
        handler.removeMessages(OPERATION_PROCESS_TASKS);
        handler.sendEmptyMessage(OPERATION_PROCESS_TASKS);
    }

    private void scheduleRetry() {
        handler.removeCallbacksAndMessages(OPERATION_PROCESS_TASKS);
        handler.sendEmptyMessageDelayed(OPERATION_PROCESS_TASKS, RETRY_INTERVAL_SECONDS * 1000);
    }

    @WorkerThread
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == OPERATION_PROCESS_TASKS) {
            processTasks();
            return true;
        }
        return false;
    }

    @WorkerThread
    private void processTasks() {
        if (!activityMonitor.isInForeground()) {
            IterableLogger.d(TAG, "App not in foreground, skipping processing tasks");
            return;
        }

        if (!healthMonitor.canProcess()) {
            return;
        }

        while (networkConnectivityManager.isConnected()) {
            IterableTask task = taskStorage.getNextScheduledTask();

            if (task == null) {
                return;
            }

            boolean proceed = processTask(task);
            if (!proceed) {
                scheduleRetry();
                return;
            }
        }
    }

    @WorkerThread
    private boolean processTask(@NonNull IterableTask task) {
        if (task.taskType == IterableTaskType.API) {
            IterableApiResponse response = null;
            TaskResult result = TaskResult.FAILURE;
            try {
                IterableApiRequest request = IterableApiRequest.fromJSON(getTaskDataWithDate(task), null, null);
                request.setProcessorType(IterableApiRequest.ProcessorType.OFFLINE);
                response = IterableRequestTask.executeApiRequest(request);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Error while processing request task", e);
                healthMonitor.onNextTaskError();
            }

            if (response != null) {
                if (response.success) {
                    result = TaskResult.SUCCESS;
                } else {
                    if (isRetriableError(response.errorMessage)) {
                        result = TaskResult.RETRY;
                    } else {
                        result = TaskResult.FAILURE;
                    }
                }
            }
            callTaskCompletedListeners(task.id, result, response);
            if (result == TaskResult.RETRY) {
                // Keep the task, stop further processing
                return false;
            } else {
                taskStorage.deleteTask(task.id);
                return true;
            }
        }
        return false;
    }

    JSONObject getTaskDataWithDate(IterableTask task) {
        try {
            JSONObject jsonData = new JSONObject(task.data);
            jsonData.getJSONObject("data").put(IterableConstants.KEY_CREATED_AT, task.createdAt / 1000);
            return jsonData;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isRetriableError(String errorMessage) {
        return errorMessage.contains("failed to connect");
    }

    @WorkerThread
    private void callTaskCompletedListeners(final String taskId, final TaskResult result, final IterableApiResponse response) {
        for (final TaskCompletedListener listener : taskCompletedListeners) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onTaskCompleted(taskId, result, response);
                }
            });
        }
    }
}
