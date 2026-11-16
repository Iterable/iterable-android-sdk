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

    @Volatile
    private var lastRequested: String? = null

    @Volatile
    var lastResult: IterableProjectSwitchResult? = null
        private set

    /** The project asked for most recently, or the live one when nothing has been asked for yet. */
    val lastRequestedLabel: String
        get() = lastRequested ?: BcitProjects.labelFor(IterableApiHelper().currentApiKey())

    fun reset() {
        lastRequested = null
        lastResult = null
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
        lastRequested = project.label
        SwitchEventLog.request(project.label)
        IterableApi.switchProject(
            context.applicationContext,
            BcitProjects.iterableProject(project),
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
        SwitchEventLog.callSetEmail(lastRequestedLabel)
        IterableApi.getInstance().setEmail(email)
    }

    fun track(name: String) {
        SwitchEventLog.callTrack(name, lastRequestedLabel)
        IterableApi.getInstance().track(name)
    }
}
