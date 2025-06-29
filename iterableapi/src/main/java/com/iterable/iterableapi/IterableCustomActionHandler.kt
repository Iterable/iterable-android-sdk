package com.iterable.iterableapi

import androidx.annotation.NonNull

/**
 * Custom action handler interface
 */
interface IterableCustomActionHandler {

    /**
     * Callback called for custom actions from push notifications
     * @param action [IterableAction] object containing action payload
     * @param actionContext  The action context
     * @return Boolean value. Reserved for future use.
     */
    fun handleIterableCustomAction(@NonNull action: IterableAction, @NonNull actionContext: IterableActionContext): Boolean

}
