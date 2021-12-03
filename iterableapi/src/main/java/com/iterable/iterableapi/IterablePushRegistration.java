package com.iterable.iterableapi;

import androidx.annotation.VisibleForTesting;

class IterablePushRegistration {

    @VisibleForTesting
    static IterablePushRegistrationImpl instance = new IterablePushRegistrationImpl();

    static void executePushRegistrationTask(IterablePushRegistrationData data) {
        instance.executePushRegistrationTask(data);
    }

    static class IterablePushRegistrationImpl {
        void executePushRegistrationTask(IterablePushRegistrationData data) {
            new IterablePushRegistrationTask().execute(data);
        }
    }
}
