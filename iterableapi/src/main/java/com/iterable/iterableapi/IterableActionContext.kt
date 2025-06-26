package com.iterable.iterableapi

import androidx.annotation.NonNull

/**
 * An object representing the action to execute and the context it is executing in
 */
class IterableActionContext(
    /** Action to execute */
    @NonNull val action: IterableAction,
    /** Source of the action: push notification, app link, etc. */
    @NonNull val source: IterableActionSource
)
