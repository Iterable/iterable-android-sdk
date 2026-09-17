package com.iterable.iterableapi;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class IterableExecutors {
    private static final Executor SERIAL_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "IterableSerialExecutor");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    });
    private static final Executor MAIN_EXECUTOR =
            runnable -> new Handler(Looper.getMainLooper()).post(runnable);

    private IterableExecutors() {
    }

    static Executor serial() {
        return SERIAL_EXECUTOR;
    }

    static Executor main() {
        return MAIN_EXECUTOR;
    }
}
