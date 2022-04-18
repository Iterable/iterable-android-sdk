package com.iterable.iterableapi;

import org.json.JSONObject;

public interface IterableRenderJsonHandler {
    JSONObject showMessage(JSONObject payload, IterableHelper.IterableUrlCallback urlCallback);
}
