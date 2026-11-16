package com.iterable.integration.tests

import android.util.Log
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iterable.integration.tests.activities.ProjectSwitchTestActivity
import com.iterable.integration.tests.capture.RequestCapture
import com.iterable.integration.tests.utils.BcitProject
import com.iterable.integration.tests.utils.BcitProjects
import com.iterable.integration.tests.utils.ProjectSwitchDriver
import com.iterable.iterableapi.IterableApiHelper
import com.iterable.iterableapi.IterableProjectSwitchResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * BCIT coverage for switchProject (SDK-760), asserted through request capture: once a switch
 * callback has fired, SDK requests must carry the destination project's key.
 *
 * The result enum is logged, not asserted. CI has no FCM token, so there is no device disable to
 * confirm and SWITCHED_WITH_WARNINGS is the expected result there.
 *
 * Skips when project B is not configured, so forks without the BCIT_*_B secrets stay green.
 */
@RunWith(AndroidJUnit4::class)
class ProjectSwitchIntegrationTest : BaseIntegrationTest() {

    companion object {
        private const val TAG = "ProjectSwitchTest"
        private const val SWITCH_TIMEOUT_SECONDS = 60L
        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val QUIET_PERIOD_MS = 3000L
        private const val TRACK_PATH = "/api/events/track"

        // Leaving a project sends its disableDevice with that project's key, and the callback does
        // not wait for it, so it can be captured after the callback.
        private const val DISABLE_DEVICE_PATH = "/api/users/disableDevice"
    }

    private val apiHelper = IterableApiHelper()
    private lateinit var screen: ActivityScenario<ProjectSwitchTestActivity>
    private var ready = false

    @Before
    override fun setUp() {
        Assume.assumeTrue("Project B is not configured, skipping", BcitProjects.isBConfigured)
        // Installed here rather than through ITERABLE_REQUEST_CAPTURE, so other BCIT runs keep
        // building with capture off. The factory is process-wide and cannot be removed, so test
        // classes that run after this one in the same process also send through capture.
        RequestCapture.install()
        Assume.assumeTrue("Request capture could not be installed", RequestCapture.isInstalled)

        super.setUp()
        ProjectSwitchDriver.reset()
        assertTrue(
            "Could not create the test user on project B: ${testUtils.getLastErrorMessage()}",
            testUtils.ensureUserExists(testUserEmail, BcitProjects.B)
        )

        // The SDK only sends its offline task queue, which carries track, while the app is in the
        // foreground, and BaseIntegrationTest.setUp sends it to the home screen.
        screen = ActivityScenario.launch(ProjectSwitchTestActivity::class.java)
        assertTrue(
            "Project switch screen did not resume",
            waitForCondition({ screen.state == Lifecycle.State.RESUMED }, 10)
        )
        ready = true
    }

    @After
    override fun tearDown() {
        // Unconditional: mid-teardown the live key is still the project being left, so it cannot
        // tell whether a switch is in flight. Already on A reports straight away.
        if (ready) {
            val latch = CountDownLatch(1)
            ProjectSwitchDriver.switchTo(context, BcitProjects.A, testUserEmail) { latch.countDown() }
            if (!latch.await(SWITCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.e(TAG, "Switching back to A did not complete during tearDown")
            }
        }
        if (::screen.isInitialized) screen.close()
        super.tearDown()
    }

    @Test
    fun switchFromAToBSendsLaterRequestsWithBKey() {
        assertEquals(BcitProjects.A.apiKey, apiHelper.currentApiKey())
        waitForQuietCapture()

        val latch = CountDownLatch(1)
        switchClearingCaptureFirst(BcitProjects.B) { result ->
            Log.d(TAG, "A -> B result=$result")
            latch.countDown()
        }
        assertTrue("A -> B callback did not fire", latch.await(SWITCH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertEquals(BcitProjects.B.apiKey, apiHelper.currentApiKey())

        var shownResult: String? = null
        screen.onActivity { shownResult = it.findViewById<TextView>(R.id.tvSwitchResult).text.toString() }
        assertEquals("The screen should show the result verbatim", ProjectSwitchDriver.lastResult?.name, shownResult)

        assertTrackedAndOnlyOn(BcitProjects.B, "bcit_project_switch_a_to_b")
    }

    @Test
    fun rapidSwitchFromAToBAndBackSettlesOnA() {
        assertEquals(BcitProjects.A.apiKey, apiHelper.currentApiKey())
        waitForQuietCapture()

        val callbacks = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(2)
        // No re-identify on B: traffic from identifying on B could commit after A's callback.
        ProjectSwitchDriver.switchTo(context, BcitProjects.B, null) { result ->
            Log.d(TAG, "A -> B result=$result")
            callbacks.add(BcitProjects.B.label)
            latch.countDown()
        }
        switchClearingCaptureFirst(BcitProjects.A) { result ->
            Log.d(TAG, "B -> A result=$result")
            callbacks.add(BcitProjects.A.label)
            latch.countDown()
        }
        assertTrue(
            "Both callbacks should fire, got $callbacks",
            latch.await(SWITCH_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        )
        assertEquals("Callbacks should fire in request order", listOf("B", "A"), callbacks.toList())
        assertEquals(BcitProjects.A.apiKey, apiHelper.currentApiKey())

        assertTrackedAndOnlyOn(BcitProjects.A, "bcit_project_switch_a_b_a")
    }

    /**
     * Clears the capture inside the callback, before re-identifying, so everything captured
     * afterwards was sent once the SDK reported being on [project].
     */
    private fun switchClearingCaptureFirst(project: BcitProject, onSwitched: (IterableProjectSwitchResult) -> Unit) {
        ProjectSwitchDriver.switchTo(context, project, null) { result ->
            RequestCapture.clear()
            ProjectSwitchDriver.setEmail(testUserEmail)
            onSwitched(result)
        }
    }

    private fun assertTrackedAndOnlyOn(project: BcitProject, eventName: String) {
        ProjectSwitchDriver.track(eventName)
        assertTrue(
            "No track request with ${project.label}'s key was captured: ${describe(RequestCapture.entries())}",
            waitForCondition({
                RequestCapture.entries().any { it.path == TRACK_PATH && it.projectLabel == project.label }
            }, REQUEST_TIMEOUT_SECONDS)
        )
        Thread.sleep(QUIET_PERIOD_MS)

        val captured = RequestCapture.entries()
        Log.d(TAG, "Captured after the ${project.label} callback: ${describe(captured)}")
        val offProject = captured.filter { it.path != DISABLE_DEVICE_PATH && it.projectLabel != project.label }
        assertTrue(
            "Requests after the ${project.label} callback carried another key: ${describe(offProject)}",
            offProject.isEmpty()
        )
    }

    /** Lets requests started on A before the switch finish, so they are not mistaken for leaks. */
    private fun waitForQuietCapture() {
        RequestCapture.clear()
        val deadline = System.currentTimeMillis() + REQUEST_TIMEOUT_SECONDS * 1000
        var lastCount = -1
        while (System.currentTimeMillis() < deadline) {
            val count = RequestCapture.entries().size
            if (count == lastCount) return
            lastCount = count
            Thread.sleep(QUIET_PERIOD_MS)
        }
    }

    private fun describe(entries: List<RequestCapture.Entry>): String =
        entries.joinToString(prefix = "[", postfix = "]") { "${it.method} ${it.path} [${it.projectLabel}]" }
}
