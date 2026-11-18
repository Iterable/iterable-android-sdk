package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestRunner::class)
class IterableNetworkRetryPolicyTest {

    @Test
    fun configCarriesTheDisabledDefaultPolicy() {
        val policy = IterableConfig.Builder().build().networkRetryPolicy

        assertSame(IterableNetworkRetryPolicy.DEFAULT, policy)
        assertFalse(policy.isRetryEnabled)
        assertEquals(5, policy.maxAttempts)
        assertEquals(IterableNetworkRetryPolicy.BackoffPolicy.Type.FIXED, policy.backoffPolicy.type)
        assertEquals(2_000L, policy.backoffPolicy.intervalMillis)
        assertEquals(60_000L, policy.totalDeadlineMillis)
    }

    @Test
    fun setterRoundTripsTheCustomPolicy() {
        val custom = IterableNetworkRetryPolicy.Builder()
            .setRetryEnabled(true)
            .setMaxAttempts(3)
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(500L))
            .setTotalDeadlineMillis(0L)
            .build()

        val policy = IterableConfig.Builder().setNetworkRetryPolicy(custom).build().networkRetryPolicy

        assertSame(custom, policy)
        assertTrue(policy.isRetryEnabled)
        assertEquals(3, policy.maxAttempts)
        assertEquals(500L, policy.backoffPolicy.intervalMillis)
        assertEquals(0L, policy.totalDeadlineMillis)
    }

    @Test
    fun jwtRetryPolicyIsUnaffected() {
        val config = IterableConfig.Builder()
            .setNetworkRetryPolicy(IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).build())
            .build()

        assertEquals(10, config.retryPolicy.maxRetry)
        assertEquals(6_000L, config.retryPolicy.retryIntervalMillis)
        assertEquals(RetryPolicy.Type.LINEAR, config.retryPolicy.retryBackoff)
    }

    @Test(expected = IllegalArgumentException::class)
    fun maxAttemptsBelowOneIsRejected() {
        IterableNetworkRetryPolicy.Builder().setMaxAttempts(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeIntervalIsRejected() {
        IterableNetworkRetryPolicy.BackoffPolicy.fixed(-1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeDeadlineIsRejected() {
        IterableNetworkRetryPolicy.Builder().setTotalDeadlineMillis(-1L)
    }
}
