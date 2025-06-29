package com.iterable.iterableapi

/**
 * Enum representing the source of the action: push notification, app link, etc.
 */
enum class IterableActionSource {
    /** Push Notification */
    PUSH,

    /** App Link */
    APP_LINK,

    /** In-App Message */
    IN_APP,

    /** Embedded Message */
    EMBEDDED
}
