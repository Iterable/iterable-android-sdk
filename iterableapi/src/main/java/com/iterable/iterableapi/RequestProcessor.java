package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface RequestProcessor {

    void processGetRequest(IterableApiClient.AuthProvider authProvider, @NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.IterableActionHandler onCallback);

    void processPostRequest(IterableApiClient.AuthProvider authProvider, @NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure);
}
