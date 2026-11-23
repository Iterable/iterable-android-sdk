package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Retry policy for API requests that fail with a server error (5xx), a connection error or a timeout.
 * The policy applies to online requests once it is enabled and the project's remote {@code autoRetry}
 * setting is on; otherwise (the default) the SDK's existing request retry behaviour is unchanged.
 * Requests stored by offline mode keep their existing retry behaviour.
 * A request re-sent after a JWT authorization failure is handled by the auth retry path, so the re-send is not
 * limited by {@link #getMaxAttempts()} or {@link #getTotalDeadline()}. Network retries after it continue
 * the same attempt count and deadline.
 */
public final class IterableNetworkRetryPolicy {
    private static final String TAG = "IterableNetworkRetryPolicy";
    private static final double MAX_SECONDS = 315_360_000d;

    /**
     * Not enabled, so the SDK's existing request retry behaviour is unchanged. Once enabled: 5 attempts,
     * fixed 2 second interval, 60 second total deadline.
     */
    @NonNull
    public static final IterableNetworkRetryPolicy DEFAULT = new Builder().build();

    private final boolean retryEnabled;
    private final int maxAttempts;
    @NonNull
    private final BackoffPolicy backoffPolicy;
    private final long totalDeadlineMillis;

    private IterableNetworkRetryPolicy(Builder builder) {
        retryEnabled = builder.retryEnabled;
        maxAttempts = builder.maxAttempts;
        backoffPolicy = builder.backoffPolicy;
        totalDeadlineMillis = builder.totalDeadlineMillis;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    /**
     * Total number of attempts per request, including the first one.
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @NonNull
    public BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }

    /**
     * Maximum time in seconds from the start of the first attempt to the start of the last retry.
     * 0 means no deadline.
     */
    public double getTotalDeadline() {
        return totalDeadlineMillis / 1000d;
    }

    long getTotalDeadlineMillis() {
        return totalDeadlineMillis;
    }

    private static boolean isValidSeconds(double seconds) {
        return seconds >= 0 && !Double.isInfinite(seconds);
    }

    private static long secondsToMillis(String name, double seconds) {
        if (seconds > MAX_SECONDS) {
            IterableLogger.w(TAG, name + " of " + seconds + "s exceeds the maximum, clamping to " + MAX_SECONDS + "s");
            return Math.round(MAX_SECONDS * 1000d);
        }
        // 0 ms means no deadline, so a positive value must not round down to it.
        return seconds > 0 ? Math.max(1L, Math.round(seconds * 1000d)) : 0L;
    }

    /**
     * Delay between retry attempts.
     */
    public static final class BackoffPolicy {
        static final BackoffPolicy DEFAULT = new BackoffPolicy(2_000L);

        private final long intervalMillis;

        private BackoffPolicy(long intervalMillis) {
            this.intervalMillis = intervalMillis;
        }

        /**
         * Waits the same interval before every retry.
         * <p>
         * Invalid values (negative, {@code NaN} or infinite) are logged and ignored rather than throwing,
         * and the default 2 second interval is returned. Values above ~10 years are clamped to that
         * ceiling. Zero is valid and means no delay.
         *
         * @param intervalSeconds delay in seconds
         */
        @NonNull
        public static BackoffPolicy fixed(double intervalSeconds) {
            if (!isValidSeconds(intervalSeconds)) {
                IterableLogger.e(TAG, "BackoffPolicy.fixed interval must be a finite number of seconds, 0 or more (was "
                        + intervalSeconds + "), ignoring it and using the default 2s");
                return DEFAULT;
            }
            return new BackoffPolicy(secondsToMillis("BackoffPolicy.fixed interval", intervalSeconds));
        }

        /**
         * Delay between retries, in seconds.
         */
        public double getInterval() {
            return intervalMillis / 1000d;
        }

        long getIntervalMillis() {
            return intervalMillis;
        }

        long delayMillis() {
            return intervalMillis;
        }
    }

    public static final class Builder {
        private boolean retryEnabled = false;
        private int maxAttempts = 5;
        @NonNull
        private BackoffPolicy backoffPolicy = BackoffPolicy.DEFAULT;
        private long totalDeadlineMillis = 60_000L;

        @NonNull
        public Builder setRetryEnabled(boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
            return this;
        }

        /**
         * Values below 1 are logged and ignored rather than throwing, leaving the value at whatever it was
         * before the call: 5 unless an earlier call set something else.
         *
         * @param maxAttempts total attempts including the first one, must be 1 or more
         */
        @NonNull
        public Builder setMaxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                IterableLogger.e(TAG, "maxAttempts must be 1 or more (was " + maxAttempts + "), ignoring it and keeping "
                        + this.maxAttempts);
                return this;
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * @param backoffPolicy delay between retries; {@code null} is treated as the default 2 second interval
         */
        @NonNull
        public Builder setBackoffPolicy(@NonNull BackoffPolicy backoffPolicy) {
            if (backoffPolicy == null) {
                IterableLogger.e(TAG, "setBackoffPolicy received null, defaulting to a fixed 2 second interval");
                this.backoffPolicy = BackoffPolicy.DEFAULT;
                return this;
            }
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        /**
         * Invalid values (negative, {@code NaN} or infinite) are logged and ignored rather than throwing,
         * leaving the deadline at whatever it was before the call: 60 seconds unless an earlier call set
         * something else. Values above ~10 years are clamped to that ceiling.
         *
         * @param seconds deadline in seconds, 0 for none
         */
        @NonNull
        public Builder setTotalDeadline(double seconds) {
            if (!isValidSeconds(seconds)) {
                IterableLogger.e(TAG, "totalDeadline must be a finite number of seconds, 0 or more (was " + seconds
                        + "), ignoring it and keeping " + totalDeadlineMillis / 1000d + "s");
                return this;
            }
            this.totalDeadlineMillis = secondsToMillis("totalDeadline", seconds);
            return this;
        }

        @NonNull
        public IterableNetworkRetryPolicy build() {
            return new IterableNetworkRetryPolicy(this);
        }
    }
}
