package com.iterable.integration.tests.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.iterable.integration.tests.R
import com.iterable.integration.tests.TestConstants
import com.iterable.integration.tests.capture.RequestCapture
import com.iterable.integration.tests.capture.maskApiKey
import com.iterable.integration.tests.utils.BcitProject
import com.iterable.integration.tests.utils.BcitProjects
import com.iterable.integration.tests.utils.IntegrationTestUtils
import com.iterable.integration.tests.utils.ProjectSwitchDriver
import com.iterable.integration.tests.utils.SwitchEventLog
import com.iterable.integration.tests.utils.TestUserEmailOverride
import com.iterable.integration.tests.utils.maskEmail
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableApiHelper

class ProjectSwitchTestActivity : AppCompatActivity() {

    companion object {
        /** Project A's test user email, so the CI test can run the screen against its dated users. */
        const val EXTRA_TEST_USER_EMAIL = "test_user_email"
    }

    private lateinit var stateTextView: TextView
    private lateinit var resultTextView: TextView
    private lateinit var reidentifySwitch: SwitchCompat
    private lateinit var logTextView: TextView
    private val apiHelper = IterableApiHelper()
    private val buttonsNeedingBUser = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_switch_test)

        stateTextView = findViewById(R.id.tvProjectState)
        resultTextView = findViewById(R.id.tvSwitchResult)
        reidentifySwitch = findViewById(R.id.swReidentify)
        logTextView = findViewById(R.id.tvSwitchLog)

        setupButtons()
        if (savedInstanceState == null) {
            startRequestCapture()
        }
        ensureTestUserOnB()
    }

    override fun onResume() {
        super.onResume()
        SwitchEventLog.onChange = { runOnUiThread { render() } }
        render()
    }

    override fun onPause() {
        super.onPause()
        SwitchEventLog.onChange = null
    }

    private fun setupButtons() {
        bind(R.id.btnSwitchToA, BcitProjects.A) { switchTo(BcitProjects.A) }
        bind(R.id.btnSwitchToB, BcitProjects.B) { switchTo(BcitProjects.B) }
        bind(R.id.btnSwitchToC, BcitProjects.C) { switchTo(BcitProjects.C) }
        bind(R.id.btnSwitchToD, BcitProjects.D) { switchTo(BcitProjects.D) }
        bind(R.id.btnRapidABA, BcitProjects.A, BcitProjects.B) {
            listOf(BcitProjects.A, BcitProjects.B, BcitProjects.A).forEach { switchTo(it) }
        }
        bind(R.id.btnRapidBCDC, BcitProjects.B, BcitProjects.C, BcitProjects.D) {
            listOf(BcitProjects.B, BcitProjects.C, BcitProjects.D, BcitProjects.C).forEach { switchTo(it) }
        }
        bind(R.id.btnCallDuringSwitch, BcitProjects.A, BcitProjects.B) { callDuringSwitch() }
        findViewById<Button>(R.id.btnClearSwitchLog).setOnClickListener { SwitchEventLog.clear() }
    }

    /** Buttons that reach B or D stay disabled until B's test user is known to exist. */
    private fun bind(id: Int, vararg required: BcitProject, action: () -> Unit) {
        val button = findViewById<Button>(id)
        val configured = required.all { it.isConfigured }
        val needsBUser = required.any { it == BcitProjects.B || it == BcitProjects.D }
        button.isEnabled = configured && !needsBUser
        if (configured && needsBUser) buttonsNeedingBUser.add(button)
        button.setOnClickListener { action() }
    }

    private fun testUserEmail(): String = intent.getStringExtra(EXTRA_TEST_USER_EMAIL)
        ?: TestUserEmailOverride.get(this)
        ?: TestConstants.TEST_USER_EMAIL

    private fun testUserEmailFor(project: BcitProject): String =
        BcitProjects.testUserEmailFor(project, testUserEmail())

    private fun switchTo(project: BcitProject) {
        val email = if (reidentifySwitch.isChecked) testUserEmailFor(project) else null
        ProjectSwitchDriver.switchTo(this, project, email)
    }

    private fun callDuringSwitch() {
        val target = if (ProjectSwitchDriver.lastRequestedLabel == BcitProjects.B.label) BcitProjects.A else BcitProjects.B
        switchTo(target)
        ProjectSwitchDriver.setEmail(testUserEmailFor(target))
        ProjectSwitchDriver.track()
    }

    private fun startRequestCapture() {
        SwitchEventLog.attachToRequestCapture()
        RequestCapture.install()
        if (RequestCapture.isInstalled) {
            SwitchEventLog.info("request capture on")
        } else {
            SwitchEventLog.info("request capture unavailable, NET lines will be missing")
        }
        SwitchEventLog.info("project B id=${BcitProjects.projectIdB.ifBlank { "<unset>" }}")
    }

    private fun ensureTestUserOnB() {
        if (!BcitProjects.isBConfigured) return
        val email = testUserEmailFor(BcitProjects.B)
        val testUtils = IntegrationTestUtils(applicationContext)
        Thread {
            val ready = testUtils.ensureUserExists(email, BcitProjects.B)
            val detail = if (ready) "ready" else "not ready, ${testUtils.getLastErrorMessage() ?: "see logcat"}"
            SwitchEventLog.info("test user ${maskEmail(email)} on B $detail")
            if (ready) {
                runOnUiThread {
                    if (!isDestroyed) buttonsNeedingBUser.forEach { it.isEnabled = true }
                }
            }
        }.start()
    }

    private fun render() {
        val apiKey = apiHelper.currentApiKey()
        val api = IterableApi.getInstance()
        stateTextView.text = listOf(
            "Project: ${BcitProjects.labelFor(apiKey)}",
            "Key: ${maskApiKey(apiKey)}",
            "Base URL: ${apiHelper.currentBaseUrl() ?: "<none>"}",
            "Email: ${maskEmail(api.email)} (${userLabel(api.email)})",
            "UserId: ${api.userId?.let { "${it.take(1)}***" } ?: "<none>"}",
            "Last requested: ${ProjectSwitchDriver.lastRequestedLabel}"
        ).joinToString("\n")
        resultTextView.text = ProjectSwitchDriver.lastResult?.name ?: "none"
        logTextView.text = SwitchEventLog.lines().joinToString("\n")
    }

    /** The mask hides the `-b` suffix, so name whose test user is live. */
    private fun userLabel(email: String?): String = when (email) {
        null -> "none"
        testUserEmailFor(BcitProjects.A) -> "A user"
        testUserEmailFor(BcitProjects.B) -> "B user"
        else -> "other user"
    }
}
