package com.iterable.iterableapi;

import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableHelper {

    /**
     * Interface to handle Iterable Actions
     */
    public interface IterableActionHandler {
        void execute(String data);
    }

    public interface SuccessHandler {
        void onSuccess(JSONObject data);
    }

    public interface FailureHandler {
        void onFailure(String reason, JSONObject data);
    }
}