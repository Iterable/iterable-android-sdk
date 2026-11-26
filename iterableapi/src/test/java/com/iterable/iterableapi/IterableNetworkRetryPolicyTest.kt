package com.iterable.iterableapi

import android.util.Log
import com.iterable.iterableapi.unit.TestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog
import java.lang.reflect.Modifier

@RunWith(TestRunner::class)
class IterableNetworkRetryPolicyTest {

    @Test
    fun configCarriesTheDisabledDefaultPolicy() {
        val policy = IterableConfig.Builder().build().networkRetryPolicy

        assertSame(IterableNetworkRetryPolicy.DEFAULT, policy)
        assertFalse(policy.isRetryEnabled)
        assertEquals(5, policy.maxAttempts)
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

    @Test
    fun nullBackoffPolicyFallsBackToTheDefaultInterval() {
        val policy = IterableNetworkRetryPolicy.Builder().setBackoffPolicy(nullOf()).build()

        assertEquals(2_000L, policy.backoffPolicy.intervalMillis)
        assertEquals(1, errorsFor("IterableNetworkRetryPolicy", "setBackoffPolicy received null"))
    }

    @Test
    fun nullConfigPolicyFallsBackToTheDefault() {
        val policy = IterableConfig.Builder().setNetworkRetryPolicy(nullOf()).build().networkRetryPolicy

        assertSame(IterableNetworkRetryPolicy.DEFAULT, policy)
        assertEquals(1, errorsFor("IterableConfig", "setNetworkRetryPolicy received null"))
    }

    @Test
    fun publicGetterReturnsTheConfiguredPolicy() {
        val custom = IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).build()
        val config = IterableConfig.Builder().setNetworkRetryPolicy(custom).build()

        assertSame(custom, config.getNetworkRetryPolicy())
        assertSame(IterableNetworkRetryPolicy.DEFAULT, IterableConfig.Builder().build().getNetworkRetryPolicy())
        assertTrue(Modifier.isPublic(IterableConfig::class.java.getMethod("getNetworkRetryPolicy").modifiers))
    }

    @Test
    fun smallestValidValuesAreAccepted() {
        val policy = IterableNetworkRetryPolicy.Builder()
            .setMaxAttempts(1)
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(0L))
            .build()

        assertEquals(1, policy.maxAttempts)
        assertEquals(0L, policy.backoffPolicy.intervalMillis)
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

    private fun errorsFor(tag: String, text: String) = ShadowLog.getLogsForTag(tag)
        .count { it.type == Log.ERROR && it.msg.contains(text) }

    @Suppress("UNCHECKED_CAST")
    private fun <T> nullOf(): T = null as T
}
