package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private final IterableRequestDispatcher immediateRequestDispatcher;

    private static final Set<String> offlineApiSet = new HashSet<>(Arrays.asList(
            IterableConstants.ENDPOINT_TRACK,
            IterableConstants.ENDPOINT_TRACK_PUSH_OPEN,
            IterableConstants.ENDPOINT_TRACK_PURCHASE,
            IterableConstants.ENDPOINT_TRACK_INAPP_OPEN,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE,
            IterableConstants.ENDPOINT_TRACK_INBOX_SESSION,
            IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY,
            IterableConstants.ENDPOINT_INAPP_CONSUME,
            IterableConstants.ENDPOINT_UPDATE_CART,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION
    ));

    OfflineRequestProcessor(
            Context context,
            IterableRequestDispatcher immediateRequestDispatcher,
            IterableRequestDispatcher offlineRequestDispatcher
    ) {
        this.immediateRequestDispatcher = immediateRequestDispatcher;
        IterableNetworkConnectivityManager networkConnectivityManager = IterableNetworkConnectivityManager.sharedInstance(context);
        taskStorage = IterableTaskStorage.sharedInstance(context);
        healthMonitor = new HealthMonitor(taskStorage);
        ApiEndpointClassification classification = IterableApi.getInstance().apiEndpointClassification;
        taskRunner = new IterableTaskRunner(taskStorage,
                IterableActivityMonitor.getInstance(),
                networkConnectivityManager,
                healthMonitor,
                classification,
                offlineRequestDispatcher);
        taskScheduler = new TaskScheduler(
                taskStorage,
                taskRunner,
                immediateRequestDispatcher
        );

        // Register task runner as auth token ready listener for JWT auto-retry support
        try {
            IterableApi.getInstance().getAuthManager().addAuthTokenReadyListener(taskRunner);
        } catch (Exception e) {
            IterableLogger.w("OfflineRequestProcessor", "Failed to register auth token listener. " +
                    "Auto-retry on JWT failure will not work until AuthManager is available.");
        }
        taskRunner.start();
    }

    /**
     * Releases the persisted-task runner when the owning API client is disposed.
     */
    void dispose() {
        try {
            IterableApi.getInstance().getAuthManager().removeAuthTokenReadyListener(taskRunner);
        } catch (Exception e) {
            IterableLogger.w("OfflineRequestProcessor", "Failed to unregister auth token listener on dispose.");
        }
        taskRunner.dispose();
    }

    OfflineRequestProcessor(
            TaskScheduler scheduler,
            IterableTaskRunner iterableTaskRunner,
            IterableTaskStorage storage,
            HealthMonitor mockHealthMonitor,
            IterableRequestDispatcher immediateRequestDispatcher
    ) {
        taskRunner = iterableTaskRunner;
        taskScheduler = scheduler;
        taskStorage = storage;
        healthMonitor = mockHealthMonitor;
        this.immediateRequestDispatcher = immediateRequestDispatcher;
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.GET, authToken, onCallback);
        immediateRequestDispatcher.execute(request);
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken,  @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.GET, authToken, onSuccess, onFailure);
        immediateRequestDispatcher.execute(request);
    }

    @Override
    public void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.POST, authToken, onSuccess, onFailure);
        if (isRequestOfflineCompatible(request.resourcePath) && healthMonitor.canSchedule()) {
            request.setProcessorType(IterableApiRequest.ProcessorType.OFFLINE);
            taskScheduler.scheduleTask(request, onSuccess, onFailure);
        } else {
            immediateRequestDispatcher.execute(request);
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

class TaskScheduler implements IterableTaskRunner.TaskCompletedListener {
    static HashMap<String, IterableHelper.SuccessHandler> successCallbackMap = new HashMap<>();
    static HashMap<String, IterableHelper.FailureHandler> failureCallbackMap = new HashMap<>();
    private final IterableTaskStorage taskStorage;
    private final IterableTaskRunner taskRunner;
    private final IterableRequestDispatcher requestDispatcher;

    TaskScheduler(
            IterableTaskStorage taskStorage,
            IterableTaskRunner taskRunner,
            IterableRequestDispatcher requestDispatcher
    ) {
        this.taskStorage = taskStorage;
        this.taskRunner = taskRunner;
        this.requestDispatcher = requestDispatcher;
        taskRunner.addTaskCompletedListener(this);
    }

    void scheduleTask(IterableApiRequest request, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        JSONObject serializedRequest = null;
        try {
            serializedRequest = request.toJSONObject();
        } catch (JSONException e) {
            IterableLogger.e("RequestProcessor", "Failed serializing the request for offline execution. Attempting to request the request now...");
            requestDispatcher.execute(request);
            return;
        }

        String taskId = taskStorage.createTask(request.resourcePath, IterableTaskType.API, serializedRequest.toString());
        if (taskId == null) {
            requestDispatcher.execute(request);
            return;
        }
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
