package com.iterable.iterableapi;

public enum IterableInAppDeleteSource {

    INBOX_SWIPE_LEFT {
        @Override
        public String toString() {
            return "inbox-swipe-left";
        }
    },

    DELETE_BUTTON {
        @Override
        public String toString() {
            return "delete-button";
        }
    },

    UNKOWN {
        @Override
        public String toString() {
            return "unknown";
        }
    }



}
