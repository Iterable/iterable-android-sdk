package com.iterable.integration.tests.capture

import android.util.Log
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
        val maskedApiKey: String
    )

    private const val TAG = "BCIT-Capture"
    private const val MAX_ENTRIES = 500

    private val buffer = ArrayDeque<Entry>()

    @Volatile
    var isInstalled = false
        private set

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
        val entry = Entry(System.currentTimeMillis(), method, url.host, url.path, maskApiKey(apiKey))
        synchronized(buffer) {
            buffer.addLast(entry)
            if (buffer.size > MAX_ENTRIES) buffer.removeFirst()
        }
        Log.d(TAG, "${entry.method} ${entry.host}${entry.path} key=${entry.maskedApiKey}")
    }
}

internal fun isIterableApiHost(host: String?): Boolean =
    host == "api.iterable.com" || (host != null && host.startsWith("api.") && host.endsWith(".iterable.com"))

/** Last four characters only, enough to tell projects apart without leaking the key. */
internal fun maskApiKey(apiKey: String?): String = when {
    apiKey.isNullOrBlank() -> "<none>"
    apiKey.length < 8 -> "****"
    else -> "****${apiKey.takeLast(4)}"
}
