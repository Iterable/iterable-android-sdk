package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class IterableExecutors {
    // HttpURLConnection is blocking I/O, so ordinary API work uses a bounded
    // multi-thread pool rather than the serial executors used for ordered work.
    static final int REQUEST_THREAD_COUNT = 8;
    // Bounds the primary pool without inheriting AsyncTask's
    // platform-version-dependent thread-pool behavior.
    static final int REQUEST_QUEUE_CAPACITY = 128;
    private static final long REQUEST_THREAD_KEEP_ALIVE_SECONDS = 30;
    private static final int REQUEST_OVERFLOW_THREAD_COUNT = 5;
    private static final long REQUEST_OVERFLOW_KEEP_ALIVE_SECONDS = 3;
    private static final AtomicInteger REQUEST_THREAD_ID = new AtomicInteger();
    private static final AtomicInteger REQUEST_OVERFLOW_THREAD_ID = new AtomicInteger();
    private static final Executor PUSH_EXECUTOR =
            newSingleThreadExecutor("IterablePushExecutor");
    private static final Executor DEEP_LINK_EXECUTOR =
            newSingleThreadExecutor("IterableDeepLinkExecutor");
    private static final Executor SERIAL_REQUEST_EXECUTOR =
            newSingleThreadExecutor("IterableSerialRequestExecutor");
    private static final Executor OFFLINE_STORED_EXECUTOR =
            newSingleThreadExecutor("IterableOfflineStoredExecutor");
    // AsyncTask's concurrent executor used a backup queue instead of dropping
    // work when its primary pool was saturated. Preserve that delivery behavior.
    private static final Executor REQUEST_OVERFLOW_EXECUTOR =
            newRequestOverflowExecutor(REQUEST_OVERFLOW_THREAD_COUNT);
    private static final Executor REQUEST_EXECUTOR =
            newRequestExecutor(
                    REQUEST_THREAD_COUNT,
                    REQUEST_QUEUE_CAPACITY,
                    REQUEST_OVERFLOW_EXECUTOR
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

    static Executor offlineImmediate() {
        return SERIAL_REQUEST_EXECUTOR;
    }

    static Executor requestRetry() {
        return SERIAL_REQUEST_EXECUTOR;
    }

    static Executor offlineStored() {
        return OFFLINE_STORED_EXECUTOR;
    }

    static Executor main() {
        return MAIN_EXECUTOR;
    }

    private static Executor newSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(
                runnable -> newThread(runnable, threadName)
        );
    }

    static ThreadPoolExecutor newRequestExecutor(int threadCount, int queueCapacity) {
        return newRequestExecutor(threadCount, queueCapacity, REQUEST_OVERFLOW_EXECUTOR);
    }

    static ThreadPoolExecutor newRequestExecutor(
            int threadCount,
            int queueCapacity,
            Executor overflowExecutor
    ) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadCount,
                threadCount,
                REQUEST_THREAD_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> newThread(
                        runnable,
                        "IterableRequestExecutor-" + REQUEST_THREAD_ID.incrementAndGet()
                ),
                (runnable, ignored) -> overflowExecutor.execute(runnable)
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    static ThreadPoolExecutor newRequestOverflowExecutor(int threadCount) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadCount,
                threadCount,
                REQUEST_OVERFLOW_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> newThread(
                        runnable,
                        "IterableRequestOverflowExecutor-"
                                + REQUEST_OVERFLOW_THREAD_ID.incrementAndGet()
                )
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static Thread newThread(Runnable runnable, String threadName) {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            runnable.run();
        }, threadName);
        thread.setDaemon(true);
        return thread;
    }
}
