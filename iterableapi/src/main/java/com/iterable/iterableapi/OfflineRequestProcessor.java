package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public class OfflineRequestProcessor implements RequestProcessor {
    @Override
    public void processGetRequest(IterableApiClient.AuthProvider authProvider, @NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.IterableActionHandler onCallback) {

    }

    @Override
    public void processPostRequest(IterableApiClient.AuthProvider authProvider, @NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {

    }

}
