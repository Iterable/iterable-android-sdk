package com.iterable.iterableapi;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
}