package com.iterable.iterableapi;

public class AnonymousUserMerge {
    private static final String TAG = "AnonymousUserMerge";

    public void tryMergeUser(IterableApiClient apiClient, String sourceUserId, String destinationUserIdOrEmail, boolean isEmail, MergeResultCallback callback) {
        IterableLogger.v(TAG, "tryMergeUser");
        if (sourceUserId != null && destinationUserIdOrEmail != null) {
            String destinationEmail = isEmail ? destinationUserIdOrEmail : null;
            String destinationUserId = isEmail ? null : destinationUserIdOrEmail;

            apiClient.mergeUser(null, sourceUserId, destinationEmail, destinationUserId, data -> {
                if (callback != null) {
                    callback.onResult(IterableConstants.MERGE_SUCCESSFUL, null); // Notify success
                }
            }, (reason, data) -> {
                if (callback != null) {
                    callback.onResult(IterableConstants.MERGE_NOTREQUIRED, reason); // Notify failure
                }
            });
        } else {
            if (callback != null) {
                callback.onResult(IterableConstants.MERGE_SUCCESSFUL, null); // Return true if inputs are null as per original logic
            }
        }
    }
}