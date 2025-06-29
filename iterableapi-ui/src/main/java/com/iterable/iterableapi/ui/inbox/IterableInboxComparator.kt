package com.iterable.iterableapi.ui.inbox

import androidx.annotation.NonNull

import com.iterable.iterableapi.IterableInAppMessage

import java.util.Comparator

/**
 * An interface to specify custom ordering of Inbox messages
 * See [Comparator]
 */
interface IterableInboxComparator : Comparator<IterableInAppMessage> {
    override fun compare(@NonNull message1: IterableInAppMessage, @NonNull message2: IterableInAppMessage): Int
}
