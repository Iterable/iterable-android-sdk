package com.iterable.iterableapi;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONObject;

import java.util.ArrayList;

class IterableTaskRunner implements IterableTaskStorage.TaskCreatedListener, Handler.Callback {
    private static final String TAG = "IterableTaskRunner";
    private IterableTaskStorage taskStorage;
    private IterableActivityMonitor activityMonitor;

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

    IterableTaskRunner(IterableTaskStorage taskStorage, IterableActivityMonitor activityMonitor) {
        this.taskStorage = taskStorage;
        this.activityMonitor = activityMonitor;

        networkThread.start();
        handler = new Handler(networkThread.getLooper(), this);
        taskStorage.addTaskCreatedListener(this);
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

    private void runNow() {
        handler.removeCallbacksAndMessages(this);
        handler.sendEmptyMessage(OPERATION_PROCESS_TASKS);
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
        while (true) {
            boolean proceed = processNextTask();
            if (!proceed) {
                break;
            }
        }
    }

    @WorkerThread
    private boolean processNextTask() {
        IterableTask task = taskStorage.getNextScheduledTask();

        if (task == null) {
            return false;
        }

        if (task.taskType == IterableTaskType.API) {
            IterableApiResponse response = null;
            TaskResult result = TaskResult.FAILURE;
            try {
                IterableApiRequest request = IterableApiRequest.fromJSON(new JSONObject(task.data), null, null);
                response = IterableRequestTask.executeApiRequest(request);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Error while processing request task", e);
            }
            if (response != null) {
                result = response.success ? TaskResult.SUCCESS : TaskResult.FAILURE;
            }
            callTaskCompletedListeners(task.id, result, response);
            taskStorage.deleteTask(task.id);
            return true;
        }
        return false;
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
