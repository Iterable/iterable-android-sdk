package com.iterable.iterableapi.ui.inbox

import androidx.annotation.NonNull

import com.iterable.iterableapi.IterableInAppMessage

/**
 * A filter interface for Inbox messages
 */
interface IterableInboxFilter {
    /**
     * @param message Inbox message
     * @return true to keep the message, false to exclude
     */
    fun filter(@NonNull message: IterableInAppMessage): Boolean
}
