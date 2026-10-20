package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

final class IterableDeeplinkRedirectResolver {
    static final int DEFAULT_TIMEOUT_MS = 3000;

    private static final String TAG = "RedirectTask";
    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final String CAMPAIGN_ID_COOKIE = "iterableEmailCampaignId";
    private static final String TEMPLATE_ID_COOKIE = "iterableTemplateId";
    private static final String MESSAGE_ID_COOKIE = "iterableMessageId";

    interface ConnectionFactory {
        HttpURLConnection open(String url) throws Exception;
    }

    private final ConnectionFactory connectionFactory;

    IterableDeeplinkRedirectResolver() {
        this(url -> (HttpURLConnection) new URL(url).openConnection());
    }

    IterableDeeplinkRedirectResolver(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @NonNull
    IterableDeeplinkRedirectResult resolve(@NonNull String url) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url);
            return resolveResponse(url, connection);
        } catch (Exception e) {
            IterableLogger.e(TAG, e.getMessage());
            return new IterableDeeplinkRedirectResult(url);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(String url) throws Exception {
        HttpURLConnection connection = connectionFactory.open(url);
        connection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
        connection.setReadTimeout(DEFAULT_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        return connection;
    }

    private IterableDeeplinkRedirectResult resolveResponse(
            String originalUrl,
            HttpURLConnection connection
    ) throws Exception {
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            IterableLogger.d(
                    TAG,
                    "Invalid Request for: " + originalUrl + ", returned code " + responseCode
            );
            return new IterableDeeplinkRedirectResult(originalUrl);
        }
        if (responseCode >= 300) {
            return resolveRedirectResponse(originalUrl, connection);
        }
        return new IterableDeeplinkRedirectResult(originalUrl);
    }

    private IterableDeeplinkRedirectResult resolveRedirectResponse(
            String originalUrl,
            HttpURLConnection connection
    ) {
        String redirectUrl = connection.getHeaderField(
                IterableConstants.LOCATION_HEADER_FIELD
        );
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            return new IterableDeeplinkRedirectResult(originalUrl);
        }

        RedirectAttribution attribution = parseAttributionCookies(connection);
        return new IterableDeeplinkRedirectResult(
                redirectUrl,
                attribution.campaignId,
                attribution.templateId,
                attribution.messageId
        );
    }

    private RedirectAttribution parseAttributionCookies(HttpURLConnection connection) {
        int campaignId = 0;
        int templateId = 0;
        String messageId = null;

        try {
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            List<String> cookies = headerFields.get(SET_COOKIE_HEADER);
            if (cookies != null) {
                for (String header : cookies) {
                    for (HttpCookie cookie : HttpCookie.parse(header)) {
                        switch (cookie.getName()) {
                            case CAMPAIGN_ID_COOKIE:
                                campaignId = Integer.parseInt(cookie.getValue());
                                break;
                            case TEMPLATE_ID_COOKIE:
                                templateId = Integer.parseInt(cookie.getValue());
                                break;
                            case MESSAGE_ID_COOKIE:
                                messageId = cookie.getValue();
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing cookies: " + e.getMessage());
        }

        return new RedirectAttribution(campaignId, templateId, messageId);
    }

    private static final class RedirectAttribution {
        final int campaignId;
        final int templateId;
        final String messageId;

        RedirectAttribution(int campaignId, int templateId, String messageId) {
            this.campaignId = campaignId;
            this.templateId = templateId;
            this.messageId = messageId;
        }
    }
}

final class IterableDeeplinkRedirectResult {
    @Nullable
    final String url;
    final int campaignId;
    final int templateId;
    @Nullable
    final String messageId;

    IterableDeeplinkRedirectResult(@Nullable String url) {
        this(url, 0, 0, null);
    }

    IterableDeeplinkRedirectResult(
            @Nullable String url,
            int campaignId,
            int templateId,
            @Nullable String messageId
    ) {
        this.url = url;
        this.campaignId = campaignId;
        this.templateId = templateId;
        this.messageId = messageId;
    }
}
