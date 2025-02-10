package com.iterable.iterableapi.messaging.inapp.enums;

public enum IterableInAppLocation {
    IN_APP {
        @Override
        public String toString() {
            return "in-app";
        }
    },
    INBOX {
        @Override
        public String toString() {
            return "inbox";
        }
    }
}
