package com.iterable.iterableapi

enum class IterableInAppDeleteActionType {

    INBOX_SWIPE {
        override fun toString(): String {
            return "inbox-swipe"
        }
    },

    DELETE_BUTTON {
        override fun toString(): String {
            return "delete-button"
        }
    },

    OTHER {
        override fun toString(): String {
            return "other"
        }
    }

}
