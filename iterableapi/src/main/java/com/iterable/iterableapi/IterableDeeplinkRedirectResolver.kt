package com.iterable.iterableapi

import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL

internal class IterableDeeplinkRedirectResolver {

    fun resolve(url: String): IterableDeeplinkRedirectResult {
        var connection: HttpURLConnection? = null

        return try {
            connection = openConnection(url)
            resolveResponse(url, connection)
        } catch (e: Exception) {
            IterableLogger.e(TAG, e.message)
            IterableDeeplinkRedirectResult(url)
        } finally {
            connection?.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            readTimeout = DEFAULT_TIMEOUT_MS
            instanceFollowRedirects = false
        }
    }

    private fun resolveResponse(
        originalUrl: String,
        connection: HttpURLConnection
    ): IterableDeeplinkRedirectResult {
        val responseCode = connection.responseCode

        return when {
            responseCode >= 400 -> {
                IterableLogger.d(
                    TAG,
                    "Invalid Request for: $originalUrl, returned code $responseCode"
                )
                IterableDeeplinkRedirectResult(originalUrl)
            }
            responseCode >= 300 -> resolveRedirectResponse(connection)
            else -> IterableDeeplinkRedirectResult(originalUrl)
        }
    }

    private fun resolveRedirectResponse(
        connection: HttpURLConnection
    ): IterableDeeplinkRedirectResult {
        val redirectUrl = connection.getHeaderField(
            IterableConstants.LOCATION_HEADER_FIELD
        )
        val attribution = parseAttributionCookies(connection)

        return IterableDeeplinkRedirectResult(
            redirectUrl,
            attribution.campaignId,
            attribution.templateId,
            attribution.messageId
        )
    }

    private fun parseAttributionCookies(
        connection: HttpURLConnection
    ): RedirectAttribution {
        var campaignId = 0
        var templateId = 0
        var messageId: String? = null

        try {
            connection.headerFields[SET_COOKIE_HEADER]
                ?.flatMap(HttpCookie::parse)
                ?.forEach { cookie ->
                    when (cookie.name) {
                        CAMPAIGN_ID_COOKIE -> campaignId = cookie.value.toInt()
                        TEMPLATE_ID_COOKIE -> templateId = cookie.value.toInt()
                        MESSAGE_ID_COOKIE -> messageId = cookie.value
                    }
                }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while parsing cookies: ${e.message}")
        }

        return RedirectAttribution(campaignId, templateId, messageId)
    }

    private data class RedirectAttribution(
        val campaignId: Int,
        val templateId: Int,
        val messageId: String?
    )

    private companion object {
        const val TAG = "RedirectTask"
        const val DEFAULT_TIMEOUT_MS = 3000
        const val SET_COOKIE_HEADER = "Set-Cookie"
        const val CAMPAIGN_ID_COOKIE = "iterableEmailCampaignId"
        const val TEMPLATE_ID_COOKIE = "iterableTemplateId"
        const val MESSAGE_ID_COOKIE = "iterableMessageId"
    }
}

internal data class IterableDeeplinkRedirectResult(
    val url: String?,
    val campaignId: Int = 0,
    val templateId: Int = 0,
    val messageId: String? = null
)
