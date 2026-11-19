package com.iterable.iterableapi

import com.iterable.iterableapi.IterableNetworkRetryClassifier.Outcome
import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestRunner::class)
class IterableNetworkRetryClassifierTest {

    private val start = 1_000_000L

    private fun enabledPolicy(deadlineMillis: Long = 60_000L) = IterableNetworkRetryPolicy.Builder()
        .setRetryEnabled(true)
        .setTotalDeadlineMillis(deadlineMillis)
        .build()

    private fun jwtError(code: String) = JSONObject().put("code", code)

    @Test
    fun transportFailuresAndServerErrorsAreRetriable() {
        for (code in listOf(0, 500, 502, 503, 504, 599)) {
            assertEquals("code $code", Outcome.RETRIABLE, IterableNetworkRetryClassifier.classify(code, null))
        }
    }

    @Test
    fun statusCodeBoundariesAreClassifiedExactly() {
        val expected = mapOf(
            199 to Outcome.TERMINAL,
            200 to Outcome.SUCCESS,
            299 to Outcome.SUCCESS,
            300 to Outcome.TERMINAL,
            399 to Outcome.TERMINAL,
            400 to Outcome.TERMINAL,
            499 to Outcome.TERMINAL,
            500 to Outcome.RETRIABLE,
            599 to Outcome.RETRIABLE
        )
        for ((code, outcome) in expected) {
            assertEquals("code $code", outcome, IterableNetworkRetryClassifier.classify(code, null))
        }
    }

    @Test
    fun jwtErrorBodyOnANon401IsTerminal() {
        val body = jwtError(IterableRequestTask.ERROR_CODE_INVALID_JWT_PAYLOAD)
        assertEquals(Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(403, body))
        assertEquals(Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(400, body))
    }

    @Test
    fun tooManyRequestsIsTerminal() {
        assertEquals(Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(429, null))
    }

    @Test
    fun clientErrorsAreTerminal() {
        for (code in listOf(400, 403, 404, 409, 422, 429)) {
            assertEquals("code $code", Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(code, null))
        }
    }

    @Test
    fun jwtUnauthorizedGoesToTheAuthPath() {
        for (code in listOf(
            IterableRequestTask.ERROR_CODE_INVALID_JWT_PAYLOAD,
            IterableRequestTask.ERROR_CODE_MISSING_JWT_PAYLOAD,
            IterableRequestTask.ERROR_CODE_JWT_USER_IDENTIFIERS_MISMATCHED
        )) {
            assertEquals(code, Outcome.AUTH, IterableNetworkRetryClassifier.classify(401, jwtError(code)))
        }
    }

    @Test
    fun unauthorizedWithoutJwtErrorCodeIsTerminal() {
        assertEquals(Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(401, null))
        assertEquals(Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(401, jwtError("BadApiKey")))
    }

    @Test
    fun successIsNotRetried() {
        assertEquals(Outcome.SUCCESS, IterableNetworkRetryClassifier.classify(200, JSONObject()))
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        assertFalse(classifier.shouldRetry(Outcome.SUCCESS, 1, start, start))
    }

    @Test
    fun defaultPolicyNeverRetries() {
        val classifier = IterableNetworkRetryClassifier(IterableNetworkRetryPolicy.DEFAULT)
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start))
    }

    @Test
    fun onlyRetriableOutcomesAreRetried() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start))
        assertFalse(classifier.shouldRetry(Outcome.TERMINAL, 1, start, start))
        assertFalse(classifier.shouldRetry(Outcome.AUTH, 1, start, start))
    }

    @Test
    fun retriesStopAfterMaxAttempts() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        for (attempt in 1..4) {
            assertTrue("attempt $attempt", classifier.shouldRetry(Outcome.RETRIABLE, attempt, start, start))
        }
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 5, start, start))
    }

    @Test
    fun retriesStopWhenTheNextDelayWouldPassTheDeadline() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineMillis = 10_000L))
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 8_000L))
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 8_001L))
    }

    @Test
    fun hugeDelayDoesNotOverflowTheDeadlineCheck() {
        val classifier = IterableNetworkRetryClassifier(
            IterableNetworkRetryPolicy.Builder()
                .setRetryEnabled(true)
                .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(Long.MAX_VALUE))
                .build()
        )
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start))
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 1L))
    }

    @Test
    fun customMaxAttemptsIsHonoured() {
        val classifier = IterableNetworkRetryClassifier(
            IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).setMaxAttempts(3).build()
        )
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 2, start, start))
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 3, start, start))
    }

    @Test
    fun singleAttemptNeverRetries() {
        val classifier = IterableNetworkRetryClassifier(
            IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).setMaxAttempts(1).build()
        )
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start))
    }

    @Test
    fun customIntervalFeedsTheDelayAndTheDeadline() {
        val classifier = IterableNetworkRetryClassifier(
            IterableNetworkRetryPolicy.Builder()
                .setRetryEnabled(true)
                .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(500L))
                .setTotalDeadlineMillis(10_000L)
                .build()
        )
        assertEquals(500L, classifier.delayMillis())
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 9_500L))
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 9_501L))
    }

    @Test
    fun zeroDeadlineIsUnbounded() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineMillis = 0L))
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 3_600_000L))
    }

    @Test
    fun defaultDelayIsAFixedTwoSeconds() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        assertEquals(2_000L, classifier.delayMillis())
    }
}
