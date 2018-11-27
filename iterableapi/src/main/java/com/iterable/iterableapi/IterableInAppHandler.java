package com.iterable.iterableapi;

import java.util.List;

public interface IterableInAppHandler {
    enum InAppResponse {
        SHOW,
        SKIP
    }

    InAppResponse onNewInApp(IterableInAppMessage message);
}
