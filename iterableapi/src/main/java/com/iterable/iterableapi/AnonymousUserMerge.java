package com.iterable.iterableapi;

public class AnonymousUserMerge {
    private static final String TAG = "AnonymousUserMerge";

    public void tryMergeUser(IterableApiClient apiClient, String sourceUserId, String sourceEmail, String destinationUserIdOrEmail, boolean isEmail, Boolean merge, MergeResultCallback callback) {
        IterableLogger.v(TAG, "tryMergeUser");
        if ((sourceUserId != null || sourceEmail != null) && destinationUserIdOrEmail != null && !(merge != null && !merge)) {
            String destinationEmail = isEmail ? destinationUserIdOrEmail : null;
            String destinationUserId = isEmail ? null : destinationUserIdOrEmail;
            apiClient.mergeUser(sourceEmail, sourceUserId, destinationEmail, destinationUserId, data -> {
                if (callback != null) {
                    callback.onResult(IterableConstants.MERGE_SUCCESSFUL, null); // Notify success
                }
            }, (reason, data) -> {
                if (callback != null) {
                    callback.onResult(null, reason); // Notify failure
                }
            });
        } else {
            if (callback != null) {
                callback.onResult(IterableConstants.MERGE_NOTREQUIRED, null); // Return true if inputs are null as per original logic
            }
        }
    }
}