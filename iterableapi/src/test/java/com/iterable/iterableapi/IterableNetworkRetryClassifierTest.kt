package com.iterable.iterableapi

import com.iterable.iterableapi.IterableNetworkRetryClassifier.Outcome
import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

@RunWith(TestRunner::class)
class IterableNetworkRetryClassifierTest {

    private val start = 1_000_000L

    private fun enabledPolicy(deadlineSeconds: Double = 60.0) = IterableNetworkRetryPolicy.Builder()
        .setRetryEnabled(true)
        .setTotalDeadline(deadlineSeconds)
        .build()

    private fun jwtError(code: String) = JSONObject().put("code", code)

    @Test
    fun statusCodesAreClassified() {
        val expected = mapOf(
            Outcome.SUCCESS to listOf(200, 299),
            Outcome.RETRIABLE to listOf(0, 500, 502, 503, 504, 599),
            Outcome.TERMINAL to listOf(199, 300, 399, 400, 403, 404, 409, 422, 429, 499, 600, 999)
        )
        for ((outcome, codes) in expected) {
            for (code in codes) {
                assertEquals("code $code", outcome, IterableNetworkRetryClassifier.classify(code, null))
            }
        }
    }

    @Test
    fun jwtBodiesReachTheAuthPathOnlyOn401() {
        for (code in listOf(
            IterableNetworkRetryClassifier.ERROR_CODE_INVALID_JWT_PAYLOAD,
            IterableNetworkRetryClassifier.ERROR_CODE_MISSING_JWT_PAYLOAD,
            IterableNetworkRetryClassifier.ERROR_CODE_JWT_USER_IDENTIFIERS_MISMATCHED
        )) {
            assertEquals(code, Outcome.AUTH, IterableNetworkRetryClassifier.classify(401, jwtError(code)))
        }
        val jwtBody = jwtError(IterableNetworkRetryClassifier.ERROR_CODE_INVALID_JWT_PAYLOAD)
        for ((code, body) in listOf(403 to jwtBody, 400 to jwtBody, 401 to null, 401 to jwtError("BadApiKey"))) {
            assertEquals("$code $body", Outcome.TERMINAL, IterableNetworkRetryClassifier.classify(code, body))
        }
    }

    @Test
    fun timeoutsDnsFailuresAndDroppedConnectionsAreNetworkFailures() {
        val networkFailures = listOf(
            SocketTimeoutException("Read timed out"),
            UnknownHostException("api.iterable.com"),
            ConnectException("Connection refused"),
            SocketException("Connection reset"),
            EOFException(),
            IOException("unexpected end of stream on http://api.iterable.com/...", EOFException("\\n not found")),
            IOException("unexpected end of stream", SocketException("Connection reset")),
            IOException("outer", IOException("inner", EOFException())),
            IOException("wrapped", SocketTimeoutException("Read timed out")),
            IOException("wrapped", UnknownHostException("api.iterable.com")),
            SSLException("Read error", SocketException("Connection reset")),
            SSLHandshakeException("Remote host terminated the handshake").apply {
                initCause(EOFException("SSL peer shut down incorrectly"))
            }
        )
        for (e in networkFailures) {
            assertTrue(causeChain(e), IterableNetworkRetryClassifier.isNetworkFailure(e))
        }
    }

    @Test
    fun otherIoFailuresAreNotNetworkFailures() {
        val otherFailures = listOf(
            IOException("stream closed"),
            IOException("wrapped", IllegalStateException("not network")),
            SSLHandshakeException("handshake failed"),
            SSLException("Unrecognized SSL message, plaintext connection?"),
            MalformedURLException("no protocol"),
            SSLHandshakeException("handshake failed").apply {
                initCause(CertificateException("Could not parse certificate", EOFException()))
            },
            SSLHandshakeException("handshake failed").apply {
                initCause(CertPathValidatorException("path validation failed", EOFException()))
            }
        )
        for (e in otherFailures) {
            assertFalse(causeChain(e), IterableNetworkRetryClassifier.isNetworkFailure(e))
        }
    }

    private fun causeChain(e: Throwable) = generateSequence(e) { it.cause }.joinToString(" <- ")

    @Test
    fun defaultPolicyNeverRetries() {
        val classifier = IterableNetworkRetryClassifier(IterableNetworkRetryPolicy.DEFAULT)
        assertFalse(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start))
    }

    @Test
    fun onlyRetriableOutcomesAreRetried() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        for (outcome in Outcome.values()) {
            assertEquals(outcome.name, outcome == Outcome.RETRIABLE, classifier.shouldRetry(outcome, 1, start, start))
        }
    }

    @Test
    fun retriesStopAfterMaxAttempts() {
        fun retries(maxAttempts: Int, failedAttempt: Int) = IterableNetworkRetryClassifier(
            IterableNetworkRetryPolicy.Builder().setRetryEnabled(true).setMaxAttempts(maxAttempts).build()
        ).shouldRetry(Outcome.RETRIABLE, failedAttempt, start, start)

        for ((maxAttempts, attempt) in listOf(5 to 1, 5 to 2, 5 to 3, 5 to 4, 3 to 2)) {
            assertTrue("max $maxAttempts, attempt $attempt", retries(maxAttempts, attempt))
        }
        for ((maxAttempts, attempt) in listOf(5 to 5, 3 to 3, 1 to 1, 5 to 0, 5 to -1)) {
            assertFalse("max $maxAttempts, attempt $attempt", retries(maxAttempts, attempt))
        }
    }

    @Test
    fun retriesStopWhenTheNextDelayWouldPassTheDeadline() {
        fun classifierWith(intervalSeconds: Double, deadlineSeconds: Double) = IterableNetworkRetryClassifier(
            IterableNetworkRetryPolicy.Builder()
                .setRetryEnabled(true)
                .setBackoffPolicy(IterableNetworkRetryPolicy.BackoffPolicy.fixed(intervalSeconds))
                .setTotalDeadline(deadlineSeconds)
                .build()
        )

        val deadline = 10_000L
        for (delay in listOf(2_000L, 500L)) {
            val classifier = classifierWith(delay / 1000.0, deadline / 1000.0)
            // A retry is approved only if it would start at least 500 ms before the deadline.
            val lastRetry = deadline - delay - 500L
            assertEquals(delay, classifier.delayMillis())
            assertTrue("delay $delay", classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + lastRetry))
            assertFalse("delay $delay", classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + lastRetry + 1))
        }
        val delayLongerThanTheDeadline = classifierWith(Double.MAX_VALUE, 60.0)
        assertFalse("MAX delay at 0 ms", delayLongerThanTheDeadline.shouldRetry(Outcome.RETRIABLE, 1, start, start))
        assertFalse("MAX delay at 1 ms", delayLongerThanTheDeadline.shouldRetry(Outcome.RETRIABLE, 1, start, start + 1L))
    }

    @Test
    fun backwardsOrWrappedClockStopsRetrying() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy())
        // The last two rows wrap the subtraction, to 1 and to -1.
        for ((first, now) in listOf(start to start - 1L, Long.MAX_VALUE to Long.MIN_VALUE, Long.MIN_VALUE to Long.MAX_VALUE)) {
            assertFalse("$first to $now", classifier.shouldRetry(Outcome.RETRIABLE, 1, first, now))
        }
    }

    @Test
    fun zeroDeadlineIsUnbounded() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineSeconds = 0.0))
        assertTrue(classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + 3_600_000L))
    }

    @Test
    fun zeroDeadlineNeverStopsARetryFromStarting() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineSeconds = 0.0))
        assertFalse(classifier.isPastDeadline(start, start + 2_000L))
        assertFalse(classifier.isPastDeadline(start, start + 3_600_000L))
    }

    @Test
    fun retryStartIsPastTheDeadlineOnlyAfterIt() {
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineSeconds = 10.0))
        assertFalse(classifier.isPastDeadline(start, start + 10_000L))
        assertTrue(classifier.isPastDeadline(start, start + 10_001L))
    }

    @Test
    fun anApprovedRetryThatStartsSlightlyLateIsStillWithinTheDeadline() {
        val deadline = 10_000L
        val classifier = IterableNetworkRetryClassifier(enabledPolicy(deadlineSeconds = deadline / 1000.0))
        val lateStart = 100L
        var approved = 0
        for (elapsed in 0L..deadline) {
            if (classifier.shouldRetry(Outcome.RETRIABLE, 1, start, start + elapsed)) {
                approved++
                assertFalse("approved at $elapsed",
                    classifier.isPastDeadline(start, start + elapsed + classifier.delayMillis() + lateStart))
            }
        }
        assertTrue(approved > 0)
    }
}
