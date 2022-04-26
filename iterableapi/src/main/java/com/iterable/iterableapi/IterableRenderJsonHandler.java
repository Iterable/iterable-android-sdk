package com.iterable.iterableapi;

import org.json.JSONObject;

public interface IterableRenderJsonHandler {
    void showMessage(JSONObject payload, IterableHelper.IterableUrlCallback urlCallback);
}
