package com.iterable.iterableapi.ui.inbox

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.iterable.iterableapi.IterableInAppMessage

/**
 * An interface to override the the default display text for the creation date of an inbox message
 */
interface IterableInboxDateMapper {
    /**
     * @param message Inbox message
     * @return The text to display for the message creation date, or null to not display it
     */
    @Nullable
    fun mapMessageToDateString(@NonNull message: IterableInAppMessage): CharSequence?
}
