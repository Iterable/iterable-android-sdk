package com.iterable.iterableapi;

/**
 * Explains why the SDK scheduled an auth token refresh.
 *
 * The reason also defines whether the refresh is part of the normal token lifecycle or a retry
 * that must respect the configured retry pause and maximum.
 */
enum IterableAuthRefreshReason {
    TOKEN_EXPIRING(true),
    TOKEN_EXPIRED(true),
    STORED_TOKEN_MISSING(true),
    TOKEN_MISSING(false),
    TOKEN_INVALID(false),
    AUTH_HANDLER_RETRY(false),
    DEFERRED_REFRESH(false),
    JWT_401(false);

    private final boolean ignoresRetryPolicy;

    IterableAuthRefreshReason(boolean ignoresRetryPolicy) {
        this.ignoresRetryPolicy = ignoresRetryPolicy;
    }

    boolean ignoresRetryPolicy() {
        return ignoresRetryPolicy;
    }
}
