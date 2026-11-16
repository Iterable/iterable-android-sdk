package com.iterable.integration.tests.utils

import android.content.Context
import android.os.Looper
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableApiHelper
import com.iterable.iterableapi.IterableProjectSwitchCallback
import com.iterable.iterableapi.IterableProjectSwitchResult

/**
 * Issues switchProject requests and the SDK calls around them, logging each to [SwitchEventLog].
 * Shared by the switch screen and ProjectSwitchIntegrationTest so both drive the same path.
 */
object ProjectSwitchDriver {

    /** Event name shared with the iOS harness. */
    const val TRACK_EVENT_NAME = "bcit_project_switch"

    @Volatile
    private var lastRequested: String? = null

    @Volatile
    var lastResult: IterableProjectSwitchResult? = null
        private set

    /** Whether the SDK's switch call gate is raised; queueOrExecute queues calls while it is. */
    internal var isCallGateRaised: () -> Boolean = { IterableApi.isSDKInitializing() }

    internal var liveLabel: () -> String = { BcitProjects.labelFor(IterableApiHelper().currentApiKey()) }

    /** The project asked for most recently, or the live one when nothing has been asked for yet. */
    val lastRequestedLabel: String
        get() = lastRequested ?: liveLabel()

    /**
     * The project a call made now is logged against: the one asked for last while the gate queues
     * it, the live one once the gate is down. Read from the gate rather than from the callback,
     * because a callback can run with a later switch's gate already raised (SDK-856).
     */
    val callLabel: String
        get() = if (isCallGateRaised()) lastRequestedLabel else liveLabel()

    fun reset() {
        lastRequested = null
        lastResult = null
    }

    internal fun noteRequest(label: String) {
        lastRequested = label
        SwitchEventLog.request(label)
    }

    /**
     * Fires [path] back to back without waiting, as iOS rapidFire does. A first step equal to the
     * project asked for last is dropped, since the SDK would report it as already there.
     */
    fun rapid(context: Context, path: List<BcitProject>, reidentifyEmailFor: (BcitProject) -> String?) {
        SwitchEventLog.info("rapid ${path.joinToString(" -> ") { it.label }}")
        val steps = if (path.firstOrNull()?.label == lastRequestedLabel) path.drop(1) else path
        steps.forEach { switchTo(context, it, reidentifyEmailFor(it)) }
    }

    /**
     * Repeats the project asked for last, then switches to [target] without waiting. The repeat
     * reports already there with its callback posted, which then runs behind [target]'s raised
     * gate, so a re-identify from it is queued for [target] (SDK-856).
     */
    fun repeatThenSwitch(context: Context, target: BcitProject, reidentifyEmailFor: (BcitProject) -> String?) {
        val current = BcitProjects.all.firstOrNull { it.label == lastRequestedLabel && it.isConfigured }
        if (current == null) {
            SwitchEventLog.info("repeat then switch needs a known current project, got $lastRequestedLabel")
            return
        }
        SwitchEventLog.info("repeat ${current.label} then switch -> ${target.label} (SDK-856)")
        switchTo(context, current, reidentifyEmailFor(current))
        switchTo(context, target, reidentifyEmailFor(target))
    }

    /**
     * Requests a switch without waiting for it. When [reidentifyEmail] is set, it is passed to
     * `setEmail` from inside the callback, because the switch clears identity.
     */
    fun switchTo(
        context: Context,
        project: BcitProject,
        reidentifyEmail: String?,
        onSwitched: ((IterableProjectSwitchResult) -> Unit)? = null
    ) {
        noteRequest(project.label)
        IterableApi.switchProject(
            context.applicationContext,
            BcitProjects.iterableProject(context, project),
            IterableProjectSwitchCallback { result ->
                lastResult = result
                SwitchEventLog.callback(project.label, result)
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    SwitchEventLog.info("callback for ${project.label} ran off the main thread")
                }
                reidentifyEmail?.let { setEmail(it) }
                onSwitched?.invoke(result)
            }
        )
    }

    fun setEmail(email: String) {
        SwitchEventLog.callSetEmail(callLabel)
        IterableApi.getInstance().setEmail(email)
    }

    fun track(name: String = TRACK_EVENT_NAME) {
        SwitchEventLog.callTrack(name, callLabel)
        IterableApi.getInstance().track(name)
    }
}
