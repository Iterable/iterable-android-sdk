package com.iterable.integration.tests.utils

import com.iterable.integration.tests.capture.RequestCapture
import com.iterable.iterableapi.IterableProjectSwitchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwitchEventLogTest {

    private val timestamp = Regex("""^\d{2}:\d{2}:\d{2}\.\d{3} """)

    @Before
    fun setUp() {
        SwitchEventLog.onChange = null
        SwitchEventLog.clear()
    }

    @Test
    fun `writes every verb in the shared format, in call order`() {
        SwitchEventLog.request("B")
        SwitchEventLog.callback("B", IterableProjectSwitchResult.SWITCHED_WITH_WARNINGS)
        SwitchEventLog.callSetEmail("B")
        SwitchEventLog.callTrack("bcit_project_switch", "B")
        SwitchEventLog.net(
            RequestCapture.Entry(0L, "GET", "api.iterable.com", "/api/inApp/getMessages", "785f...16f8", "B")
        )
        SwitchEventLog.info("free text")

        val lines = SwitchEventLog.lines()
        lines.forEach { assertTrue(it, timestamp.containsMatchIn(it)) }
        assertEquals(
            listOf(
                "REQUEST switch -> B",
                "CALLBACK B result=SWITCHED_WITH_WARNINGS",
                "CALL setEmail project=B",
                "CALL track name=bcit_project_switch project=B",
                "NET GET api.iterable.com/api/inApp/getMessages key=785f...16f8 [B]",
                "INFO free text"
            ),
            lines.map { it.replace(timestamp, "") }
        )
    }

    @Test
    fun `clear empties the log`() {
        SwitchEventLog.info("x")
        SwitchEventLog.clear()
        assertEquals(emptyList<String>(), SwitchEventLog.lines())
    }
}
