package com.iterable.integration.tests.capture

import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import javax.net.ssl.HttpsURLConnection

/**
 * Wraps https connections to Iterable API hosts and leaves every other connection, and every
 * other protocol, to the platform.
 *
 * Real connections are opened through URLs built against [platformHttps]. A URL built against a
 * context URL of the same protocol inherits the context's handler, which reaches the platform's
 * https handler without reflection on hidden APIs and keeps this handler from recursing into itself.
 */
internal class CapturingHandlerFactory(private val platformHttps: URL) : URLStreamHandlerFactory {
    override fun createURLStreamHandler(protocol: String): URLStreamHandler? =
        if (protocol.equals("https", ignoreCase = true)) CapturingHttpsHandler(platformHttps) else null
}

private class CapturingHttpsHandler(private val platformHttps: URL) : URLStreamHandler() {

    override fun openConnection(u: URL): URLConnection =
        wrap(URL(platformHttps, u.toExternalForm()).openConnection())

    override fun openConnection(u: URL, p: Proxy): URLConnection =
        wrap(URL(platformHttps, u.toExternalForm()).openConnection(p))

    override fun getDefaultPort(): Int = 443

    private fun wrap(connection: URLConnection): URLConnection =
        if (connection is HttpsURLConnection && isIterableApiHost(connection.url.host)) {
            ForwardingHttpsConnection(connection)
        } else {
            connection
        }
}
