package com.iterable.iterableapi.ui.inbox;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableInAppMessage;

/**
 * A filter interface for Inbox messages
 */
public interface IterableInboxFilter {
    /**
     * @param message Inbox message
     * @return true to keep the message, false to exclude
     */
    boolean filter(@NonNull IterableInAppMessage message);
}
