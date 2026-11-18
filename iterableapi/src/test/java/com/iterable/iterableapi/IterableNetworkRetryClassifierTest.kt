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
    fun zeroDeadlineIsUnbounded() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineMillis = 0L))
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 3_600_000L))
    }

    @Test
    fun defaultDelayIsAFixedTwoSeconds() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        for (attempt in 1..5) {
            assertEquals(2_000L, classifier.delayForAttempt(attempt))
        }
    }
}
