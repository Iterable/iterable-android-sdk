package com.iterable.integration.tests.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSwitchDriverTest {

    @Test
    fun `a call made inside a switch callback is logged against the project that landed`() {
        ProjectSwitchDriver.inCallbackOf("B") {
            assertEquals("B", ProjectSwitchDriver.callLabel)
        }
    }
}
