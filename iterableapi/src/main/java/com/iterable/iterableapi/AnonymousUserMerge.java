package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public class AnonymousUserMerge {
    private static final AnonymousUserManager anonymousUserManager = new AnonymousUserManager();
    private static final String TAG = "AnonymousUserMerge";

    public void tryMergeUser(IterableApiClient apiClient, String sourceUserId, String destinationUserIdOrEmail, boolean isEmail, MergeResultCallback callback) {
        IterableLogger.v(TAG, "tryMergeUser");
        if (sourceUserId != null && destinationUserIdOrEmail != null) {
            String destinationEmail = isEmail ? destinationUserIdOrEmail : null;
            String destinationUserId = isEmail ? null : destinationUserIdOrEmail;

            apiClient.mergeUser(null, sourceUserId, destinationEmail, destinationUserId, data -> {
                if (callback != null) {
                    callback.onResult(true); // Notify success
                }
            }, (reason, data) -> {
                if (callback != null) {
                    callback.onResult(false); // Notify failure
                }
            });
        } else {
            if (callback != null) {
                callback.onResult(true); // Return true if inputs are null as per original logic
            }
        }
    }
}