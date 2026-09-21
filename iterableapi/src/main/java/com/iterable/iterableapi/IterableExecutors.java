package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class IterableExecutors {
    private static final int REQUEST_THREAD_COUNT =
            Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 4));
    private static final AtomicInteger REQUEST_THREAD_ID = new AtomicInteger();
    private static final Executor PUSH_EXECUTOR =
            newSingleThreadExecutor("IterablePushExecutor");
    private static final Executor DEEP_LINK_EXECUTOR =
            newSingleThreadExecutor("IterableDeepLinkExecutor");
    private static final Executor OFFLINE_EXECUTOR =
            newSingleThreadExecutor("IterableOfflineExecutor");
    private static final Executor REQUEST_EXECUTOR = Executors.newFixedThreadPool(
            REQUEST_THREAD_COUNT,
            runnable -> newThread(
                    runnable,
                    "IterableRequestExecutor-" + REQUEST_THREAD_ID.incrementAndGet()
            )
    );
    private static final Executor MAIN_EXECUTOR =
            runnable -> new Handler(Looper.getMainLooper()).post(runnable);

    private IterableExecutors() {
    }

    static Executor push() {
        return PUSH_EXECUTOR;
    }

    static Executor deepLink() {
        return DEEP_LINK_EXECUTOR;
    }

    static Executor request() {
        return REQUEST_EXECUTOR;
    }

    static Executor offline() {
        return OFFLINE_EXECUTOR;
    }

    static Executor main() {
        return MAIN_EXECUTOR;
    }

    private static Executor newSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(
                runnable -> newThread(runnable, threadName)
        );
    }

    private static Thread newThread(Runnable runnable, String threadName) {
        Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}
