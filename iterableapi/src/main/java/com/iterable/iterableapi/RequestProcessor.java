package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface RequestProcessor {
    void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback);

    void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure);

    void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure);

    /**
     * @param baseUrl region endpoint the request was created for, or null to resolve it from the live
     *                config when the request is sent. Set by callers that captured an API key ahead
     *                of time, so the key and the endpoint stay a matched pair. The default
     *                implementation ignores it and keeps resolving the endpoint at send time.
     */
    default void processPostRequest(@Nullable String apiKey, @Nullable String baseUrl, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        processPostRequest(apiKey, resourcePath, json, authToken, onSuccess, onFailure);
    }

    void onLogout(Context context);
}
