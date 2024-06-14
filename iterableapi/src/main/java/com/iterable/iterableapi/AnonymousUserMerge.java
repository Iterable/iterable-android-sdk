package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public class AnonymousUserMerge {
    private static final AnonymousUserManager anonymousUserManager = new AnonymousUserManager();
    private static final String TAG = "AnonymousUserMerge";

    public void tryMergeUser(IterableApiClient apiClient, String sourceUserId, String destinationUserIdOrEmail, boolean isEmail, MergeResultCallback callback) {
        if (sourceUserId != null && destinationUserIdOrEmail != null) {
            String destinationEmail = isEmail ? destinationUserIdOrEmail : null;
            String destinationUserId = isEmail ? null : destinationUserIdOrEmail;

            apiClient.mergeUser(null, sourceUserId, destinationEmail, destinationUserId, new IterableHelper.SuccessHandler() {
                @Override
                public void onSuccess(@NonNull JSONObject data) {
                    IterableApi.getInstance().setAnonUser(null);
                    anonymousUserManager.syncEvents();
                    if (callback != null) {
                        callback.onResult(true); // Notify success
                    }
                }
            }, new IterableHelper.FailureHandler() {
                @Override
                public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                    if (callback != null) {
                        callback.onResult(false); // Notify failure
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onResult(true); // Return true if inputs are null as per original logic
            }
        }
    }

    void mergeUserUsingUserId(IterableApiClient apiClient, String destinationUserId) {
        String sourceUserId = IterableApi.getInstance().getUserId();
        if (sourceUserId == null || sourceUserId.isEmpty() || sourceUserId.equals(destinationUserId)) {
            IterableLogger.d(TAG, "sourceUserId is null or same as destinationUserId");
            return;
        }
        callMergeApi(apiClient, null, sourceUserId, null, destinationUserId);
    }

    void mergeUserUsingEmail(IterableApiClient apiClient, String destinationEmail) {
        String sourceUserId = IterableApi.getInstance().getUserId();
        if (sourceUserId == null || sourceUserId.isEmpty()) {
            IterableLogger.d(TAG, "sourceEmail is null or same as destinationEmail");
            return;
        }
        callMergeApi(apiClient, null, sourceUserId, destinationEmail, null);

    }

    private void callMergeApi(IterableApiClient apiClient,  String sourceEmail, String sourceUserId, String destinationEmail, String destinationUserId) {
        apiClient.mergeUser(sourceEmail, sourceUserId, destinationEmail, destinationUserId, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                IterableApi.getInstance().setAnonUser(null);
                anonymousUserManager.syncEvents();
            }
        }, null);
    }
}