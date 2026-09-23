package com.iterable.iterableapi;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class IterableExecutors {
    private static final Executor PUSH_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "IterablePushExecutor");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    });

    private IterableExecutors() {
    }

    static Executor push() {
        return PUSH_EXECUTOR;
    }
}
