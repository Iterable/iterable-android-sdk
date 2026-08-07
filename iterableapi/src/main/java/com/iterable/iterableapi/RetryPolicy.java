package com.iterable.iterableapi;


public class RetryPolicy {

    /**
     * Number of consecutive JWT refresh retries the SDK should attempt before disabling JWT refresh attempts altogether.
     */
    int maxRetry;

    /**
     * Configurable duration in milliseconds between JWT refresh retries. Starting point for the retry backoff.
     */
    long retryIntervalMillis;

    /**
     * Linear or Exponential. Determines the backoff pattern to apply between retry attempts.
     */
    RetryPolicy.Type retryBackoff;
    public enum Type {
        LINEAR,
        EXPONENTIAL
    }
    /**
     * @param retryInterval in seconds
     */
    public RetryPolicy(int maxRetry, long retryInterval, RetryPolicy.Type retryBackoff) {
        this.maxRetry = maxRetry;
        this.retryIntervalMillis = retryInterval * 1000L;
        this.retryBackoff = retryBackoff;
    }
}

