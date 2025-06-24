package com.iterable.iterableapi

enum class IterableInAppLocation {
    IN_APP {
        override fun toString(): String {
            return "in-app"
        }
    },
    INBOX {
        override fun toString(): String {
            return "inbox"
        }
    }
}
