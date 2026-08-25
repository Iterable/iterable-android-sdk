package com.iterable.iterableapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
            IterableConstants.ENDPOINT_INAPP_CONSUME,
            IterableConstants.ENDPOINT_UPDATE_CART,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION,
            IterableConstants.ENDPOINT_DISABLE_DEVICE,
            // Queued alongside disableDevice so a logout-then-login sequence replays in
            // scheduledAt order as disable-then-register. Queueing the disable on its own
            // would let a stale disable land after the new user registered and silently kill
            // a live push registration, because the backend merge is last-write-wins.
            IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN
    ));

    OfflineRequestProcessor(Context context) {
        IterableNetworkConnectivityManager networkConnectivityManager = IterableNetworkConnectivityManager.sharedInstance(context);
        taskStorage = IterableTaskStorage.sharedInstance(context);
        healthMonitor = new HealthMonitor(taskStorage);
        ApiEndpointClassification classification = IterableApi.getInstance().apiEndpointClassification;
        taskRunner = new IterableTaskRunner(taskStorage,
                IterableActivityMonitor.getInstance(),
                networkConnectivityManager,
                healthMonitor,
                classification);
        taskScheduler = new TaskScheduler(taskStorage, taskRunner);

        // Register task runner as auth token ready listener for JWT auto-retry support
        try {
            IterableApi.getInstance().getAuthManager().addAuthTokenReadyListener(taskRunner);
        } catch (Exception e) {
            IterableLogger.w("OfflineRequestProcessor", "Failed to register auth token listener. " +
                    "Auto-retry on JWT failure will not work until AuthManager is available.");
        }
    }

    /**
     * Unregisters the auth token listener to prevent stale listener accumulation
     * when the processor is replaced (e.g., when offline mode is toggled).
     */
    void dispose() {
        try {
            IterableApi.getInstance().getAuthManager().removeAuthTokenReadyListener(taskRunner);
        } catch (Exception e) {
            IterableLogger.w("OfflineRequestProcessor", "Failed to unregister auth token listener on dispose.");
        }
    }

    @VisibleForTesting
    OfflineRequestProcessor(TaskScheduler scheduler, IterableTaskRunner iterableTaskRunner, IterableTaskStorage storage, HealthMonitor mockHealthMonitor) {
        taskRunner = iterableTaskRunner;
        taskScheduler = scheduler;
        taskStorage = storage;
        healthMonitor = mockHealthMonitor;
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.GET, authToken, onCallback);
        new IterableRequestTask().execute(request);
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken,  @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.GET, authToken, onSuccess, onFailure);
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
        // A queued disableDevice is the logout itself retrying, so it has to outlive the purge.
        // It carries the identity it was created with, so it still targets the outgoing user.
        taskScheduler.onTasksPurged(taskStorage.deleteAllTasksExcept(IterableConstants.ENDPOINT_DISABLE_DEVICE));
    }

    boolean isRequestOfflineCompatible(String baseUrl) {
        return offlineApiSet.contains(baseUrl);
    }

    @VisibleForTesting
    static Set<String> getOfflineApiSet() {
        return Collections.unmodifiableSet(offlineApiSet);
    }
}

class TaskScheduler implements IterableTaskRunner.TaskCompletedListener {
    @VisibleForTesting
    static final String PURGED_ON_LOGOUT_REASON =
            "Request was discarded before it could be sent because the user logged out";

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
        if (taskId == null) {
            new IterableRequestTask().execute(request);
            return;
        }
        successCallbackMap.put(taskId, onSuccess);
        failureCallbackMap.put(taskId, onFailure);
    }

    /**
     * Settles the callbacks parked for tasks that were deleted from the queue before they could run.
     * {@link #onTaskCompleted} is only ever reached from {@link IterableTaskRunner}, and a deleted
     * task is never run, so without this the app's handler never fires and the map entries live for
     * the rest of the process. Most visibly, {@code setEmail}'s completion handlers travel with the
     * queued {@code registerDeviceToken}, so an app dismissing a login spinner in that callback
     * would wait forever.
     *
     * @param taskIds ids of the tasks the purge removed
     */
    void onTasksPurged(@NonNull List<String> taskIds) {
        // Unparked before anything is notified, so a handler that logs back in cannot see, re-fire
        // or re-purge an entry this call already owns.
        final List<IterableHelper.FailureHandler> orphanedHandlers = new ArrayList<>();
        for (String taskId : taskIds) {
            IterableHelper.FailureHandler onFailure = failureCallbackMap.remove(taskId);
            successCallbackMap.remove(taskId);
            if (onFailure != null) {
                orphanedHandlers.add(onFailure);
            }
        }
        if (orphanedHandlers.isEmpty()) {
            return;
        }

        // Same thread and looper a real failure would arrive on. It also puts the notification after
        // the purge and after the logout that triggered it, so app code re-entering the SDK from the
        // handler runs against a settled queue.
        new Handler(Looper.getMainLooper()).post(() -> {
            for (IterableHelper.FailureHandler onFailure : orphanedHandlers) {
                try {
                    onFailure.onFailure(PURGED_ON_LOGOUT_REASON, null);
                } catch (Exception e) {
                    IterableLogger.e("TaskScheduler", "Failed to notify a discarded request's failure handler", e);
                }
            }
        });
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
