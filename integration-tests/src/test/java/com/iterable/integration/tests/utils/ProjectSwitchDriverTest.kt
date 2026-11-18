package com.iterable.integration.tests.utils

import android.content.Context
import android.os.Looper
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableDataRegion
import com.iterable.iterableapi.IterableProjectSwitchCallback
import com.iterable.iterableapi.IterableProjectSwitchResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify

class ProjectSwitchDriverTest {

    private val gate = ProjectSwitchDriver.isCallGateRaised
    private val live = ProjectSwitchDriver.liveLabel

    private val context = mock(Context::class.java)
    private val sdkInstance = mock(IterableApi::class.java)
    private lateinit var sdk: MockedStatic<IterableApi>
    private lateinit var looper: MockedStatic<Looper>

    // IterableProject rejects a blank key, and the harness keys can be unset in a unit test run.
    private val projectA = BcitProject("A", "key-a", "", IterableDataRegion.US)
    private val projectB = BcitProject("B", "key-b", "", IterableDataRegion.US)
    private val projectC = BcitProject("C", "key-c", "", IterableDataRegion.US)

    @Before
    fun setUp() {
        sdk = mockStatic(IterableApi::class.java)
        sdk.`when`<IterableApi> { IterableApi.getInstance() }.thenReturn(sdkInstance)
        // The switch callback reads Looper, which the unit test android.jar only stubs.
        looper = mockStatic(Looper::class.java)
        ProjectSwitchDriver.reset()
        ProjectSwitchDriver.liveLabel = { "B" }
        ProjectSwitchDriver.noteRequest("C")
        SwitchEventLog.clear()
    }

    @After
    fun tearDown() {
        ProjectSwitchDriver.isCallGateRaised = gate
        ProjectSwitchDriver.liveLabel = live
        ProjectSwitchDriver.reset()
        SwitchEventLog.clear()
        looper.close()
        sdk.close()
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

    @Test
    fun `rapid drops a first step equal to the project asked for last and fires any other path in full`() {
        ProjectSwitchDriver.rapid(context, listOf(projectC, projectA)) { null }
        ProjectSwitchDriver.rapid(context, listOf(projectB, projectC)) { null }

        assertEquals(
            listOf("REQUEST switch -> A", "REQUEST switch -> B", "REQUEST switch -> C"),
            messages().filter { it.startsWith("REQUEST") }
        )
    }

    @Test
    fun `a re-identify from a callback behind a later switch names the callback and logs the call against the later switch`() {
        ProjectSwitchDriver.isCallGateRaised = { true }
        ProjectSwitchDriver.switchTo(context, projectB, "b@example.com")
        ProjectSwitchDriver.switchTo(context, projectC, null)

        switchCallbacks().first().onProjectSwitched(IterableProjectSwitchResult.SWITCHED_CLEANLY)

        assertEquals(
            listOf(
                "REQUEST switch -> B",
                "REQUEST switch -> C",
                "CALLBACK B result=SWITCHED_CLEANLY",
                "INFO re-identify from callback B",
                "CALL setEmail project=C"
            ),
            messages()
        )
        verify(sdkInstance).setEmail("b@example.com")
    }

    private fun messages(): List<String> = SwitchEventLog.lines().map { it.substringAfter(' ') }

    private fun switchCallbacks(): List<IterableProjectSwitchCallback> {
        val captor = ArgumentCaptor.forClass(IterableProjectSwitchCallback::class.java)
        sdk.verify({ IterableApi.switchProject(any(), any(), captor.capture()) }, atLeastOnce())
        return captor.allValues
    }
}
