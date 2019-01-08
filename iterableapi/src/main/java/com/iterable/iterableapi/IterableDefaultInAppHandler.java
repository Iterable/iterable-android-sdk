package com.iterable.iterableapi;

public class IterableDefaultInAppHandler implements IterableInAppHandler {
    @Override
    public InAppResponse onNewInApp(IterableInAppMessage message) {
        return InAppResponse.SHOW;
    }
}
