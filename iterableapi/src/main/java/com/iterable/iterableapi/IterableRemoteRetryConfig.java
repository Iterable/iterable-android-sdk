package com.iterable.iterableapi;

/**
 * Remote {@code autoRetry} state, owned by {@link IterableApi} and passed to the request layer.
 */
final class IterableRemoteRetryConfig {
    // Written on the main thread by the remote config fetch, read on request threads.
    private volatile boolean autoRetryEnabled;

    boolean isAutoRetryEnabled() {
        return autoRetryEnabled;
    }

    void setAutoRetryEnabled(boolean autoRetryEnabled) {
        this.autoRetryEnabled = autoRetryEnabled;
    }
}
