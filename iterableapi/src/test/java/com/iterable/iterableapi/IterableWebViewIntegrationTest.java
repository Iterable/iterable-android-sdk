package com.iterable.iterableapi;

import android.content.Context;
import android.webkit.WebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class IterableWebViewIntegrationTest extends BaseTest {

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

    @Test
    public void testCreateWithHtml_DefaultConfiguration() {
        // Test: WebView uses empty string as base URL when not configured
        IterableWebView.HTMLNotificationCallbacks mockCallbacks = 
            new MockHTMLNotificationCallbacks();
        
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
        
        assertEquals("Default base URL should be empty string", "", baseUrlCaptor.getValue());
    }

    @Test
    public void testCreateWithHtml_CustomConfiguration() {
        // Test: WebView uses configured base URL
        String customBaseUrl = "https://app.iterable.com";
        
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(customBaseUrl)
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        IterableWebView.HTMLNotificationCallbacks mockCallbacks = 
            new MockHTMLNotificationCallbacks();
        
        String testHtml = "<html><body>Test Content with Custom Fonts</body></html>";
        
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
        
        assertEquals("Custom base URL should be used", customBaseUrl, baseUrlCaptor.getValue());
    }

    @Test
    public void testCreateWithHtml_EUConfiguration() {
        // Test: WebView uses EU base URL for CORS compliance
        String euBaseUrl = "https://app.eu.iterable.com";
        
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(euBaseUrl)
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        IterableWebView.HTMLNotificationCallbacks mockCallbacks = 
            new MockHTMLNotificationCallbacks();
        
        String testHtml = "<html><head><link href='https://webfonts.wolt.com/index.css' rel='stylesheet'></head><body>EU Content</body></html>";
        
        webViewSpy.createWithHtml(mockCallbacks, testHtml);
        
        // Verify loadDataWithBaseURL was called with EU base URL
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(webViewSpy).loadDataWithBaseURL(
            baseUrlCaptor.capture(),
            eq(testHtml),
            eq(IterableWebView.MIME_TYPE),
            eq(IterableWebView.ENCODING),
            eq("")
        );
        
        assertEquals("EU base URL should be used for CORS", euBaseUrl, baseUrlCaptor.getValue());
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
