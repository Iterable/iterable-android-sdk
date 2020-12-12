package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

class OfflineRequestProcessor implements RequestProcessor {
    private TaskScheduler taskScheduler;

    OfflineRequestProcessor(Context context) {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(context);
        taskScheduler = new TaskScheduler(taskStorage);
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback) {
        //Invoke taskScheduler and add task to database
    }

    @Override
    public void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, json, IterableApiRequest.POST, authToken, onSuccess, onFailure);
        taskScheduler.scheduleTask(request, onSuccess, onFailure);
    }
}

//Placeholder Taskschedular for testing purpose.
class TaskScheduler {
    static HashMap<String, IterableHelper.SuccessHandler> successCallbackMap = new HashMap<>();
    static HashMap<String, IterableHelper.FailureHandler> failureCallbackMap = new HashMap<>();
    private final IterableTaskStorage taskStorage;

    TaskScheduler(IterableTaskStorage taskStorage) {
        this.taskStorage = taskStorage;
    }

    void scheduleTask(IterableApiRequest request, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(IterableApi.getInstance().getMainActivityContext());
        JSONObject serializedRequest = null;
        try {
            serializedRequest = request.toJSONObject();
        } catch (JSONException e) {
            IterableLogger.e("RequestProcessor", "Failed serializating the request for offline execution. Attempting to request the request now...");
            new IterableRequestTask().execute(request);
            return;
        }

        String taskId = taskStorage.createTask(request.resourcePath, IterableTaskType.API, serializedRequest.toString());

        successCallbackMap.put(taskId, onSuccess);
        failureCallbackMap.put(taskId, onFailure);

        processTasks();
    }

    //Temporary function to convert database offline task to ITerableReuqest and execute.
    void processTasks() {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(IterableApi.getInstance().getMainActivityContext());
        ArrayList<String> taskIds = taskStorage.getAllTaskIds();
        for (String id : taskIds) {
            try {
                IterableApiRequest request = makeRequestFromTask(taskStorage.getTask(id));
                new IterableRequestTask().execute(request);
                taskStorage.deleteTask(id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    IterableApiRequest makeRequestFromTask(IterableTask task) throws JSONException {
        IterableHelper.SuccessHandler onSuccess = successCallbackMap.get(task.id);
        IterableHelper.FailureHandler onFailure = failureCallbackMap.get(task.id);
        successCallbackMap.remove(task.id);
        failureCallbackMap.remove(task.id);
        return IterableApiRequest.fromJSON(new JSONObject(task.data), onSuccess, onFailure);
    }
}
