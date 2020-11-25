package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public class OnlineRequestProcessor implements RequestProcessor {
    @Override
    public void processGetRequest(IterableApiClient.AuthProvider authProvider, @NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(authProvider.getApiKey(), resourcePath, json, IterableApiRequest.GET, authProvider.getAuthToken(), onCallback);
        new IterableRequest().execute(request);
    }

    @Override
    public void processPostRequest(IterableApiClient.AuthProvider authProvider, @NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(authProvider.getApiKey(), resourcePath, json, IterableApiRequest.POST, authProvider.getAuthToken(), onSuccess, onFailure);
        new IterableRequest().execute(request);
    }

}
