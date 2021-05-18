package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class OfflineRequestProcessor implements RequestProcessor {
    private TaskScheduler taskScheduler;
    private IterableTaskRunner taskRunner;
    private IterableTaskStorage taskStorage;
    private HealthMonitor healthMonitor;

    private static final Set<String> offlineApiSet = new HashSet<>(Arrays.asList(
            IterableConstants.ENDPOINT_TRACK,
            IterableConstants.ENDPOINT_TRACK_PUSH_OPEN,
            IterableConstants.ENDPOINT_TRACK_PURCHASE,
            IterableConstants.ENDPOINT_TRACK_INAPP_OPEN,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE,
            IterableConstants.ENDPOINT_TRACK_INBOX_SESSION,
            IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY,
            IterableConstants.ENDPOINT_INAPP_CONSUME));

    OfflineRequestProcessor(Context context, HealthMonitor healthMonitor) {
        IterableNetworkConnectivityManager networkConnectivityManager = IterableNetworkConnectivityManager.sharedInstance(context);
        taskStorage = IterableTaskStorage.sharedInstance(context);
        this.healthMonitor = healthMonitor;
        taskRunner = new IterableTaskRunner(taskStorage,
                IterableActivityMonitor.getInstance(),
                networkConnectivityManager,
                healthMonitor);
        taskScheduler = new TaskScheduler(taskStorage, taskRunner);
    }

    @VisibleForTesting
    OfflineRequestProcessor(TaskScheduler scheduler, IterableTaskRunner iterableTaskRunner, IterableTaskStorage storage, HealthMonitor healthMonitor) {
        taskRunner = iterableTaskRunner;
        taskScheduler = scheduler;
        taskStorage = storage;
        this.healthMonitor = healthMonitor;
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.GET, authToken, onCallback);
        new IterableRequestTask().execute(request);
    }

    @Override
    public void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.POST, authToken, onSuccess, onFailure);
        if (isRequestOfflineCompatible(request.resourcePath) && healthMonitor.canSchedule()) {
            request.setProcessorType(IterableApiRequest.ProcessorType.OFFLINE);
            taskScheduler.scheduleTask(request, onSuccess, onFailure);
        } else {
            new IterableRequestTask().execute(request);
        }
    }

    @Override
    public void onLogout(Context context) {
        taskStorage.deleteAllTasks();
    }

    boolean isRequestOfflineCompatible(String baseUrl) {
        return offlineApiSet.contains(baseUrl);
    }
}

//Mock TaskScheduler for testing purpose.
class TaskScheduler implements IterableTaskRunner.TaskCompletedListener {
    static HashMap<String, IterableHelper.SuccessHandler> successCallbackMap = new HashMap<>();
    static HashMap<String, IterableHelper.FailureHandler> failureCallbackMap = new HashMap<>();
    private final IterableTaskStorage taskStorage;
    private final IterableTaskRunner taskRunner;

    TaskScheduler(IterableTaskStorage taskStorage, IterableTaskRunner taskRunner) {
        this.taskStorage = taskStorage;
        this.taskRunner = taskRunner;
        taskRunner.addTaskCompletedListener(this);
    }

    void scheduleTask(IterableApiRequest request, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        JSONObject serializedRequest = null;
        try {
            serializedRequest = request.toJSONObject();
        } catch (JSONException e) {
            IterableLogger.e("RequestProcessor", "Failed serializing the request for offline execution. Attempting to request the request now...");
            new IterableRequestTask().execute(request);
            return;
        }

        String taskId = taskStorage.createTask(request.resourcePath, IterableTaskType.API, serializedRequest.toString());

        successCallbackMap.put(taskId, onSuccess);
        failureCallbackMap.put(taskId, onFailure);
    }

    @MainThread
    @Override
    public void onTaskCompleted(String taskId, IterableTaskRunner.TaskResult result, IterableApiResponse response) {
        IterableHelper.SuccessHandler onSuccess = successCallbackMap.get(taskId);
        IterableHelper.FailureHandler onFailure = failureCallbackMap.get(taskId);
        successCallbackMap.remove(taskId);
        failureCallbackMap.remove(taskId);
        if (response.success) {
            if (onSuccess != null) {
                onSuccess.onSuccess(response.responseJson);
            }
        } else {
            if (onFailure != null) {
                onFailure.onFailure(response.errorMessage, response.responseJson);
            }
        }
    }
}
