package com.iterable.iterableapi

class RetryPolicy(
    /**
     * Number of consecutive JWT refresh retries the SDK should attempt before disabling JWT refresh attempts altogether.
     */
    val maxRetry: Int,
    /**
     * Configurable duration between JWT refresh retries. Starting point for the retry backoff.
     */
    retryInterval: Long,
    /**
     * Linear or Exponential. Determines the backoff pattern to apply between retry attempts.
     */
    val retryBackoff: Type
) {
    val retryInterval: Long = retryInterval * 1000L

    enum class Type {
        LINEAR,
        EXPONENTIAL
    }
}

