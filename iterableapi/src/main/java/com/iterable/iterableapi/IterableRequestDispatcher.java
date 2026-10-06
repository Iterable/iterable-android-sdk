package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class IterableRequestDispatcher {
    private static final String TAG = "RequestDispatcher";
    private static final String QUEUE_FULL_ERROR =
            "Iterable request queue is full";

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
            sdkDispatcher(IterableExecutors.request());
    private static final IterableRequestDispatcher PUSH_DISPATCHER =
            sdkDispatcher(IterableExecutors.push());
    private static final IterableRequestDispatcher OFFLINE_DISPATCHER =
            sdkDispatcher(IterableExecutors.offline());

    private final Executor requestExecutor;
    private final Executor callbackExecutor;
    private final RetryScheduler retryScheduler;

    static IterableRequestDispatcher online() {
        return ONLINE_DISPATCHER;
    }

    static IterableRequestDispatcher push() {
        return PUSH_DISPATCHER;
    }

    static IterableRequestDispatcher offline() {
        return OFFLINE_DISPATCHER;
    }

    IterableRequestDispatcher(
            Executor requestExecutor,
            Executor callbackExecutor,
            RetryScheduler retryScheduler
    ) {
        this.requestExecutor = requestExecutor;
        this.callbackExecutor = callbackExecutor;
        this.retryScheduler = retryScheduler;
    }

    void execute(IterableApiRequest request) {
        IterableRequestTask requestTask = new IterableRequestTask(request, 0, this);
        try {
            requestExecutor.execute(requestTask);
        } catch (RejectedExecutionException e) {
            IterableLogger.e(TAG, QUEUE_FULL_ERROR, e);
            deliverResult(() -> requestTask.handleResponse(queueFullResponse()));
        }
    }

    void executeForResponse(IterableApiRequest request, ResponseHandler responseHandler) {
        try {
            requestExecutor.execute(() -> responseHandler.onResponse(
                    IterableRequestTask.executeApiRequest(request, this)
            ));
        } catch (RejectedExecutionException e) {
            IterableLogger.e(TAG, QUEUE_FULL_ERROR, e);
            deliverResult(() -> responseHandler.onResponse(queueFullResponse()));
        }
    }

    void executeRetry(IterableApiRequest request, int retryCount) {
        if (request.canRetry()) {
            IterableRequestTask requestTask =
                    new IterableRequestTask(request, retryCount, this, true);
            try {
                requestExecutor.execute(requestTask);
            } catch (RejectedExecutionException e) {
                IterableLogger.e(TAG, QUEUE_FULL_ERROR, e);
                deliverResult(() -> {
                    if (request.canRetry()) {
                        requestTask.handleResponse(queueFullResponse());
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
        return new IterableRequestDispatcher(
                requestExecutor,
                IterableExecutors.main(),
                SDK_RETRY_SCHEDULER
        );
    }

    private static IterableApiResponse queueFullResponse() {
        return IterableApiResponse.failure(0, null, null, QUEUE_FULL_ERROR);
    }
}

final class IterableRequestDispatchers {
    private static final IterableRequestDispatchers SDK_DISPATCHERS =
            new IterableRequestDispatchers(
                    IterableRequestDispatcher.online(),
                    IterableRequestDispatcher.push(),
                    IterableRequestDispatcher.offline()
            );

    private final IterableRequestDispatcher online;
    private final IterableRequestDispatcher push;
    private final IterableRequestDispatcher offline;

    static IterableRequestDispatchers sdk() {
        return SDK_DISPATCHERS;
    }

    static IterableRequestDispatchers same(IterableRequestDispatcher dispatcher) {
        return new IterableRequestDispatchers(dispatcher, dispatcher, dispatcher);
    }

    IterableRequestDispatchers(
            IterableRequestDispatcher online,
            IterableRequestDispatcher push,
            IterableRequestDispatcher offline
    ) {
        this.online = online;
        this.push = push;
        this.offline = offline;
    }

    IterableRequestDispatcher online() {
        return online;
    }

    IterableRequestDispatcher push() {
        return push;
    }

    IterableRequestDispatcher offline() {
        return offline;
    }
}
