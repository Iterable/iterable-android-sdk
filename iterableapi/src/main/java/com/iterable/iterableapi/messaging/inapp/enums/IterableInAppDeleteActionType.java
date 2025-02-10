package com.iterable.iterableapi.messaging.inapp.enums;

public enum IterableInAppDeleteActionType {

    INBOX_SWIPE {
        @Override
        public String toString() {
            return "inbox-swipe";
        }
    },

    DELETE_BUTTON {
        @Override
        public String toString() {
            return "delete-button";
        }
    },

    OTHER {
        @Override
        public String toString() {
            return "other";
        }
    }

}
