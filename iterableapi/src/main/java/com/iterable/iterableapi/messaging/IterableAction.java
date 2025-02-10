package com.iterable.iterableapi.messaging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.core.interfaces.IterableCustomActionHandler;
import com.iterable.iterableapi.core.interfaces.IterableUrlHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  {@link IterableAction} represents an action defined as a response to user events.
 *  It is currently used in push notification actions (open push &amp; action buttons).
 */
public class IterableAction {

    /** Open the URL or deep link */
    public static final String ACTION_TYPE_OPEN_URL    = "openUrl";

    private final @NonNull JSONObject config;

    /** The text response typed by the user */
    public @Nullable String userInput;

    /**
     * Creates a new {@link IterableAction} from a JSON payload
     * @param config JSON containing action data
     */
    private IterableAction(@Nullable JSONObject config) {
        if (config != null) {
            this.config = config;
        } else {
            this.config = new JSONObject();
        }
    }

    @Nullable
    static IterableAction from(@Nullable JSONObject config) {
        if (config != null) {
            return new IterableAction(config);
        } else {
            return null;
        }
    }

    @Nullable
    static IterableAction actionOpenUrl(@Nullable String url) {
        if (url != null) {
            try {
                JSONObject config = new JSONObject();
                config.put("type", ACTION_TYPE_OPEN_URL);
                config.put("data", url);
                return new IterableAction(config);
            } catch (JSONException ignored) {}
        }
        return null;
    }

    @Nullable
    static IterableAction actionCustomAction(@NonNull String customActionName) {
        try {
            JSONObject config = new JSONObject();
            config.put("type", customActionName);
            return new IterableAction(config);
        } catch (JSONException ignored) {}
        return null;
    }

    /**
     * If {@link #ACTION_TYPE_OPEN_URL}, the SDK will call {@link IterableUrlHandler} and then try to
     * open the URL if the delegate returned `false` or was not set.
     *
     * For other types, {@link IterableCustomActionHandler} will be called.
     * @return Action type
     */
    @Nullable
    public String getType() {
        return config.optString("type", null);
    }

    /**
     * Additional data, its content depends on the action type
     * @return Additional data
     */
    @Nullable
    public String getData() {
        return config.optString("data", null);
    }

    /**
     * Checks whether this action is of a specific type
     * @param type Action type to match against
     * @return Boolean indicating whether the action type matches the one passed to this method
     */
    public boolean isOfType(@NonNull String type) {
        return this.getType() != null && this.getType().equals(type);
    }
}
