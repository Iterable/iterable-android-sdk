package com.iterable.integration.tests.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the per-day BCIT integration-test email. Mirrors the iOS BCIT shape
 * (YYYY-MM-DD-integration-test-user@test.com) so a fresh user is created daily
 * and the embedded campaign's audience-eligibility transition fires reliably.
 *
 * Date defaults to "now" in UTC so the email is identical on every CI runner
 * regardless of system timezone. The date param is exposed so the logic is
 * unit-testable without time mocks.
 *
 * Avoids java.time to keep the test app working on API < 26 without the
 * core-library-desugaring opt-in.
 */
object TestUserEmailGenerator {

    private val UTC = TimeZone.getTimeZone("UTC")

    fun generate(date: Date = Date()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = UTC }
        return "${formatter.format(date)}-integration-test-user@test.com"
    }
}
