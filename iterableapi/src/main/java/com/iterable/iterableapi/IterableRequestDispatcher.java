package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class IterableRequestDispatcher {
    private static final String TAG = "RequestDispatcher";
    private static final String REQUEST_REJECTED_ERROR =
            "Iterable request executor rejected work";

    interface RetryScheduler {
        void schedule(Runnable runnable, long delayMs);
    }

    interface ResponseHandler {
        void onResponse(IterableApiResponse response);
    }

    private static final RetryScheduler SDK_RETRY_SCHEDULER =
            (runnable, delayMs) ->
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMs);
    private static final IterableRequestDispatcher ONLINE_DISPATCHER =
            sdkDispatcher(
                    IterableExecutors.request(),
                    IterableExecutors.requestRetry()
            );
    private static final IterableRequestDispatcher PUSH_DISPATCHER =
            sdkDispatcher(IterableExecutors.push());
    private static final IterableRequestDispatcher OFFLINE_IMMEDIATE_DISPATCHER =
            sdkDispatcher(IterableExecutors.offlineImmediate());
    private static final IterableRequestDispatcher OFFLINE_STORED_DISPATCHER =
            sdkDispatcher(IterableExecutors.offlineStored());

    private final Executor requestExecutor;
    private final Executor retryExecutor;
    private final Executor callbackExecutor;
    private final RetryScheduler retryScheduler;

    static IterableRequestDispatcher online() {
        return ONLINE_DISPATCHER;
    }

    static IterableRequestDispatcher push() {
        return PUSH_DISPATCHER;
    }

    static IterableRequestDispatcher offlineImmediate() {
        return OFFLINE_IMMEDIATE_DISPATCHER;
    }

    static IterableRequestDispatcher offlineStored() {
        return OFFLINE_STORED_DISPATCHER;
    }

    IterableRequestDispatcher(
            Executor requestExecutor,
            Executor callbackExecutor,
            RetryScheduler retryScheduler
    ) {
        this(requestExecutor, requestExecutor, callbackExecutor, retryScheduler);
    }

    IterableRequestDispatcher(
            Executor requestExecutor,
            Executor retryExecutor,
            Executor callbackExecutor,
            RetryScheduler retryScheduler
    ) {
        this.requestExecutor = requestExecutor;
        this.retryExecutor = retryExecutor;
        this.callbackExecutor = callbackExecutor;
        this.retryScheduler = retryScheduler;
    }

    void execute(IterableApiRequest request) {
        IterableRequestTask requestTask = new IterableRequestTask(request, 0, this);
        try {
            requestExecutor.execute(requestTask);
        } catch (RejectedExecutionException e) {
            IterableLogger.e(TAG, REQUEST_REJECTED_ERROR, e);
            deliverResult(() -> requestTask.handleResponse(rejectedRequestResponse()));
        }
    }

    void executeForResponse(IterableApiRequest request, ResponseHandler responseHandler) {
        try {
            requestExecutor.execute(() -> responseHandler.onResponse(
                    IterableRequestTask.executeApiRequest(request, this)
            ));
        } catch (RejectedExecutionException e) {
            IterableLogger.e(TAG, REQUEST_REJECTED_ERROR, e);
            deliverResult(() -> responseHandler.onResponse(rejectedRequestResponse()));
        }
    }

    void executeRetry(IterableApiRequest request, int retryCount) {
        if (request.canRetry()) {
            IterableRequestTask requestTask =
                    new IterableRequestTask(request, retryCount, this, true);
            try {
                retryExecutor.execute(requestTask);
            } catch (RejectedExecutionException e) {
                IterableLogger.e(TAG, REQUEST_REJECTED_ERROR, e);
                deliverResult(() -> {
                    if (request.canRetry()) {
                        requestTask.handleResponse(rejectedRequestResponse());
                    }
                });
            }
        }
    }

    void deliverResult(Runnable runnable) {
        callbackExecutor.execute(runnable);
    }

    void retry(IterableApiRequest request, int retryCount, long delayMs) {
        retryScheduler.schedule(() -> executeRetry(request, retryCount), delayMs);
    }

    private static IterableRequestDispatcher sdkDispatcher(Executor requestExecutor) {
        return sdkDispatcher(requestExecutor, requestExecutor);
    }

    private static IterableRequestDispatcher sdkDispatcher(
            Executor requestExecutor,
            Executor retryExecutor
    ) {
        return new IterableRequestDispatcher(
                requestExecutor,
                retryExecutor,
                IterableExecutors.main(),
                SDK_RETRY_SCHEDULER
        );
    }

    private static IterableApiResponse rejectedRequestResponse() {
        return IterableApiResponse.failure(0, null, null, REQUEST_REJECTED_ERROR);
    }
}

final class IterableRequestDispatchers {
    private static final IterableRequestDispatchers SDK_DISPATCHERS =
            new IterableRequestDispatchers(
                    IterableRequestDispatcher.online(),
                    IterableRequestDispatcher.push(),
                    IterableRequestDispatcher.offlineImmediate(),
                    IterableRequestDispatcher.offlineStored()
            );

    private final IterableRequestDispatcher online;
    private final IterableRequestDispatcher push;
    private final IterableRequestDispatcher offlineImmediate;
    private final IterableRequestDispatcher offlineStored;

    static IterableRequestDispatchers sdk() {
        return SDK_DISPATCHERS;
    }

    static IterableRequestDispatchers same(IterableRequestDispatcher dispatcher) {
        return new IterableRequestDispatchers(
                dispatcher,
                dispatcher,
                dispatcher,
                dispatcher
        );
    }

    IterableRequestDispatchers(
            IterableRequestDispatcher online,
            IterableRequestDispatcher push,
            IterableRequestDispatcher offlineImmediate,
            IterableRequestDispatcher offlineStored
    ) {
        this.online = online;
        this.push = push;
        this.offlineImmediate = offlineImmediate;
        this.offlineStored = offlineStored;
    }

    IterableRequestDispatcher online() {
        return online;
    }

    IterableRequestDispatcher push() {
        return push;
    }

    IterableRequestDispatcher offlineImmediate() {
        return offlineImmediate;
    }

    IterableRequestDispatcher offlineStored() {
        return offlineStored;
    }
}
