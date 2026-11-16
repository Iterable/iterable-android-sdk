package com.iterable.integration.tests.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectSwitchDriverTest {

    private val gate = ProjectSwitchDriver.isCallGateRaised
    private val live = ProjectSwitchDriver.liveLabel

    @Before
    fun setUp() {
        ProjectSwitchDriver.reset()
        ProjectSwitchDriver.liveLabel = { "B" }
        ProjectSwitchDriver.noteRequest("C")
    }

    @After
    fun tearDown() {
        ProjectSwitchDriver.isCallGateRaised = gate
        ProjectSwitchDriver.liveLabel = live
        ProjectSwitchDriver.reset()
    }

    @Test
    fun `while the gate is raised a call is logged against the project asked for last`() {
        ProjectSwitchDriver.isCallGateRaised = { true }
        assertEquals("C", ProjectSwitchDriver.callLabel)
    }

    @Test
    fun `once the gate is down a call is logged against the live project`() {
        ProjectSwitchDriver.isCallGateRaised = { false }
        assertEquals("B", ProjectSwitchDriver.callLabel)
    }
}
