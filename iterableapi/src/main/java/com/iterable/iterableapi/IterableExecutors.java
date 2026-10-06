package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class IterableExecutors {
    // HttpURLConnection is blocking I/O, so ordinary API work uses a bounded
    // multi-thread pool rather than the serial executors used for ordered work.
    static final int REQUEST_THREAD_COUNT = 8;
    // Matches the historical AsyncTask queue bound without inheriting its
    // platform-version-dependent thread-pool behavior.
    static final int REQUEST_QUEUE_CAPACITY = 128;
    private static final long REQUEST_THREAD_KEEP_ALIVE_SECONDS = 30;
    private static final AtomicInteger REQUEST_THREAD_ID = new AtomicInteger();
    private static final Executor PUSH_EXECUTOR =
            newSingleThreadExecutor("IterablePushExecutor");
    private static final Executor DEEP_LINK_EXECUTOR =
            newSingleThreadExecutor("IterableDeepLinkExecutor");
    private static final Executor OFFLINE_EXECUTOR =
            newSingleThreadExecutor("IterableOfflineExecutor");
    private static final Executor REQUEST_EXECUTOR =
            newRequestExecutor(REQUEST_THREAD_COUNT, REQUEST_QUEUE_CAPACITY);
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

    static ThreadPoolExecutor newRequestExecutor(int threadCount, int queueCapacity) {
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
                new ThreadPoolExecutor.AbortPolicy()
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
