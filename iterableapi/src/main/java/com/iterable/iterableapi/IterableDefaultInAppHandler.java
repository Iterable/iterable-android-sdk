package com.iterable.iterableapi;

public class IterableDefaultInAppHandler extends IterableInAppHandler {
    @Override
    InAppResponse onNewInApp(IterableInAppMessage message) {
        return InAppResponse.SHOW;
    }
}
