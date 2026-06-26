package com.iterable.iterableapi;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public interface IterableUnknownUserHandler {
    /**
     * Called when an unknown user is created after activation criteria are met.
     * @param userId the generated unknown user id
     */
    void onUnknownUserCreated(String userId);

    /**
     * Called when the SDK fetches unknown user activation criteria successfully.
     * <p>
     * Fires after the criteria response has been persisted to local storage, so it is safe to call
     * {@code track}, {@code trackPurchase}, etc. from this callback and have those events evaluated
     * against the just-arrived criteria set.
     * <p>
     * Fires on every criteria fetch, implementation should take that into consideration.
     * @param criteria the criteria returned by the server
     */
    default void onCriteriaReceived(@NonNull JSONObject criteria) {}

    /**
     * Called when the unknown user activation criteria fetch fails.
     * @param reason a description of the failure
     */
    default void onCriteriaFetchFailed(@NonNull String reason) {}
}
