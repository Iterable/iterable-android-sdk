package com.iterable.integration.tests.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TestUserEmailGeneratorTest {

    private fun dateAt(yyyyMmDdInUtc: String): Date {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return parser.parse(yyyyMmDdInUtc)!!
    }

    @Test
    fun `formats date as YYYY-MM-DD with the iOS BCIT email shape`() {
        assertEquals(
            "2026-06-02-integration-test-user@test.com",
            TestUserEmailGenerator.generate(dateAt("2026-06-02"))
        )
    }

    @Test
    fun `single-digit months and days are zero-padded`() {
        assertEquals(
            "2026-01-07-integration-test-user@test.com",
            TestUserEmailGenerator.generate(dateAt("2026-01-07"))
        )
    }

    @Test
    fun `formatting is timezone-stable - epoch boundary in UTC`() {
        // 1970-01-01T00:00:00Z
        assertEquals(
            "1970-01-01-integration-test-user@test.com",
            TestUserEmailGenerator.generate(Date(0L))
        )
    }

    @Test
    fun `late-day UTC instant still uses today's UTC date`() {
        // 2026-06-02T23:59:59Z — should not roll over to 2026-06-03
        val almostMidnightUtc = Date(dateAt("2026-06-02").time + (23 * 3600 + 59 * 60 + 59) * 1000L)
        assertEquals(
            "2026-06-02-integration-test-user@test.com",
            TestUserEmailGenerator.generate(almostMidnightUtc)
        )
    }
}
