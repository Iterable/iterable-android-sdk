package com.iterable.integration.tests.utils

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestUserEmailOverrideTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun tearDown() {
        TestUserEmailOverride.clear(context)
    }

    @Test
    fun `get returns null when no override has been set`() {
        assertNull(TestUserEmailOverride.get(context))
    }

    @Test
    fun `set then get returns the persisted email`() {
        TestUserEmailOverride.set(context, "user@example.com")
        assertEquals("user@example.com", TestUserEmailOverride.get(context))
    }

    @Test
    fun `clear removes a previously persisted email`() {
        TestUserEmailOverride.set(context, "user@example.com")
        TestUserEmailOverride.clear(context)
        assertNull(TestUserEmailOverride.get(context))
    }

    @Test
    fun `blank emails are treated as no override`() {
        TestUserEmailOverride.set(context, "   ")
        assertNull(TestUserEmailOverride.get(context))
    }
}
