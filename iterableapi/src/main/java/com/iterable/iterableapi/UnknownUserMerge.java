package com.iterable.iterableapi;

public class UnknownUserMerge {
    private static final String TAG = "UnknownUserMerge";

    void tryMergeUser(IterableApiClient apiClient, String unknownUserId, String destinationUser, boolean isEmail, boolean merge, MergeResultCallback callback) {
        IterableLogger.v(TAG, "tryMergeUser");
        if (unknownUserId != null && merge) {
            String destinationEmail = isEmail ? destinationUser : null;
            String destinationUserId = isEmail ? null : destinationUser;
            apiClient.mergeUser(null, unknownUserId, destinationEmail, destinationUserId, data -> {
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