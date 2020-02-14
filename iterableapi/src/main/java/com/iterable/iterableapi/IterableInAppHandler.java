package com.iterable.iterableapi;

import android.support.annotation.NonNull;

public interface IterableInAppHandler {
    enum InAppResponse {
        SHOW,
        SKIP
    }

    @NonNull
    InAppResponse onNewInApp(@NonNull IterableInAppMessage message);
}
