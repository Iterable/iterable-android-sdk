package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;

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

    // A failed 2xx is terminal because the server already has the request.
    @NonNull
    static Outcome classify(@NonNull IterableApiResponse response) {
        if (response.success) {
            return Outcome.SUCCESS;
        }
        int code = response.responseCode;
        if ((code >= 200 && code < 300) || (code == 0 && !response.networkFailure)) {
            return Outcome.TERMINAL;
        }
        return classify(code, response.responseJson);
    }

    // Expects the code as IterableRequestTask reports it: 0 for any client-side failure to get a response,
    // including connection errors and timeouts.
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

    private static final int MAX_CAUSE_DEPTH = 8;

    // On devices a server-dropped connection surfaces as a plain IOException caused by an EOF or socket error.
    static boolean isNetworkFailure(@NonNull IOException e) {
        if (e instanceof SocketTimeoutException
                || e instanceof UnknownHostException
                || e instanceof SocketException
                || e instanceof EOFException) {
            return true;
        }
        Throwable cause = e.getCause();
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            // A certificate parser can throw EOF on bytes in memory, so a certificate error is never a network failure.
            if (cause instanceof CertificateException || cause instanceof CertPathValidatorException) {
                return false;
            }
            if (cause instanceof SocketTimeoutException || cause instanceof UnknownHostException
                    || cause instanceof EOFException || cause instanceof SocketException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    // A retry starts a little after its delay, so approval leaves this margin for the retry-start deadline check.
    static final long RETRY_START_TOLERANCE_MILLIS = 500L;

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
        return delayMillis() <= deadline - elapsed - RETRY_START_TOLERANCE_MILLIS;
    }

    boolean isPastDeadline(long firstAttemptStartMillis, long nowMillis) {
        long deadline = policy.getTotalDeadlineMillis();
        return deadline != 0 && nowMillis - firstAttemptStartMillis > deadline;
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
