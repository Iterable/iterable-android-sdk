package com.iterable.iterableapi;

import org.json.JSONObject;

public class IterableAction {

    public static final String ACTION_TYPE_OPEN_URL    = "openUrl";

    private final JSONObject config;

    public String userInput;

    public IterableAction(JSONObject config) {
        if (config != null) {
            this.config = config;
        } else {
            this.config = new JSONObject();
        }
    }

    public String getType() {
        return config.optString("type", null);
    }

    public String getData() {
        return config.optString("data", null);
    }

    public boolean isOfType(String type) {
        return this.getType().equals(type);
    }
}
