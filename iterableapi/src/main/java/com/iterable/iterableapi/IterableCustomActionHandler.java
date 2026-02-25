package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Custom action handler interface
 */
public interface IterableCustomActionHandler {

    /**
     * Callback called for custom actions from push notifications
     * @param action {@link IterableAction} object containing action payload
     * @param actionContext  The action context
     * @return true if your app handled the action, false otherwise
     */
    boolean handleIterableCustomAction(@NonNull IterableAction action, @NonNull IterableActionContext actionContext);

}
