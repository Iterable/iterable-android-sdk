package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    void loadSaved(@NonNull Context context) {
        autoRetryEnabled = context.getSharedPreferences(IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION, Context.MODE_PRIVATE)
                .getBoolean(IterableConstants.SHARED_PREFS_AUTO_RETRY_KEY, false);
    }

    @Nullable
    IterableNetworkRetryPolicy activeNetworkRetryPolicy(@NonNull IterableNetworkRetryPolicy localPolicy) {
        return localPolicy.isRetryEnabled() && autoRetryEnabled ? localPolicy : null;
    }
}
