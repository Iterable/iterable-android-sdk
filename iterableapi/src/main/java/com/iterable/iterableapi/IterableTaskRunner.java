package com.iterable.iterableapi;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONObject;

class IterableTaskRunner implements IterableTaskStorage.TaskCreatedListener, Handler.Callback {
    private static final String TAG = "IterableTaskRunner";
    private IterableTaskStorage taskStorage;
    private IterableActivityMonitor activityMonitor;

    private static final int OPERATION_PROCESS_TASKS = 100;

    private final HandlerThread networkThread = new HandlerThread("NetworkThread");
    Handler handler;

    IterableTaskRunner(IterableTaskStorage taskStorage, IterableActivityMonitor activityMonitor) {
        this.taskStorage = taskStorage;
        this.activityMonitor = activityMonitor;

        networkThread.start();
        handler = new Handler(networkThread.getLooper(), this);
        taskStorage.addTaskCreatedListener(this);
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
            try {
                IterableApiRequest request = IterableApiRequest.fromJSON(new JSONObject(task.data), null, null);
                IterableApiResponse response = IterableRequestTask.executeApiRequest(request);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Error while processing request task", e);
            }
            taskStorage.deleteTask(task.id);
            return true;
        }
        return false;
    }
}
