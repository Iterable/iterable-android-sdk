package com.iterable.iterableapi.messaging.inapp.interfaces;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableInAppMessage;

public interface IterableInAppHandler {
    enum InAppResponse {
        SHOW,
        SKIP
    }

    @NonNull
    InAppResponse onNewInApp(@NonNull IterableInAppMessage message);
}
