package com.iterable.iterableapi;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.Executor;

class OnlineRequestProcessor implements RequestProcessor {

    private static final String TAG = "OnlineRequestProcessor";
    private final @Nullable Executor executor;

    OnlineRequestProcessor() {
        this(null);
    }

    OnlineRequestProcessor(@Nullable Executor executor) {
        this.executor = executor;
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, addCreatedAtToJson(json), IterableApiRequest.GET, authToken, onCallback);
        executeRequest(request, null);
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, addCreatedAtToJson(json), IterableApiRequest.GET, authToken, onSuccess, onFailure);
        executeRequest(request, null);
    }

    @Override
    public void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        processPostRequest(apiKey, resourcePath, json, authToken, onSuccess, onFailure, null);
    }

    void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure, @Nullable IterableRequestRetryState retryState) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, addCreatedAtToJson(json), IterableApiRequest.POST, authToken, onSuccess, onFailure);
        executeRequest(request, retryState);
    }

    private void executeRequest(@NonNull IterableApiRequest request, @Nullable IterableRequestRetryState retryState) {
        request.setExecutionContext(executor, retryState);
        new IterableRequestTask().executeOnExecutor(getExecutor(), request);
    }

    private @NonNull Executor getExecutor() {
        return executor != null ? executor : AsyncTask.THREAD_POOL_EXECUTOR;
    }

    @Override
    public void onLogout(Context context) {

    }

    JSONObject addCreatedAtToJson(JSONObject jsonObject) {
        try {
            long createdAt;
            if (jsonObject.has(IterableConstants.KEY_CREATED_AT)) {
                createdAt = Long.parseLong(jsonObject.getString(IterableConstants.KEY_CREATED_AT));
            } else {
                createdAt = new Date().getTime() / 1000;
            }
            jsonObject.put(IterableConstants.KEY_CREATED_AT, createdAt);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "Could not add createdAt timestamp to json object");
        }
        return jsonObject;
    }

}
