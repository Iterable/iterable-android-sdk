package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class AnonymousUserMerge {
    private static final AnonymousUserManager anonymousUserManager = new AnonymousUserManager();
    private static final String TAG = "AnonymousUserMerge";

    void mergeUserUsingUserId(IterableApiClient apiClient, String destinationUserId) {
        String sourceUserId = IterableApi.getInstance().getUserId();
        if (sourceUserId == null || sourceUserId.isEmpty() || sourceUserId.equals(destinationUserId)) {
            IterableLogger.d(TAG, "sourceUserId is null or same as destinationUserId");
            return;
        }
        apiClient.getUserByUserID(destinationUserId, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(@Nullable String data) {
                if (data != null) {
                    try {
                        JSONObject dataObj = new JSONObject(data);
                        if (dataObj.has("user")) {
                            callMergeApi(apiClient, "", sourceUserId, IterableApi.getInstance().getEmail(), destinationUserId);
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    void mergeUserUsingEmail(IterableApiClient apiClient, String destinationEmail) {
        String sourceEmail = IterableApi.getInstance().getEmail();
        if (sourceEmail == null || sourceEmail.isEmpty() || sourceEmail.equals(destinationEmail)) {
            IterableLogger.d(TAG, "sourceEmail is null or same as destinationEmail");
            return;
        }
        apiClient.getUserByEmail(destinationEmail, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(@Nullable String data) {
                IterableLogger.d(TAG, "data of email: " + data);
                if (data != null) {
                    callMergeApi(apiClient, destinationEmail, "", destinationEmail, IterableApi.getInstance().getUserId());
                }
            }
        });
    }

    private void callMergeApi(IterableApiClient apiClient,  String sourceEmail, String sourceUserId, String destinationEmail, String destinationUserId) {
        apiClient.mergeUser(sourceEmail, sourceUserId, destinationEmail, destinationUserId, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                anonymousUserManager.syncEvents();
            }
        }, null);
    }
}