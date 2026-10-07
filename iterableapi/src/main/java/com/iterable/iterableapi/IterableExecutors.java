package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class IterableExecutors {
    private static final Executor PUSH_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "IterablePushExecutor");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    });
    private static final Executor DEEP_LINK_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "IterableDeepLinkExecutor");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            });
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

    static Executor main() {
        return MAIN_EXECUTOR;
    }
}
