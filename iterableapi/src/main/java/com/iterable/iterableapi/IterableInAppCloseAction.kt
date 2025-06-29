package com.iterable.iterableapi

enum class IterableInAppCloseAction {
    BACK {
        override fun toString(): String {
            return "back"
        }
    },

    LINK {
        override fun toString(): String {
            return "link"
        }
    },

    OTHER {
        override fun toString(): String {
            return "other"
        }
    }
}
