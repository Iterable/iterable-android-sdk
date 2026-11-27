package com.iterable.iterableapi

import android.util.Log
import com.iterable.iterableapi.unit.TestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertEquals(2.0, policy.backoffPolicy.interval, 0.0)
        assertEquals(60.0, policy.totalDeadline, 0.0)
        assertEquals(2_000L, policy.backoffPolicy.intervalMillis)
        assertEquals(60_000L, policy.totalDeadlineMillis)
    }

    @Test
    fun defaultHasABackoffPolicyWhicheverClassInitializesFirst() {
        val policyClass = IterableNetworkRetryPolicy::class.java.name
        for (first in listOf(policyClass, "$policyClass\$Builder", "$policyClass\$BackoffPolicy")) {
            val loader = freshPolicyClassLoader()
            Class.forName(first, true, loader)
            val outer = Class.forName(policyClass, true, loader)

            val default = outer.getField("DEFAULT").get(null)

            assertNotNull("$first first", outer.getMethod("getBackoffPolicy").invoke(default))
        }
    }

    @Test
    fun setterRoundTripsTheCustomPolicy() {
        val custom = IterableNetworkRetryPolicy.Builder()
            .setRetryEnabled(true)
            .setMaxAttempts(3)
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(0.5))
            .setTotalDeadline(0.0)
            .build()

        val policy = IterableConfig.Builder().setNetworkRetryPolicy(custom).build().networkRetryPolicy

        assertSame(custom, policy)
        assertTrue(policy.isRetryEnabled)
        assertEquals(3, policy.maxAttempts)
        assertEquals(0.5, policy.backoffPolicy.interval, 0.0)
        assertEquals(0.0, policy.totalDeadline, 0.0)
    }

    @Test
    fun secondsAreConvertedToMillisecondsInternally() {
        val policy = IterableNetworkRetryPolicy.Builder()
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(0.25))
            .setTotalDeadline(1.5)
            .build()

        assertEquals(250L, policy.backoffPolicy.intervalMillis)
        assertEquals(1_500L, policy.totalDeadlineMillis)
    }

    @Test
    fun secondsAreRoundedToTheNearestMillisecond() {
        val policy = IterableNetworkRetryPolicy.Builder()
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(1.001))
            .setTotalDeadline(1.001)
            .build()

        assertEquals(1_001L, policy.backoffPolicy.intervalMillis)
        assertEquals(1_001L, policy.totalDeadlineMillis)
    }

    @Test
    fun positiveSecondsNeverRoundDownToZero() {
        val tiny = IterableNetworkRetryPolicy.Builder()
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(0.0004))
            .setTotalDeadline(0.0004)
            .build()
        val zero = IterableNetworkRetryPolicy.Builder()
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(0.0))
            .setTotalDeadline(0.0)
            .build()

        assertEquals(1L, tiny.backoffPolicy.intervalMillis)
        assertEquals(1L, tiny.totalDeadlineMillis)
        assertEquals(0L, zero.backoffPolicy.intervalMillis)
        assertEquals(0L, zero.totalDeadlineMillis)
    }

    @Test
    fun hugeSecondsAreClampedToTenYears() {
        val policy = IterableNetworkRetryPolicy.Builder()
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(Double.MAX_VALUE))
            .setTotalDeadline(Double.MAX_VALUE)
            .build()

        assertEquals(315_360_000.0, policy.backoffPolicy.interval, 0.0)
        assertEquals(315_360_000.0, policy.totalDeadline, 0.0)
        assertEquals(315_360_000_000L, policy.totalDeadlineMillis)
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

        assertEquals(2.0, policy.backoffPolicy.interval, 0.0)
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
            .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(0.0))
            .build()

        assertEquals(1, policy.maxAttempts)
        assertEquals(0.0, policy.backoffPolicy.interval, 0.0)
    }

    @Test
    fun invalidValuesAreLoggedAndKeepThePreviousValue() {
        val policy = IterableNetworkRetryPolicy.Builder()
            .setMaxAttempts(3)
            .setMaxAttempts(0)
            .setTotalDeadline(10.0)
            .setTotalDeadline(-1.0)
            .build()

        assertEquals(3, policy.maxAttempts)
        assertEquals(10.0, policy.totalDeadline, 0.0)
        assertEquals(1, errorsFor("IterableNetworkRetryPolicy", "maxAttempts"))
        assertEquals(1, errorsFor("IterableNetworkRetryPolicy", "totalDeadline"))
    }

    @Test
    fun invalidValuesKeepTheDefaults() {
        val policy = IterableNetworkRetryPolicy.Builder()
            .setMaxAttempts(0)
            .setTotalDeadline(-1.0)
            .build()

        assertEquals(5, policy.maxAttempts)
        assertEquals(60.0, policy.totalDeadline, 0.0)
    }

    @Test
    fun invalidIntervalIsLoggedAndGivesTheDefaultInterval() {
        val backoff = IterableNetworkRetryPolicy.BackoffPolicy.fixed(-1.0)

        assertEquals(2.0, backoff.interval, 0.0)
        assertEquals(1, errorsFor("IterableNetworkRetryPolicy", "BackoffPolicy.fixed"))
    }

    @Test
    fun nanAndInfiniteSecondsAreIgnored() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val policy = IterableNetworkRetryPolicy.Builder()
                .setTotalDeadline(10.0)
                .setTotalDeadline(value)
                .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(value))
                .build()

            assertEquals("deadline $value", 10.0, policy.totalDeadline, 0.0)
            assertEquals("interval $value", 2.0, policy.backoffPolicy.interval, 0.0)
        }
        assertEquals(3, errorsFor("IterableNetworkRetryPolicy", "totalDeadline"))
        assertEquals(3, errorsFor("IterableNetworkRetryPolicy", "BackoffPolicy.fixed"))
    }

    private fun errorsFor(tag: String, text: String) = ShadowLog.getLogsForTag(tag)
        .count { it.type == Log.ERROR && it.msg.contains(text) }

    // Static init runs once per class loader, so each order needs its own copy of the policy classes.
    private fun freshPolicyClassLoader(): ClassLoader {
        val policyClass = IterableNetworkRetryPolicy::class.java.name
        return object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name != policyClass && !name.startsWith("$policyClass\$")) {
                    return super.loadClass(name, resolve)
                }
                synchronized(this) {
                    findLoadedClass(name)?.let { return it }
                    val bytes = parent.getResourceAsStream(name.replace('.', '/') + ".class")!!.use { it.readBytes() }
                    return defineClass(name, bytes, 0, bytes.size)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> nullOf(): T = null as T
}
