package com.iterable.iterableapi.ui.inbox;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableInAppMessage;

import java.util.Comparator;

/**
 * An interface to specify custom ordering of Inbox messages
 * See {@link Comparator}
 */
public interface IterableInboxComparator extends Comparator<IterableInAppMessage>{
    int compare(@NonNull IterableInAppMessage message1, @NonNull IterableInAppMessage message2);
}
