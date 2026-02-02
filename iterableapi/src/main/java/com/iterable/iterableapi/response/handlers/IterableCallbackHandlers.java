package com.iterable.iterableapi.response.handlers;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.response.IterableResponseObject;

/**
 * Callback handlers for Iterable SDK operations.
 */
public class IterableCallbackHandlers {

    /**
     * Generic callback interface for successful SDK operations.
     *
     * <p><b>When to use this callback:</b></p>
     * <ul>
     *   <li>When you want to proceed on <b>any</b> type of success (remote, local, or auth token)</li>
     *   <li>When you don't need to access specific response data (like JSON from API or auth tokens)</li>
     *   <li>When you just need to know the operation completed successfully, regardless of how</li>
     * </ul>
     *
     * <p><b>Example use cases:</b></p>
     * <ul>
     *   <li>{@code setEmail()} - Can complete locally (if autoPushRegistration is false) or remotely (via registerForPush)</li>
     *   <li>{@code setUserId()} - May complete locally or trigger remote operations</li>
     *   <li>Any operation where you just need confirmation of success without caring about the response details</li>
     * </ul>
     *
     * <p><b>When to use specialized callbacks instead:</b></p>
     * <ul>
     *   <li>Use {@link RemoteSuccessCallback} if you need to access JSON response data from the API</li>
     *   <li>Use {@link LocalSuccessCallback} if you only want to proceed when no remote call was made</li>
     *   <li>Use {@link com.iterable.iterableapi.response.handlers.auth.IterableAuthCallbackHandlers.AuthTokenCallback}
     *       if you need to access the authentication token</li>
     * </ul>
     *
     * @see RemoteSuccessCallback
     * @see LocalSuccessCallback
     */
    public interface SuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.Success data);
    }

    /**
     * Callback specifically for operations that make remote API calls and return JSON response data.
     *
     * <p><b>When to use this callback:</b></p>
     * <ul>
     *   <li>When you need to access the JSON response data from the Iterable API</li>
     *   <li>When you want your callback to trigger <b>only</b> if a remote API call was made</li>
     *   <li>When the operation you're calling always makes a remote API request</li>
     * </ul>
     *
     * <p><b>Example use cases:</b></p>
     * <ul>
     *   <li>{@code trackEvent()} - Always makes a remote API call with JSON response</li>
     *   <li>{@code updateUser()} - Makes a remote call to update user profile</li>
     *   <li>Operations where you need to inspect the server's response data</li>
     * </ul>
     *
     * <p><b>Important:</b> If the operation completes locally (e.g., {@code setEmail} with autoPushRegistration disabled),
     * your callback will NOT be triggered. Use {@link SuccessCallback} instead if you want to handle both
     * remote and local success cases.</p>
     */
    public interface RemoteSuccessCallback extends SuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.RemoteSuccess data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            // Dispatch to the specific method if it's the correct type
            if (data instanceof IterableResponseObject.RemoteSuccess) {
                onSuccess((IterableResponseObject.RemoteSuccess) data);
            } else {
                IterableLogger.w(
                    "IterableHelper",
                    "RemoteSuccessCallback received unexpected success type: " + data.getClass().getSimpleName() +
                        ". This callback only triggers for remote API responses. Consider using SuccessCallback if you want to handle all success types."
                );
            }
        }
    }

    /**
     * Callback specifically for operations that complete locally without making a remote API call.
     *
     * <p><b>When to use this callback:</b></p>
     * <ul>
     *   <li>When you want your callback to trigger <b>only</b> if the operation completed locally</li>
     *   <li>When you want to distinguish between operations that hit the server vs. those that don't</li>
     *   <li>For testing or debugging purposes to verify no remote call was made</li>
     * </ul>
     *
     * <p><b>Example use cases:</b></p>
     * <ul>
     *   <li>{@code setEmail()} with {@code autoPushRegistration = false} - Updates locally without calling the API</li>
     *   <li>Operations that only update local state or keychain</li>
     *   <li>Scenarios where you want different behavior based on whether a network call occurred</li>
     * </ul>
     *
     * <p><b>Important:</b> If the operation makes a remote call (e.g., {@code setEmail} with autoPushRegistration enabled),
     * your callback will NOT be triggered. Use {@link SuccessCallback} instead if you want to handle both
     * local and remote success cases.</p>
     */
    public interface LocalSuccessCallback extends SuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.LocalSuccess data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            // Dispatch to the specific method if it's the correct type
            if (data instanceof IterableResponseObject.LocalSuccess) {
                onSuccess((IterableResponseObject.LocalSuccess) data);
            } else {
                IterableLogger.w(
                    "IterableHelper",
                    "LocalSuccessCallback received unexpected success type: " + data.getClass().getSimpleName() +
                        ". This callback only triggers for local-only operations. Consider using SuccessCallback if you want to handle all success types."
                );
            }
        }
    }


}
