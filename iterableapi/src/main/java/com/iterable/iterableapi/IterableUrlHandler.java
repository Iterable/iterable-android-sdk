package com.iterable.iterableapi;

import android.net.Uri;

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
    boolean handleIterableURL(Uri uri, IterableActionContext actionContext);

}
