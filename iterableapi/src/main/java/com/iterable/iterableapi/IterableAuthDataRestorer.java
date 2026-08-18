package com.iterable.iterableapi;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Retries a stored auth-token read without turning a transient timeout into a missing token.
 */
class IterableAuthDataRestorer {
    private static final String TAG = "IterableAuthRestore";

    @VisibleForTesting
    static final int MAX_TIMEOUT_RETRIES = 2;

    @VisibleForTesting
    static final long RETRY_DELAY_MS = 1000L;

    private static final ScheduledExecutorService RETRY_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "IterableAuthRestore");
                thread.setDaemon(true);
                return thread;
            });

    interface Callback {
        void onAuthTokenRestored(@Nullable String authToken);

        void onAuthTokenUnavailable();
    }

    private final IterableKeychain keychain;
    private final ScheduledExecutorService retryExecutor;

    @Nullable
    private ScheduledFuture<?> pendingRetry;

    @Nullable
    private Callback callback;

    private int generation;
    private boolean restoring;
    private boolean unavailable;

    IterableAuthDataRestorer(IterableKeychain keychain) {
        this(keychain, RETRY_EXECUTOR);
    }

    @VisibleForTesting
    IterableAuthDataRestorer(
            IterableKeychain keychain,
            ScheduledExecutorService retryExecutor) {
        this.keychain = keychain;
        this.retryExecutor = retryExecutor;
    }

    void restore(KeychainReadResult initialRead, Callback callback) {
        final int currentGeneration;
        synchronized (this) {
            cancelPendingRetry();
            generation++;
            currentGeneration = generation;
            this.callback = callback;
            restoring = true;
            unavailable = false;
        }

        IterableLogger.d(TAG, "auth_restore action=start");
        handleRead(currentGeneration, 0, initialRead);
    }

    /**
     * Returns true while restoration is unresolved. An unavailable restore starts a new cycle;
     * an already-running restore is left alone.
     */
    synchronized boolean resumeIfUnresolved() {
        if (restoring) {
            IterableLogger.d(
                    TAG,
                    "auth_restore action=resume outcome=already_running source=foreground");
            return true;
        }
        if (!unavailable || callback == null) {
            return false;
        }

        generation++;
        restoring = true;
        unavailable = false;
        IterableLogger.d(
                TAG,
                "auth_restore action=resume outcome=scheduled source=foreground");
        scheduleRead(generation, 0, 0);
        return true;
    }

    synchronized void cancel(String reason) {
        boolean wasUnresolved = restoring || unavailable;
        generation++;
        cancelPendingRetry();
        callback = null;
        restoring = false;
        unavailable = false;
        if (wasUnresolved) {
            IterableLogger.d(TAG, "auth_restore action=cancel reason=" + reason);
        }
    }

    private void handleRead(
            int currentGeneration,
            int timeoutRetries,
            KeychainReadResult result) {
        if (!isCurrent(currentGeneration)) {
            return;
        }

        int attempt = timeoutRetries + 1;
        if (result instanceof KeychainReadResult.Value) {
            String authToken = ((KeychainReadResult.Value) result).getValue();
            complete(currentGeneration, authToken, attempt);
            return;
        }

        IterableLogger.d(
                TAG,
                "auth_restore action=read attempt="
                        + attempt
                        + " outcome=timeout");
        if (timeoutRetries >= MAX_TIMEOUT_RETRIES) {
            markUnavailable(currentGeneration, attempt);
        } else {
            scheduleRead(currentGeneration, timeoutRetries + 1, RETRY_DELAY_MS);
        }
    }

    private synchronized void scheduleRead(
            int currentGeneration,
            int timeoutRetries,
            long delayMs) {
        if (!isCurrent(currentGeneration)) {
            return;
        }

        pendingRetry = retryExecutor.schedule(() -> {
            synchronized (IterableAuthDataRestorer.this) {
                if (!isCurrent(currentGeneration)) {
                    IterableLogger.d(
                            TAG,
                            "auth_restore action=ignore reason=stale_generation");
                    return;
                }
                pendingRetry = null;
            }
            handleRead(currentGeneration, timeoutRetries, keychain.readAuthToken());
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private synchronized void complete(
            int currentGeneration,
            @Nullable String authToken,
            int attempt) {
        if (!isCurrent(currentGeneration)) {
            return;
        }

        restoring = false;
        unavailable = false;
        pendingRetry = null;
        IterableLogger.d(
                TAG,
                "auth_restore action=read attempt="
                        + attempt
                        + " outcome="
                        + (authToken == null ? "token_missing" : "token_found"));

        Callback currentCallback = callback;
        callback = null;
        if (currentCallback != null) {
            // Keep completion ordered with cancel(): an explicit identity change must win.
            currentCallback.onAuthTokenRestored(authToken);
        }
    }

    private synchronized void markUnavailable(int currentGeneration, int attempts) {
        if (!isCurrent(currentGeneration)) {
            return;
        }

        restoring = false;
        unavailable = true;
        pendingRetry = null;
        IterableLogger.w(
                TAG,
                "auth_restore action=complete attempts="
                        + attempts
                        + " outcome=unavailable");
        if (callback != null) {
            callback.onAuthTokenUnavailable();
        }
    }

    private synchronized boolean isCurrent(int currentGeneration) {
        return generation == currentGeneration && restoring;
    }

    private void cancelPendingRetry() {
        if (pendingRetry != null) {
            pendingRetry.cancel(true);
            pendingRetry = null;
        }
    }
}
