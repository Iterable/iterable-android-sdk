package com.iterable.iterableapi;

import static com.iterable.iterableapi.IterableConstants.ENDPOINT_DISABLE_DEVICE;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executor;

final class IterablePushRegistrationRequestProcessor {
    private static final String TAG = "IterablePushRegistrationRequestProcessor";

    interface RetryScheduler {
        void schedule(Runnable runnable, long delayMs);
    }

    private static final RetryScheduler SDK_RETRY_SCHEDULER =
            (runnable, delayMs) ->
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMs);

    private final Executor requestExecutor;
    private final Executor callbackExecutor;
    private final RetryScheduler retryScheduler;

    IterablePushRegistrationRequestProcessor() {
        this(
                IterableExecutors.push(),
                IterableExecutors.main(),
                SDK_RETRY_SCHEDULER
        );
    }

    IterablePushRegistrationRequestProcessor(
            Executor requestExecutor,
            Executor callbackExecutor,
            RetryScheduler retryScheduler
    ) {
        this.requestExecutor = requestExecutor;
        this.callbackExecutor = callbackExecutor;
        this.retryScheduler = retryScheduler;
    }

    void processPostRequest(
            @Nullable String apiKey,
            @NonNull String resourcePath,
            @NonNull JSONObject json,
            @Nullable String authToken,
            @Nullable IterableHelper.SuccessHandler onSuccess,
            @Nullable IterableHelper.FailureHandler onFailure,
            @NonNull PushRegistrationRetryState retryState
    ) {
        IterableApiRequest request = new IterableApiRequest(
                apiKey,
                resourcePath,
                addCreatedAtToJson(json),
                IterableApiRequest.POST,
                authToken,
                onSuccess,
                onFailure
        );
        execute(request, retryState, 0, false);
    }

    void scheduleRetry(
            IterableApiRequest request,
            @NonNull PushRegistrationRetryState retryState,
            int retryCount,
            long delayMs
    ) {
        retryScheduler.schedule(
                () -> execute(request, retryState, retryCount, true),
                delayMs
        );
    }

    void retryWithNewAuthToken(
            String newAuthToken,
            IterableApiRequest request,
            @NonNull PushRegistrationRetryState retryState
    ) {
        IterableApiRequest retryRequest = new IterableApiRequest(
                request.apiKey,
                request.resourcePath,
                request.json,
                request.requestType,
                newAuthToken,
                request.legacyCallback
        );
        execute(retryRequest, retryState, 0, true);
    }

    void deliverResult(Runnable runnable) {
        callbackExecutor.execute(runnable);
    }

    private void execute(
            IterableApiRequest request,
            @NonNull PushRegistrationRetryState retryState,
            int retryCount,
            boolean retry
    ) {
        requestExecutor.execute(new IterablePushRegistrationRequestTask(
                request,
                retryState,
                retryCount,
                retry,
                this
        ));
    }

    private JSONObject addCreatedAtToJson(JSONObject json) {
        try {
            long createdAt;
            if (json.has(IterableConstants.KEY_CREATED_AT)) {
                createdAt = Long.parseLong(
                        json.getString(IterableConstants.KEY_CREATED_AT)
                );
            } else {
                createdAt = new Date().getTime() / 1000;
            }
            json.put(IterableConstants.KEY_CREATED_AT, createdAt);
        } catch (JSONException | NumberFormatException e) {
            IterableLogger.e(
                    TAG,
                    "Could not add createdAt timestamp to json object"
            );
        }
        return json;
    }
}

final class IterablePushRegistrationRequestTask implements Runnable {
    private final IterableApiRequest request;
    private final PushRegistrationRetryState retryState;
    private final int retryCount;
    private final boolean retry;
    private final IterablePushRegistrationRequestProcessor requestProcessor;

    IterablePushRegistrationRequestTask(
            IterableApiRequest request,
            PushRegistrationRetryState retryState,
            int retryCount,
            boolean retry,
            IterablePushRegistrationRequestProcessor requestProcessor
    ) {
        this.request = request;
        this.retryState = retryState;
        this.retryCount = retryCount;
        this.retry = retry;
        this.requestProcessor = requestProcessor;
    }

    @Override
    public void run() {
        if (retry && !retryState.canRetry()) {
            return;
        }

        IterableApiResponse response = IterableRequestTask.executeApiRequest(
                request,
                (newAuthToken, originalRequest) ->
                        requestProcessor.retryWithNewAuthToken(
                                newAuthToken,
                                originalRequest,
                                retryState
                        )
        );
        requestProcessor.deliverResult(() -> handleResponse(response));
    }

    void handleResponse(IterableApiResponse response) {
        if (response == null || (retry && !retryState.canRetry())) {
            return;
        }

        if (shouldRetry(response)) {
            int nextRetryCount = retryCount + 1;
            long delayMs = retryCount > 2
                    ? IterableRequestTask.RETRY_DELAY_MS * retryCount
                    : 0;
            requestProcessor.scheduleRetry(
                    request,
                    retryState,
                    nextRetryCount,
                    delayMs
            );
            return;
        }

        if (response.success) {
            handleSuccess(response);
        } else {
            handleFailure(response);
        }

        if (request.legacyCallback != null) {
            request.legacyCallback.execute(response.responseBody);
        }
    }

    private boolean shouldRetry(IterableApiResponse response) {
        return retryState.canRetry()
                && !response.success
                && response.responseCode >= 500
                && retryCount <= IterableRequestTask.MAX_RETRY_COUNT;
    }

    private void handleSuccess(IterableApiResponse response) {
        if (!Objects.equals(request.resourcePath, ENDPOINT_DISABLE_DEVICE)) {
            IterableApi.getInstance().getAuthManager().resetFailedAuth();
            IterableApi.getInstance().getAuthManager().pauseAuthRetries(false);
            IterableApi.getInstance().getAuthManager().setIsLastAuthTokenValid(true);
        }

        if (request.successCallback != null) {
            request.successCallback.onSuccess(response.responseJson);
        }
    }

    private void handleFailure(IterableApiResponse response) {
        if (request.failureCallback == null) {
            return;
        }

        JSONObject responseJson = response.responseJson;
        if (responseJson != null) {
            try {
                responseJson.put(
                        IterableConstants.HTTP_STATUS_CODE,
                        response.responseCode
                );
            } catch (JSONException ignored) {
            }
        }
        request.failureCallback.onFailure(response.errorMessage, responseJson);
    }
}

interface PushRegistrationRetryState {
    boolean canRetry();
}
