package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

final class IterableRequestDispatcher {
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
        requestExecutor.execute(new IterableRequestTask(request, 0, this));
    }

    void executeForResponse(IterableApiRequest request, ResponseHandler responseHandler) {
        requestExecutor.execute(() -> responseHandler.onResponse(
                IterableRequestTask.executeApiRequest(request, this)
        ));
    }

    void executeRetry(IterableApiRequest request, int retryCount) {
        if (request.canRetry()) {
            requestExecutor.execute(new IterableRequestTask(request, retryCount, this, true));
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
