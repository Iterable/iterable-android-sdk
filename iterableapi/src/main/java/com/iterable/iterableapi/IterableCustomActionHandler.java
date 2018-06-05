package com.iterable.iterableapi;

/**
 * Custom action handler interface
 */
public interface IterableCustomActionHandler {

    /**
     * Callback called for custom actions from push notifications
     * @param action {@link IterableAction} object containing action payload
     * @return Boolean value. Reserved for future use.
     */
    boolean handleIterableCustomAction(IterableAction action);

}
