package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Retry policy for API requests that fail with a server error, a timeout or a connection failure.
 * Retry is disabled by default. The policy is not applied to requests yet.
 */
public final class IterableNetworkRetryPolicy {

    /**
     * Retry disabled, 5 attempts, fixed 2 second interval, 60 second total deadline.
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
     * Maximum time from the start of the first attempt to the start of the last retry.
     * 0 means no deadline.
     */
    public long getTotalDeadlineMillis() {
        return totalDeadlineMillis;
    }

    /**
     * Delay between retry attempts.
     */
    public static final class BackoffPolicy {

        public enum Type {
            FIXED
        }

        @NonNull
        private final Type type;
        private final long intervalMillis;

        private BackoffPolicy(@NonNull Type type, long intervalMillis) {
            this.type = type;
            this.intervalMillis = intervalMillis;
        }

        /**
         * Waits the same interval before every retry.
         * @param intervalMillis delay in milliseconds, must be 0 or more
         */
        @NonNull
        public static BackoffPolicy fixed(long intervalMillis) {
            if (intervalMillis < 0) {
                throw new IllegalArgumentException("intervalMillis must be >= 0");
            }
            return new BackoffPolicy(Type.FIXED, intervalMillis);
        }

        @NonNull
        public Type getType() {
            return type;
        }

        public long getIntervalMillis() {
            return intervalMillis;
        }

        long delayForAttempt(int attempt) {
            return intervalMillis;
        }
    }

    public static final class Builder {
        private boolean retryEnabled = false;
        private int maxAttempts = 5;
        @NonNull
        private BackoffPolicy backoffPolicy = BackoffPolicy.fixed(2000L);
        private long totalDeadlineMillis = 60_000L;

        @NonNull
        public Builder setRetryEnabled(boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
            return this;
        }

        /**
         * @param maxAttempts total attempts including the first one, must be 1 or more
         */
        @NonNull
        public Builder setMaxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        @NonNull
        public Builder setBackoffPolicy(@NonNull BackoffPolicy backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        /**
         * @param totalDeadlineMillis deadline in milliseconds, 0 for none
         */
        @NonNull
        public Builder setTotalDeadlineMillis(long totalDeadlineMillis) {
            if (totalDeadlineMillis < 0) {
                throw new IllegalArgumentException("totalDeadlineMillis must be >= 0");
            }
            this.totalDeadlineMillis = totalDeadlineMillis;
            return this;
        }

        @NonNull
        public IterableNetworkRetryPolicy build() {
            return new IterableNetworkRetryPolicy(this);
        }
    }
}
