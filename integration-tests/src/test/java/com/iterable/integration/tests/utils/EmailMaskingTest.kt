package com.iterable.integration.tests.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailMaskingTest {

    @Test
    fun `masks local part keeping first char and domain`() {
        assertEquals("f***@iterable.com", maskEmail("franco.zalamena@iterable.com"))
    }

    @Test
    fun `masks dated test user`() {
        assertEquals("2***@test.com", maskEmail("2026-06-09-integration-test-user@test.com"))
    }

    @Test
    fun `null or blank returns placeholder`() {
        assertEquals("<none>", maskEmail(null))
        assertEquals("<none>", maskEmail(""))
        assertEquals("<none>", maskEmail("   "))
    }

    @Test
    fun `string without at sign is fully masked`() {
        assertEquals("***", maskEmail("not-an-email"))
    }
}
