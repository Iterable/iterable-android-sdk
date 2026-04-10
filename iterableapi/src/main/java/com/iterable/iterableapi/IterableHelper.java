package com.iterable.iterableapi;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableHelper {

    /**
     * Interface to handle Iterable Actions
     */
    public interface IterableActionHandler {
        void execute(@Nullable String data);
    }

    public interface IterableUrlCallback {
        void execute(@Nullable Uri url);
    }

    public interface SuccessHandler {
        void onSuccess(@NonNull JSONObject data);
    }

    public interface FailureHandler {
        void onFailure(@NonNull String reason, @Nullable JSONObject data);
    }

    public interface SuccessAuthHandler {
        void onSuccess(@NonNull String authToken);
    }
}

class IterableResponse {
    static final JSONObject setEmailLocalSuccessResponse;
    static final JSONObject setReadLocalSuccessResponse;

    static {
        JSONObject emailResponse = new JSONObject();
        JSONObject readResponse = new JSONObject();
        try {
            emailResponse.put("message", "setEmail was completed locally.");
            readResponse.put("message", "setRead was completed locally.");
        } catch (JSONException ignored) {}
        setEmailLocalSuccessResponse = emailResponse;
        setReadLocalSuccessResponse = readResponse;
    }
}