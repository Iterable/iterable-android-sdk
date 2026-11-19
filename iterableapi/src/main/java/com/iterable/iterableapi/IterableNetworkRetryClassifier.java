package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

class IterableNetworkRetryClassifier {

    enum Outcome {
        SUCCESS,
        RETRIABLE,
        TERMINAL,
        AUTH
    }

    @NonNull
    private final IterableNetworkRetryPolicy policy;

    IterableNetworkRetryClassifier(@NonNull IterableNetworkRetryPolicy policy) {
        this.policy = policy;
    }

    // Expects the code as IterableRequestTask reports it: 0 for any client-side failure to get a response,
    // including connection errors and timeouts.
    @NonNull
    static Outcome classify(int responseCode, @Nullable JSONObject json) {
        if (responseCode >= 200 && responseCode < 300) {
            return Outcome.SUCCESS;
        }
        if (responseCode == 0 || responseCode >= 500) {
            return Outcome.RETRIABLE;
        }
        if (responseCode == 401 && IterableRequestTask.matchesJWTErrorCodes(json)) {
            return Outcome.AUTH;
        }
        return Outcome.TERMINAL;
    }

    boolean shouldRetry(@NonNull Outcome outcome, int failedAttempt, long firstAttemptStartMillis, long nowMillis) {
        if (!policy.isRetryEnabled() || outcome != Outcome.RETRIABLE || failedAttempt >= policy.getMaxAttempts()) {
            return false;
        }
        long deadline = policy.getTotalDeadlineMillis();
        if (deadline == 0) {
            return true;
        }
        long elapsed = Math.max(0L, nowMillis - firstAttemptStartMillis);
        return elapsed <= deadline && delayMillis() <= deadline - elapsed;
    }

    long delayMillis() {
        return policy.getBackoffPolicy().delayMillis();
    }
}
