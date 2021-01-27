package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface RequestProcessor {
    void processGetRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.IterableActionHandler onCallback);
    void processPostRequest(@Nullable String apiKey, @NonNull String resourcePath, @NonNull JSONObject json, String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure);
    void onLogout(Context context);
}
