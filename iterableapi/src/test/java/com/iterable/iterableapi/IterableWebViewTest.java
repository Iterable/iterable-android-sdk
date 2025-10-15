package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class IterableWebViewTest extends BaseTest {

    private IterableWebView webView;
    private IterableWebView webViewSpy;

    @Before
    public void setUp() {
        IterableTestUtils.createIterableApiNew();
        webView = new IterableWebView(getContext());
        webViewSpy = spy(webView);
    }

    @After
    public void tearDown() {
        IterableTestUtils.resetIterableApi();
    }

    // ===== Base URL Configuration Tests =====

    @Test
    public void testGetWebViewBaseUrl_DefaultConfiguration() {
        // Test: When webViewBaseUrl is not configured, should return empty string
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Default webViewBaseUrl should be empty string", "", baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_CustomConfiguration() {
        // Test: When webViewBaseUrl is configured, should return the configured value
        String customBaseUrl = "https://app.iterable.com";

        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(customBaseUrl)
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Custom webViewBaseUrl should be returned", customBaseUrl, baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_EUConfiguration() {
        // Test: EU region configuration
        String euBaseUrl = "https://app.eu.iterable.com";

        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(euBaseUrl)
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("EU webViewBaseUrl should be returned", euBaseUrl, baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_NullConfiguration() {
        // Test: When webViewBaseUrl is explicitly set to null, should return empty string
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(null)
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Null webViewBaseUrl should return empty string", "", baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_EmptyStringConfiguration() {
        // Test: When webViewBaseUrl is explicitly set to empty string, should return empty string
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl("")
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Empty webViewBaseUrl should return empty string", "", baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_ExceptionHandling() {
        // Test: Exception handling when SDK is not initialized properly
        IterableTestUtils.resetIterableApi();

        // This should not throw an exception and should return empty string
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Exception case should return empty string", "", baseUrl);
    }

    // ===== WebView Integration Tests =====

    @Test
    public void testCreateWithHtml_DefaultConfiguration_UsesEmptyBaseUrl() {
        // Test: WebView uses empty string as base URL when not configured (about:blank origin)
        MockHTMLNotificationCallbacks mockCallbacks = new MockHTMLNotificationCallbacks();
        String testHtml = "<html><body>Test Content</body></html>";

        webViewSpy.createWithHtml(mockCallbacks, testHtml);

        // Verify loadDataWithBaseURL was called with empty string (default behavior)
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(webViewSpy).loadDataWithBaseURL(
            baseUrlCaptor.capture(),
            eq(testHtml),
            eq(IterableWebView.MIME_TYPE),
            eq(IterableWebView.ENCODING),
            eq("")
        );

        assertEquals("Default base URL should be empty string (about:blank origin)", "", baseUrlCaptor.getValue());
    }

    @Test
    public void testCreateWithHtml_CustomConfiguration_UsesConfiguredBaseUrl() {
        // Test: WebView uses configured base URL to enable CORS for external resources
        String customBaseUrl = "https://app.iterable.com";

        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(customBaseUrl)
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        MockHTMLNotificationCallbacks mockCallbacks = new MockHTMLNotificationCallbacks();
        String testHtml = "<html><head><link href='https://webfonts.wolt.com/index.css' rel='stylesheet'></head><body>Custom Fonts</body></html>";

        webViewSpy.createWithHtml(mockCallbacks, testHtml);

        // Verify loadDataWithBaseURL was called with custom base URL
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(webViewSpy).loadDataWithBaseURL(
            baseUrlCaptor.capture(),
            eq(testHtml),
            eq(IterableWebView.MIME_TYPE),
            eq(IterableWebView.ENCODING),
            eq("")
        );

        assertEquals("Custom base URL should enable CORS for external resources", customBaseUrl, baseUrlCaptor.getValue());
    }

    @Test
    public void testCreateWithHtml_EUConfiguration_EnablesCORSForWoltFonts() {
        // Test: WebView uses EU base URL for CORS compliance with Wolt's self-hosted fonts
        String euBaseUrl = "https://app.eu.iterable.com";

        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(euBaseUrl)
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        MockHTMLNotificationCallbacks mockCallbacks = new MockHTMLNotificationCallbacks();
        String woltHtml = "<html><head><link href='https://webfonts.wolt.com/index.css' rel='stylesheet'></head><body>Wolt Content with Custom Fonts</body></html>";

        webViewSpy.createWithHtml(mockCallbacks, woltHtml);

        // Verify loadDataWithBaseURL was called with EU base URL
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(webViewSpy).loadDataWithBaseURL(
            baseUrlCaptor.capture(),
            eq(woltHtml),
            eq(IterableWebView.MIME_TYPE),
            eq(IterableWebView.ENCODING),
            eq("")
        );

        assertEquals("EU base URL should enable CORS for Wolt's custom fonts", euBaseUrl, baseUrlCaptor.getValue());
    }

    @Test
    public void testCreateWithHtml_CustomDomain_EnablesCORSForAnyDomain() {
        // Test: Custom domain configuration works for any customer domain
        String customDomain = "https://custom.example.com";

        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(customDomain)
                .build();

        IterableApi.initialize(getContext(), "test-api-key", config);

        MockHTMLNotificationCallbacks mockCallbacks = new MockHTMLNotificationCallbacks();
        String testHtml = "<html><body>Custom Domain Content</body></html>";

        webViewSpy.createWithHtml(mockCallbacks, testHtml);

        // Verify loadDataWithBaseURL was called with custom domain
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(webViewSpy).loadDataWithBaseURL(
            baseUrlCaptor.capture(),
            eq(testHtml),
            eq(IterableWebView.MIME_TYPE),
            eq(IterableWebView.ENCODING),
            eq("")
        );

        assertEquals("Custom domain should be used for CORS", customDomain, baseUrlCaptor.getValue());
    }

    // Mock implementation for testing
    private static class MockHTMLNotificationCallbacks implements IterableWebView.HTMLNotificationCallbacks {
        @Override
        public void onUrlClicked(String url) {
            // Mock implementation
        }

        @Override
        public void setLoaded(boolean loaded) {
            // Mock implementation
        }

        @Override
        public void runResizeScript() {
            // Mock implementation
        }
    }
}
