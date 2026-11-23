package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

class IterableNetworkRetryClassifier {
    static final String ERROR_CODE_INVALID_JWT_PAYLOAD = "InvalidJwtPayload";
    static final String ERROR_CODE_MISSING_JWT_PAYLOAD = "BadAuthorizationHeader";
    static final String ERROR_CODE_JWT_USER_IDENTIFIERS_MISMATCHED = "JwtUserIdentifiersMismatched";

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

    // Expects the code as IterableRequestTask reports it: 0 for connection errors and timeouts.
    @NonNull
    static Outcome classify(int responseCode, @Nullable JSONObject json) {
        if (responseCode >= 200 && responseCode < 300) {
            return Outcome.SUCCESS;
        }
        if (responseCode == 0 || (responseCode >= 500 && responseCode < 600)) {
            return Outcome.RETRIABLE;
        }
        if (responseCode == 401 && matchesJWTErrorCodes(json)) {
            return Outcome.AUTH;
        }
        return Outcome.TERMINAL;
    }

    /**
     * @param failedAttempt 1-based number of the attempt that just failed; values below 1 never retry
     */
    boolean shouldRetry(@NonNull Outcome outcome, int failedAttempt, long firstAttemptStartMillis, long nowMillis) {
        if (!policy.isRetryEnabled() || outcome != Outcome.RETRIABLE
                || failedAttempt < 1 || failedAttempt >= policy.getMaxAttempts()) {
            return false;
        }
        long deadline = policy.getTotalDeadlineMillis();
        if (deadline == 0) {
            return true;
        }
        // Times are expected from a monotonic clock; a backwards jump stops retrying instead of reopening the budget.
        if (nowMillis < firstAttemptStartMillis) {
            return false;
        }
        long elapsed = nowMillis - firstAttemptStartMillis;
        if (elapsed < 0) {
            return false;
        }
        return elapsed <= deadline && delayMillis() <= deadline - elapsed;
    }

    long delayMillis() {
        return policy.getBackoffPolicy().delayMillis();
    }

    static boolean matchesJWTErrorCodes(@Nullable JSONObject json) {
        return matchesErrorCode(json, ERROR_CODE_INVALID_JWT_PAYLOAD)
                || matchesErrorCode(json, ERROR_CODE_MISSING_JWT_PAYLOAD)
                || matchesErrorCode(json, ERROR_CODE_JWT_USER_IDENTIFIERS_MISMATCHED);
    }

    private static boolean matchesErrorCode(@Nullable JSONObject json, String errorCode) {
        try {
            return json != null && json.has("code") && json.getString("code").equals(errorCode);
        } catch (JSONException e) {
            return false;
        }
    }
}
