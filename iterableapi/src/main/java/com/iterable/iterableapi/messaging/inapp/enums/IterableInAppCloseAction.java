package com.iterable.iterableapi.messaging.inapp.enums;

public enum IterableInAppCloseAction {
    BACK {
        @Override
        public String toString() {
            return "back";
        }
    },

    LINK {
        @Override
        public String toString() {
            return "link";
        }
    },

    OTHER {
        @Override
        public String toString() {
            return "other";
        }
    }
}
