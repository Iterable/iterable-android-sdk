package com.iterable.iterableapi;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

class OnlineRequestProcessor implements RequestProcessor {

    private static final String TAG = "OnlineRequestProcessor";

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, addCreatedAtToJson(json), IterableApiRequest.GET, authToken, onCallback);
        new IterableRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, addCreatedAtToJson(json), IterableApiRequest.GET, authToken, onSuccess, onFailure);
        new IterableRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, addCreatedAtToJson(json), IterableApiRequest.POST, authToken, onSuccess, onFailure);
        new IterableRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    @Override
    public void onLogout(Context context) {

    }

    JSONObject addCreatedAtToJson(JSONObject jsonObject) {
        try {
            jsonObject.put(IterableConstants.KEY_CREATED_AT, new Date().getTime() / 1000);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "Could not add createdAt timestamp to json object");
        }
        return jsonObject;
    }

}
