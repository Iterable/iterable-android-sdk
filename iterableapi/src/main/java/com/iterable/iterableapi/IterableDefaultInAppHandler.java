package com.iterable.iterableapi;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.messaging.inapp.interfaces.IterableInAppHandler;

public class IterableDefaultInAppHandler implements IterableInAppHandler {
    @NonNull
    @Override
    public InAppResponse onNewInApp(@NonNull IterableInAppMessage message) {
        return InAppResponse.SHOW;
    }
}
