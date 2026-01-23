package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.response.handlers.IterableCallbackHandlers;

import org.json.JSONObject;

public interface RequestProcessor {
    void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback);

    void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableCallbackHandlers.SuccessCallback onSuccess, @Nullable IterableHelper.FailureHandler onFailure);

    void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableCallbackHandlers.SuccessCallback onSuccess, @Nullable IterableHelper.FailureHandler onFailure);
    void onLogout(Context context);
}
