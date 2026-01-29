package com.iterable.iterableapi.response.handlers.auth;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.response.IterableAuthResponseObject;
import com.iterable.iterableapi.response.IterableResponseObject;
import com.iterable.iterableapi.response.handlers.IterableCallbackHandlers;

/**
 * Callback handlers for authentication-related operations.
 */
public class IterableAuthCallbackHandlers {

    /**
     * Callback specifically for authentication token operations.
     *
     * <p><b>When to use this callback:</b></p>
     * <ul>
     *   <li>When you need to access the JWT authentication token from the response</li>
     *   <li>For operations that request or refresh auth tokens</li>
     *   <li>When implementing custom auth token handling or caching</li>
     * </ul>
     *
     * <p><b>Example use cases:</b></p>
     * <ul>
     *   <li>Auth token refresh operations triggered by {@link com.iterable.iterableapi.IterableAuthHandler}</li>
     *   <li>Operations that return a new JWT token for the current user</li>
     *   <li>Custom auth token validation or storage logic</li>
     * </ul>
     *
     * <p><b>Note:</b> This is primarily used internally by the SDK's auth system. Most applications
     * won't need to use this callback directly.</p>
     */
    public interface AuthTokenCallback extends IterableCallbackHandlers.SuccessCallback {
        void onSuccess(@NonNull IterableAuthResponseObject.Success data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            // Dispatch to the specific method if it's the correct type
            if (data instanceof IterableAuthResponseObject.Success) {
                onSuccess((IterableAuthResponseObject.Success) data);
            } else {
                IterableLogger.w(
                    "IterableHelper",
                    "AuthTokenCallback received unexpected success type: " + data.getClass().getSimpleName() +
                        ". This callback is for auth token operations only."
                );
            }
        }
    }
}
