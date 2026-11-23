package com.iterable.integration.tests.capture

import android.util.Log
import com.iterable.integration.tests.utils.BcitProjects
import java.net.URL
import java.util.ArrayDeque

/**
 * Records every request the SDK sends to an Iterable API host, in send order, with the masked
 * API key it carried, so a tester can see which project and region each call went to.
 *
 * The SDK talks to the API through `HttpURLConnection`, so capture hooks in at the
 * `URLStreamHandlerFactory` level rather than through an OkHttp interceptor.
 */
object RequestCapture {

    data class Entry(
        val timestampMs: Long,
        val method: String,
        val host: String,
        val path: String,
        val maskedApiKey: String,
        val projectLabel: String
    )

    private const val TAG = "BCIT-Capture"
    private const val MAX_ENTRIES = 500

    private val buffer = ArrayDeque<Entry>()

    @Volatile
    var isInstalled = false
        private set

    /** Called on the thread that sends the request, after the entry is buffered. */
    @Volatile
    var onRecord: ((Entry) -> Unit)? = null

    /**
     * Must run before the SDK opens its first connection. The factory can be set once per process
     * and never removed, so only call this when capture is wanted.
     */
    @Synchronized
    fun install() {
        if (isInstalled) return
        // Created before the factory exists, so it carries the platform's own https handler.
        val platformHttps = URL("https://api.iterable.com/")
        try {
            URL.setURLStreamHandlerFactory(CapturingHandlerFactory(platformHttps))
            isInstalled = true
            Log.i(TAG, "Request capture installed")
        } catch (e: Error) {
            Log.e(TAG, "Request capture unavailable, another URLStreamHandlerFactory is already set", e)
        }
    }

    fun entries(): List<Entry> = synchronized(buffer) { buffer.toList() }

    fun clear() = synchronized(buffer) { buffer.clear() }

    internal fun record(method: String, url: URL, apiKey: String?) {
        val entry = Entry(
            System.currentTimeMillis(), method, url.host, url.path, maskApiKey(apiKey), BcitProjects.labelFor(apiKey)
        )
        synchronized(buffer) {
            buffer.addLast(entry)
            if (buffer.size > MAX_ENTRIES) buffer.removeFirst()
        }
        Log.d(TAG, "${entry.method} ${entry.host}${entry.path} key=${entry.maskedApiKey} [${entry.projectLabel}]")
        onRecord?.invoke(entry)
    }
}

internal fun isIterableApiHost(host: String?): Boolean =
    host == "api.iterable.com" || (host != null && host.startsWith("api.") && host.endsWith(".iterable.com"))

/** First and last four characters, `a1b2...c3d4`, the format shared with the iOS harness. */
internal fun maskApiKey(apiKey: String?): String = when {
    apiKey.isNullOrBlank() -> "<none>"
    apiKey.length < 8 -> "****"
    else -> "${apiKey.take(4)}...${apiKey.takeLast(4)}"
}
