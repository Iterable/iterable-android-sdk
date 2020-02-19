package com.iterable.iterableapi;

import androidx.annotation.NonNull;

public interface IterableInAppHandler {
    enum InAppResponse {
        SHOW,
        SKIP
    }

    @NonNull
    InAppResponse onNewInApp(@NonNull IterableInAppMessage message);
}
