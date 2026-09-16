package com.iterable.iterableapi;

import java.util.concurrent.Executor;

final class IterablePushRegistration {
    private final Executor executor;

    IterablePushRegistration() {
        this(IterableExecutors.serial());
    }

    IterablePushRegistration(Executor executor) {
        this.executor = executor;
    }

    void executePushRegistrationTask(IterablePushRegistrationData data) {
        executor.execute(new IterablePushRegistrationTask(data));
    }
}
