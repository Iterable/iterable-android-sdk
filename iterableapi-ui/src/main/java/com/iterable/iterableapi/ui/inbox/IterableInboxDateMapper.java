package com.iterable.iterableapi.ui.inbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.IterableInAppMessage;

import java.util.Comparator;

/**
 * An interface to override the the default display text for the creation date of an inbox message
 */
public interface IterableInboxDateMapper {
    /**
     * @param message Inbox message
     * @return The text to display for the message creation date, or null to not display it
     */
    @Nullable
    CharSequence mapMessageToDateString(@NonNull IterableInAppMessage message);
}
