package com.iterable.iterableapi;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableHelper {

    /**
     * Interface to handle Iterable Actions
     */
    public interface IterableActionHandler {
        void execute(@Nullable String data);
    }

    public interface IterableUrlCallback {
        void execute(@Nullable Uri url);
    }

    /**
     * @deprecated Use {@link IterableSuccessCallback} instead.
     * <p>
     * This interface is deprecated and will be removed in a future version.
     * Replace all usages with {@link IterableSuccessCallback} for general success handling,
     * or {@link RemoteSuccessCallback} if you specifically need JSON response data from the API.
     * <p>
     * <b>Quick Migration (Recommended):</b> Replace {@code SuccessHandler} with {@code IterableSuccessCallback}
     * <pre>
     * // REPLACE THIS:
     * IterableHelper.SuccessHandler successHandler = data -> {
     *     // Your code here
     * };
     *
     * // WITH THIS (see JavaDoc on IterableSuccessCallback for more details):
     * IterableHelper.IterableSuccessCallback successHandler = data -> {
     *     // Your code here - see JavaDoc for accessing response data
     * };
     * </pre>
     *
     * @see IterableSuccessCallback
     * @see RemoteSuccessCallback
     * @see LocalSuccessCallback
     */
    @Deprecated
    public interface SuccessHandler extends IterableSuccessCallback {
        void onSuccess(@NonNull JSONObject data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            IterableLogger.w("IterableHelper", "SuccessHandler is deprecated. Please migrate to IterableSuccessCallback or RemoteSuccessCallback. " +
                    "See JavaDoc for migration guide. Current success type: " + data.getClass().getSimpleName());
            
            JSONObject jsonObject = new JSONObject();
            try {
                if (data instanceof IterableResponseObject.RemoteSuccess) {
                    JSONObject originalJson = ((IterableResponseObject.RemoteSuccess) data).getResponseJson();
                    jsonObject = new JSONObject(originalJson.toString());
                } else if (data instanceof IterableResponseObject.AuthTokenSuccess) {
                    jsonObject.put("newAuthToken", ((IterableResponseObject.AuthTokenSuccess) data).getAuthToken());
                } else if (data instanceof IterableResponseObject.LocalSuccess) {
                    jsonObject.put("message", data.getMessage());
                } else {
                    jsonObject.put("message", data.getMessage());
                }
            } catch (JSONException e) {
                IterableLogger.e("IterableHelper", "Error creating JSON for deprecated SuccessHandler callback", e);
            }
            
            onSuccess(jsonObject);
        }
    }

    /**
     * Generic callback interface for successful SDK operations.
     * <p>
     * <b>When to use this callback:</b>
     * <ul>
     *   <li>When you want to proceed on <b>any</b> type of success (remote, local, or auth token)</li>
     *   <li>When you don't need to access specific response data (like JSON from API or auth tokens)</li>
     *   <li>When you just need to know the operation completed successfully, regardless of how</li>
     * </ul>
     * <p>
     * <b>Example use cases:</b>
     * <ul>
     *   <li>{@code setEmail()} - Can complete locally (if autoPushRegistration is false) or remotely (via registerForPush)</li>
     *   <li>{@code setUserId()} - May complete locally or trigger remote operations</li>
     *   <li>Any operation where you just need confirmation of success without caring about the response details</li>
     * </ul>
     * <p>
     * <b>When to use specialized callbacks instead:</b>
     * <ul>
     *   <li>Use {@link RemoteSuccessCallback} if you need to access JSON response data from the API</li>
     *   <li>Use {@link LocalSuccessCallback} if you only want to proceed when no remote call was made</li>
     *   <li>Use {@link AuthTokenCallback} if you need to access the authentication token</li>
     * </ul>
     *
     * @see RemoteSuccessCallback
     * @see LocalSuccessCallback
     * @see AuthTokenCallback
     */
    public interface IterableSuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.Success data);
    }

    /**
     * Callback specifically for operations that make remote API calls and return JSON response data.
     * <p>
     * <b>When to use this callback:</b>
     * <ul>
     *   <li>When you need to access the JSON response data from the Iterable API</li>
     *   <li>When you want your callback to trigger <b>only</b> if a remote API call was made</li>
     *   <li>When the operation you're calling always makes a remote API request</li>
     * </ul>
     * <p>
     * <b>Example use cases:</b>
     * <ul>
     *   <li>{@code trackEvent()} - Always makes a remote API call with JSON response</li>
     *   <li>{@code updateUser()} - Makes a remote call to update user profile</li>
     *   <li>Operations where you need to inspect the server's response data</li>
     * </ul>
     * <p>
     * <b>Important:</b> If the operation completes locally (e.g., {@code setEmail} with autoPushRegistration disabled),
     * your callback will NOT be triggered. Use {@link IterableSuccessCallback} instead if you want to handle both
     * remote and local success cases.
     */
    public interface RemoteSuccessCallback extends IterableSuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.RemoteSuccess data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            // Dispatch to the specific method if it's the correct type
            if (data instanceof IterableResponseObject.RemoteSuccess) {
                onSuccess((IterableResponseObject.RemoteSuccess) data);
            } else {
                IterableLogger.w("IterableHelper", "RemoteSuccessCallback received unexpected success type: " + data.getClass().getSimpleName() +
                    ". This callback only triggers for remote API responses. Consider using IterableSuccessCallback if you want to handle all success types.");
            }
        }
    }

    /**
     * Callback specifically for authentication token operations.
     * <p>
     * <b>When to use this callback:</b>
     * <ul>
     *   <li>When you need to access the JWT authentication token from the response</li>
     *   <li>For operations that request or refresh auth tokens</li>
     *   <li>When implementing custom auth token handling or caching</li>
     * </ul>
     * <p>
     * <b>Example use cases:</b>
     * <ul>
     *   <li>Auth token refresh operations triggered by {@link IterableAuthHandler}</li>
     *   <li>Operations that return a new JWT token for the current user</li>
     *   <li>Custom auth token validation or storage logic</li>
     * </ul>
     * <p>
     * <b>Note:</b> This is primarily used internally by the SDK's auth system. Most applications
     * won't need to use this callback directly.
     */
    public interface AuthTokenCallback extends IterableSuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.AuthTokenSuccess data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            // Dispatch to the specific method if it's the correct type
            if (data instanceof IterableResponseObject.AuthTokenSuccess) {
                onSuccess((IterableResponseObject.AuthTokenSuccess) data);
            } else {
                IterableLogger.w("IterableHelper", "AuthTokenCallback received unexpected success type: " + data.getClass().getSimpleName() +
                    ". This callback is for auth token operations only.");
            }
        }
    }

    /**
     * Callback specifically for operations that complete locally without making a remote API call.
     * <p>
     * <b>When to use this callback:</b>
     * <ul>
     *   <li>When you want your callback to trigger <b>only</b> if the operation completed locally</li>
     *   <li>When you want to distinguish between operations that hit the server vs. those that don't</li>
     *   <li>For testing or debugging purposes to verify no remote call was made</li>
     * </ul>
     * <p>
     * <b>Example use cases:</b>
     * <ul>
     *   <li>{@code setEmail()} with {@code autoPushRegistration = false} - Updates locally without calling the API</li>
     *   <li>Operations that only update local state or keychain</li>
     *   <li>Scenarios where you want different behavior based on whether a network call occurred</li>
     * </ul>
     * <p>
     * <b>Important:</b> If the operation makes a remote call (e.g., {@code setEmail} with autoPushRegistration enabled),
     * your callback will NOT be triggered. Use {@link IterableSuccessCallback} instead if you want to handle both
     * local and remote success cases.
     */
    public interface LocalSuccessCallback extends IterableSuccessCallback {
        void onSuccess(@NonNull IterableResponseObject.LocalSuccess data);

        @Override
        default void onSuccess(@NonNull IterableResponseObject.Success data) {
            // Dispatch to the specific method if it's the correct type
            if (data instanceof IterableResponseObject.LocalSuccess) {
                onSuccess((IterableResponseObject.LocalSuccess) data);
            } else {
                IterableLogger.w("IterableHelper", "LocalSuccessCallback received unexpected success type: " + data.getClass().getSimpleName() +
                    ". This callback only triggers for local-only operations. Consider using IterableSuccessCallback if you want to handle all success types.");
            }
        }
    }

    public interface FailureHandler {
        void onFailure(@NonNull String reason, @Nullable JSONObject data);
    }

    /**
     * @deprecated Use {@link AuthTokenCallback} instead for better type safety and clarity.
     */
    @Deprecated
    public interface SuccessAuthHandler {
        void onSuccess(@NonNull String authToken);
    }
}