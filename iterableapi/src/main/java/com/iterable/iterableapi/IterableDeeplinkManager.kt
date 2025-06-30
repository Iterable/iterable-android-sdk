package com.iterable.iterableapi

import android.os.AsyncTask
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.regex.Pattern

internal object IterableDeeplinkManager {

    private val deeplinkPattern = Pattern.compile(IterableConstants.ITBL_DEEPLINK_IDENTIFIER)

    /**
     * Tracks a link click and passes the redirected URL to the callback
     * @param url The URL that was clicked
     * @param callback The callback to execute the original URL is retrieved
     */
    @JvmStatic
    fun getAndTrackDeeplink(url: String?, @NonNull callback: IterableHelper.IterableActionHandler) {
        if (url != null) {
            if (!IterableUtil.isUrlOpenAllowed(url)) {
                return
            }
            if (isIterableDeeplink(url)) {
                RedirectTask(callback).execute(url)
            } else {
                callback.execute(url)
            }
        } else {
            callback.execute(null)
        }
    }

    /**
     * Checks if the URL looks like a link rewritten by Iterable
     * @param url The URL to check
     * @return `true` if it looks like a link rewritten by Iterable, `false` otherwise
     */
    @JvmStatic
    fun isIterableDeeplink(url: String?): Boolean {
        if (url != null) {
            val m = deeplinkPattern.matcher(url)
            if (m.find()) {
                return true
            }
        }
        return false
    }

    private class RedirectTask(
        private val callback: IterableHelper.IterableActionHandler?
    ) : AsyncTask<String, Void, String>() {
        
        companion object {
            const val TAG = "RedirectTask"
            const val DEFAULT_TIMEOUT_MS = 3000   //3 seconds
        }

        var campaignId: Int = 0
        var templateId: Int = 0
        var messageId: String? = null

        override fun doInBackground(vararg params: String): String? {
            if (params.isEmpty()) {
                return null
            }

            var urlString = params[0]
            var urlConnection: HttpURLConnection? = null

            try {
                val url = URL(urlString)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.readTimeout = DEFAULT_TIMEOUT_MS
                urlConnection.instanceFollowRedirects = false

                val responseCode = urlConnection.responseCode

                if (responseCode >= 400) {
                    IterableLogger.d(TAG, "Invalid Request for: $urlString, returned code $responseCode")
                } else if (responseCode >= 300) {
                    urlString = urlConnection.getHeaderField(IterableConstants.LOCATION_HEADER_FIELD)
                    try {
                        val cookieHeaders = urlConnection.headerFields["Set-Cookie"]
                        if (cookieHeaders != null) {
                            val httpCookies = ArrayList<HttpCookie>(cookieHeaders.size)
                            for (cookieString in cookieHeaders) {
                                val cookies = HttpCookie.parse(cookieString)
                                if (cookies != null) {
                                    httpCookies.addAll(cookies)
                                }
                            }
                            for (cookie in httpCookies) {
                                when (cookie.name) {
                                    "iterableEmailCampaignId" -> campaignId = cookie.value.toInt()
                                    "iterableTemplateId" -> templateId = cookie.value.toInt()
                                    "iterableMessageId" -> messageId = cookie.value ?: ""
                                }
                            }
                        }
                    } catch (e: Exception) {
                        IterableLogger.e(TAG, "Error while parsing cookies: " + e.message)
                    }
                }
            } catch (e: Exception) {
                IterableLogger.e(TAG, e.message)
            } finally {
                urlConnection?.disconnect()
            }
            return urlString
        }

        override fun onPostExecute(s: String?) {
            callback?.execute(s)

            if (campaignId != 0) {
                val attributionInfo = IterableAttributionInfo(campaignId, templateId, messageId)
                IterableApi.sharedInstance.setAttributionInfo(attributionInfo)
            }
        }
    }
}
