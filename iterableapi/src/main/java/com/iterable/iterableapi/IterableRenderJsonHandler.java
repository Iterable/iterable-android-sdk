package com.iterable.iterableapi;

import org.json.JSONObject;

public interface IterableRenderJsonHandler {
    boolean showMessage(JSONObject payload, IterableHelper.IterableUrlCallback urlCallback);
}
