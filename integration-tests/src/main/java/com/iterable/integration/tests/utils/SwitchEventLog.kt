package com.iterable.integration.tests.utils

import com.iterable.integration.tests.capture.RequestCapture
import com.iterable.iterableapi.IterableProjectSwitchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ordered, process-wide log for the project switch screen. Line format is shared with the iOS
 * harness, so keep the verbs and spacing identical: `HH:mm:ss.SSS <VERB> ...`.
 */
object SwitchEventLog {

    private const val MAX_LINES = 1000

    private val lines = ArrayList<String>()

    /** Called on the thread that appended the line. */
    @Volatile
    var onChange: (() -> Unit)? = null

    fun request(label: String) = append("REQUEST switch -> $label")

    fun callback(label: String, result: IterableProjectSwitchResult) = append("CALLBACK $label result=${result.name}")

    fun callSetEmail(project: String) = append("CALL setEmail project=$project")

    fun callTrack(name: String, project: String) = append("CALL track name=$name project=$project")

    fun net(entry: RequestCapture.Entry) = append(
        "NET ${entry.method} ${entry.host}${entry.path} key=${entry.maskedApiKey} [${entry.projectLabel}]",
        entry.timestampMs
    )

    fun info(text: String) = append("INFO $text")

    fun lines(): List<String> = synchronized(lines) { lines.toList() }

    fun clear() {
        synchronized(lines) { lines.clear() }
        onChange?.invoke()
    }

    /** Feeds captured requests into the log as `NET` lines. */
    fun attachToRequestCapture() {
        RequestCapture.onRecord = { net(it) }
    }

    internal fun format(timestampMs: Long, message: String): String =
        "${SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestampMs))} $message"

    private fun append(message: String, timestampMs: Long = System.currentTimeMillis()) {
        synchronized(lines) {
            lines.add(format(timestampMs, message))
            if (lines.size > MAX_LINES) lines.removeAt(0)
        }
        onChange?.invoke()
    }
}
