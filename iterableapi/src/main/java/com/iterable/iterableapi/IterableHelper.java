package com.iterable.iterableapi;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.response.IterableAuthResponseObject;
import com.iterable.iterableapi.response.IterableResponseObject;

import org.json.JSONException;
import org.json.JSONObject;
import com.iterable.iterableapi.response.handlers.auth.IterableAuthCallbackHandlers;
import com.iterable.iterableapi.response.handlers.IterableCallbackHandlers;

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
     * @deprecated Use {@link } instead.
     * <p>
     * This interface is deprecated and will be removed in a future version.
     * Replace all usages with {@link IterableCallbackHandlers.SuccessCallback} for general success handling,
     * or {@link IterableCallbackHandlers.RemoteSuccessCallback} if you specifically need JSON response data from the API.
     * <p>
     * <b>Quick Migration (Recommended):</b> Replace {@code SuccessHandler} with {@code IterableSuccessCallback}
     * <pre>
     * // REPLACE THIS:
     * IterableHelper.SuccessHandler successHandler = data -> {
     *     // Your code here
     * };
     *
     * // WITH THIS (see JavaDoc on IterableSuccessCallback for more details):
     * IterableCallbackHandlers.SuccessCallback successHandler = data -> {
     *     // Your code here - see JavaDoc for accessing response data
     * };
     * </pre>
     *
     * @see IterableCallbackHandlers.SuccessCallback
     * @see IterableCallbackHandlers.RemoteSuccessCallback
     * @see IterableCallbackHandlers.LocalSuccessCallback
     */
    @Deprecated
    public interface SuccessHandler extends IterableCallbackHandlers.SuccessCallback {
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
                } else if (data instanceof IterableAuthResponseObject.Success) {
                    jsonObject.put("newAuthToken", ((IterableAuthResponseObject.Success) data).getAuthToken());
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



    public interface FailureHandler {
        void onFailure(@NonNull String reason, @Nullable JSONObject data);
    }

    /**
     * @deprecated Use {@link IterableAuthCallbackHandlers.AuthTokenCallback} instead for better type safety and clarity.
     */
    @Deprecated
    public interface SuccessAuthHandler {
        void onSuccess(@NonNull String authToken);
    }
}