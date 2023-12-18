package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class AnonymousUserMerge {
    private static final AnonymousUserManager anonymousUserManager = new AnonymousUserManager();
    void mergeUserUsingUserId(IterableApiClient apiClient, String destinationUserId) {
        String sourceUserId = IterableApi.getInstance().getUserId();
        if (sourceUserId == null || sourceUserId.isEmpty()) {
            return;
        }
        apiClient.getUserByUserID(sourceUserId, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(@Nullable String data) {
                if(data != null) {
                    try {
                        JSONObject dataObj = new JSONObject(data);
                        if (dataObj.has("user")){
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
        String sourceEmail = IterableApi.getInstance().getUserId();
        if (sourceEmail == null || sourceEmail.isEmpty()) {
            return;
        }
        apiClient.getUserByEmail(sourceEmail, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(@Nullable String data) {
                if(data != null) {
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
