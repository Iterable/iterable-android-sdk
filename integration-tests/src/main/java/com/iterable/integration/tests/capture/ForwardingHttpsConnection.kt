package com.iterable.integration.tests.capture

import com.iterable.iterableapi.IterableConstants
import java.io.InputStream
import java.io.OutputStream
import java.security.Permission
import java.security.Principal
import java.security.cert.Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * Forwards everything to the platform connection and records the request once, just before it is
 * committed. The record has to happen first: request properties are unreadable once connected.
 * Convenience getters such as `getContentLength` are left to the base class, which routes them
 * through the forwarded `getHeaderField`.
 */
internal class ForwardingHttpsConnection(
    private val delegate: HttpsURLConnection
) : HttpsURLConnection(delegate.url) {

    private var recorded = false

    private fun recordOnce() {
        if (recorded) return
        recorded = true
        RequestCapture.record(
            delegate.requestMethod,
            delegate.url,
            delegate.getRequestProperty(IterableConstants.HEADER_API_KEY)
        )
    }

    // Calls that commit the request.
    override fun connect() { recordOnce(); delegate.connect() }
    override fun getOutputStream(): OutputStream { recordOnce(); return delegate.outputStream }
    override fun getInputStream(): InputStream { recordOnce(); return delegate.inputStream }
    override fun getResponseCode(): Int { recordOnce(); return delegate.responseCode }
    override fun getResponseMessage(): String? { recordOnce(); return delegate.responseMessage }
    override fun getHeaderField(name: String?): String? { recordOnce(); return delegate.getHeaderField(name) }
    override fun getHeaderField(n: Int): String? { recordOnce(); return delegate.getHeaderField(n) }
    override fun getHeaderFieldKey(n: Int): String? { recordOnce(); return delegate.getHeaderFieldKey(n) }
    override fun getHeaderFields(): Map<String, List<String>> { recordOnce(); return delegate.headerFields }

    override fun getErrorStream(): InputStream? = delegate.errorStream
    override fun disconnect() = delegate.disconnect()
    override fun usingProxy(): Boolean = delegate.usingProxy()
    override fun getPermission(): Permission = delegate.permission
    override fun toString(): String = delegate.toString()

    override fun setRequestMethod(method: String) { delegate.requestMethod = method }
    override fun getRequestMethod(): String = delegate.requestMethod
    override fun setRequestProperty(key: String, value: String?) = delegate.setRequestProperty(key, value)
    override fun addRequestProperty(key: String, value: String?) = delegate.addRequestProperty(key, value)
    override fun getRequestProperty(key: String): String? = delegate.getRequestProperty(key)
    override fun getRequestProperties(): Map<String, List<String>> = delegate.requestProperties

    override fun setDoOutput(dooutput: Boolean) { delegate.doOutput = dooutput }
    override fun getDoOutput(): Boolean = delegate.doOutput
    override fun setDoInput(doinput: Boolean) { delegate.doInput = doinput }
    override fun getDoInput(): Boolean = delegate.doInput
    override fun setUseCaches(usecaches: Boolean) { delegate.useCaches = usecaches }
    override fun getUseCaches(): Boolean = delegate.useCaches
    override fun setAllowUserInteraction(allowuserinteraction: Boolean) { delegate.allowUserInteraction = allowuserinteraction }
    override fun getAllowUserInteraction(): Boolean = delegate.allowUserInteraction
    override fun setIfModifiedSince(ifmodifiedsince: Long) { delegate.ifModifiedSince = ifmodifiedsince }
    override fun getIfModifiedSince(): Long = delegate.ifModifiedSince
    override fun setConnectTimeout(timeout: Int) { delegate.connectTimeout = timeout }
    override fun getConnectTimeout(): Int = delegate.connectTimeout
    override fun setReadTimeout(timeout: Int) { delegate.readTimeout = timeout }
    override fun getReadTimeout(): Int = delegate.readTimeout
    override fun setInstanceFollowRedirects(followRedirects: Boolean) { delegate.instanceFollowRedirects = followRedirects }
    override fun getInstanceFollowRedirects(): Boolean = delegate.instanceFollowRedirects
    override fun setFixedLengthStreamingMode(contentLength: Int) = delegate.setFixedLengthStreamingMode(contentLength)
    override fun setFixedLengthStreamingMode(contentLength: Long) = delegate.setFixedLengthStreamingMode(contentLength)
    override fun setChunkedStreamingMode(chunklen: Int) = delegate.setChunkedStreamingMode(chunklen)

    override fun getCipherSuite(): String = delegate.cipherSuite
    override fun getLocalCertificates(): Array<Certificate>? = delegate.localCertificates
    override fun getServerCertificates(): Array<Certificate> = delegate.serverCertificates
    override fun getPeerPrincipal(): Principal = delegate.peerPrincipal
    override fun getLocalPrincipal(): Principal? = delegate.localPrincipal
    override fun setHostnameVerifier(v: HostnameVerifier) { delegate.hostnameVerifier = v }
    override fun getHostnameVerifier(): HostnameVerifier = delegate.hostnameVerifier
    override fun setSSLSocketFactory(sf: SSLSocketFactory) { delegate.sslSocketFactory = sf }
    override fun getSSLSocketFactory(): SSLSocketFactory = delegate.sslSocketFactory
}
