package com.iterable.iterableapi;

import org.json.JSONObject;

class IterableRenderJsonDisplayer extends IterableMessageDisplayer {
    private static IterableRenderJsonHandler renderJsonHandler;

    IterableRenderJsonDisplayer(IterableActivityMonitor activityMonitor, IterableRenderJsonHandler renderJsonHandler) {
        super(activityMonitor);
        this.renderJsonHandler = renderJsonHandler;
    }

    static boolean showMessage(JSONObject messagePayload, IterableHelper.IterableUrlCallback clickCallback) {
        return renderJsonHandler.showMessage(messagePayload, clickCallback);
    }
}
