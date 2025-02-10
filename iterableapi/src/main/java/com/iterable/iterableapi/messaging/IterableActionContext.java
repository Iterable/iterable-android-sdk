package com.iterable.iterableapi.messaging;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableAction;

/**
 * An object representing the action to execute and the context it is executing in
 */
public class IterableActionContext {

    /** Action to execute */
    public final @NonNull IterableAction action;

    /** Source of the action: push notification, app link, etc. */
    public final @NonNull IterableActionSource source;

    /**
     * Create an {@link IterableActionContext} object with the given action and source
     * @param action Action to execute
     * @param source Source of the action
     */
    IterableActionContext(@NonNull IterableAction action, @NonNull IterableActionSource source) {
        this.action = action;
        this.source = source;
    }
}
