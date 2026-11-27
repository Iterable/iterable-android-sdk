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

@RunWith(TestRunner::class)
class IterableNetworkRetryPolicyTest {

    @Test
    fun configCarriesTheDisabledDefaultPolicy() {
        val policy = IterableConfig.Builder().build().getNetworkRetryPolicy()

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

        val policy = IterableConfig.Builder().setNetworkRetryPolicy(custom).build().getNetworkRetryPolicy()

        assertSame(custom, policy)
        assertTrue(policy.isRetryEnabled)
        assertEquals(3, policy.maxAttempts)
        assertEquals(0.5, policy.backoffPolicy.interval, 0.0)
        assertEquals(0.0, policy.totalDeadline, 0.0)
    }

    @Test
    fun secondsAreConvertedToMilliseconds() {
        val rows = listOf(
            0.25 to 250L,
            1.5 to 1_500L,
            // 1.001 s is 1000.999... ms in binary, so truncation would give 1000.
            1.001 to 1_001L,
            // A positive value never rounds to 0, because a 0 ms deadline means no deadline.
            0.0004 to 1L,
            0.0 to 0L,
            Double.MAX_VALUE to 315_360_000_000L
        )
        for ((seconds, millis) in rows) {
            val policy = IterableNetworkRetryPolicy.Builder()
                .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(seconds))
                .setTotalDeadline(seconds)
                .build()

            assertEquals("interval $seconds", millis, policy.backoffPolicy.intervalMillis)
            assertEquals("deadline $seconds", millis, policy.totalDeadlineMillis)
            assertEquals("interval $seconds", millis / 1000.0, policy.backoffPolicy.interval, 0.0)
            assertEquals("deadline $seconds", millis / 1000.0, policy.totalDeadline, 0.0)
        }
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
    fun invalidMaxAttemptsAreLoggedAndIgnored() {
        val keepsTheEarlierValue = IterableNetworkRetryPolicy.Builder().setMaxAttempts(3).setMaxAttempts(0).build()
        assertEquals("maxAttempts 3 then 0", 3, keepsTheEarlierValue.maxAttempts)
        assertEquals("maxAttempts 3 then 0 logged", 1, policyErrorsSinceLastCheck("maxAttempts"))

        val keepsTheDefault = IterableNetworkRetryPolicy.Builder().setMaxAttempts(0).build()
        assertEquals("maxAttempts 0", 5, keepsTheDefault.maxAttempts)
        assertEquals("maxAttempts 0 logged", 1, policyErrorsSinceLastCheck("maxAttempts"))

        val smallestValid = IterableNetworkRetryPolicy.Builder().setMaxAttempts(1).build()
        assertEquals("maxAttempts 1", 1, smallestValid.maxAttempts)
        assertEquals("maxAttempts 1 logged", 0, policyErrorsSinceLastCheck("maxAttempts"))
    }

    @Test
    fun invalidSecondsAreLoggedAndIgnored() {
        for (invalid in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val policy = IterableNetworkRetryPolicy.Builder().setTotalDeadline(10.0).setTotalDeadline(invalid).build()
            assertEquals("totalDeadline 10 then $invalid", 10.0, policy.totalDeadline, 0.0)
            assertEquals("totalDeadline 10 then $invalid logged", 1, policyErrorsSinceLastCheck("totalDeadline"))

            val backoff = IterableNetworkRetryPolicy.BackoffPolicy.fixed(invalid)
            assertEquals("fixed($invalid)", 2.0, backoff.interval, 0.0)
            assertEquals("fixed($invalid) logged", 1, policyErrorsSinceLastCheck("BackoffPolicy.fixed"))
        }

        val keepsTheDefault = IterableNetworkRetryPolicy.Builder().setTotalDeadline(-1.0).build()
        assertEquals("totalDeadline -1", 60.0, keepsTheDefault.totalDeadline, 0.0)
        assertEquals("totalDeadline -1 logged", 1, policyErrorsSinceLastCheck("totalDeadline"))

        val noDelay = IterableNetworkRetryPolicy.BackoffPolicy.fixed(0.0)
        assertEquals("fixed(0)", 0.0, noDelay.interval, 0.0)
        assertEquals("fixed(0) logged", 0, policyErrorsSinceLastCheck("BackoffPolicy.fixed"))
    }

    private fun policyErrorsSinceLastCheck(text: String) =
        errorsFor("IterableNetworkRetryPolicy", text).also { ShadowLog.clear() }

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
