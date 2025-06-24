package com.iterable.iterableapi

import android.net.Uri
import androidx.annotation.NonNull

/**
 * Custom URL handler interface
 */
interface IterableUrlHandler {

    /**
     * Callback called for a deeplink action. Return YES to override default behavior
     * @param uri     Deeplink URL
     * @param actionContext  The action context
     * @return Boolean value. Return YES if the URL was handled to override default behavior.
     */
    fun handleIterableURL(@NonNull uri: Uri, @NonNull actionContext: IterableActionContext): Boolean

}
