package com.iterable.iterableapi.core.interfaces;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.iterable.iterableapi.messaging.IterableActionContext;

/**
 * Custom URL handler interface
 */
public interface IterableUrlHandler {

    /**
     * Callback called for a deeplink action. Return YES to override default behavior
     * @param uri     Deeplink URL
     * @param actionContext  The action context
     * @return Boolean value. Return YES if the URL was handled to override default behavior.
     */
    boolean handleIterableURL(@NonNull Uri uri, @NonNull IterableActionContext actionContext);

}
