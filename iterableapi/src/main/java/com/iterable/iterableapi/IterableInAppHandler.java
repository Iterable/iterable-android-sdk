package com.iterable.iterableapi;

import java.util.List;

public abstract class IterableInAppHandler {
    enum InAppResponse {
        SHOW,
        SKIP
    }

    abstract InAppResponse onNewInApp(IterableInAppMessage message);
    void onNewBatch(List<IterableInAppMessage> messages) {
        for (IterableInAppMessage message : messages) {
            if (onNewInApp(message) == InAppResponse.SHOW) {
                IterableApi.getInstance().getInAppManager().showMessage(message);
                return;
            }
        }
    }
}
