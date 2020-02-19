package com.iterable.iterableapi;

import android.support.annotation.NonNull;

public class IterableDefaultInAppHandler implements IterableInAppHandler {
    @NonNull
    @Override
    public InAppResponse onNewInApp(@NonNull IterableInAppMessage message) {
        return InAppResponse.SHOW;
    }
}
