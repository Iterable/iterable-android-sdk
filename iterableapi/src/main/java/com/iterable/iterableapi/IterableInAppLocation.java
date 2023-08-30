package com.iterable.iterableapi;

import androidx.annotation.NonNull;

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
    },
    EMBEDDED {
        @NonNull
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
