package com.iterable.iterableapi;

import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for IterableWebChromeClient
 * Verifies that resize is only triggered when page is fully loaded (100% progress)
 */
public class IterableWebChromeClientTest extends BaseTest {

    @Mock
    private IterableWebView.HTMLNotificationCallbacks mockCallbacks;

    private IterableWebChromeClient webChromeClient;
    private WebView mockWebView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        webChromeClient = new IterableWebChromeClient(mockCallbacks);
        mockWebView = mock(WebView.class);
    }

    @Test
    public void testOnProgressChanged_TriggersResizeAt100Percent() {
        // Test: Resize should be triggered when progress reaches 100%
        webChromeClient.onProgressChanged(mockWebView, 100);

        verify(mockCallbacks, times(1)).runResizeScript();
    }

    @Test
    public void testOnProgressChanged_DoesNotTriggerBelow100Percent() {
        // Test: Resize should NOT be triggered for progress < 100%
        webChromeClient.onProgressChanged(mockWebView, 0);
        webChromeClient.onProgressChanged(mockWebView, 50);
        webChromeClient.onProgressChanged(mockWebView, 99);

        verify(mockCallbacks, never()).runResizeScript();
    }

    @Test
    public void testOnProgressChanged_MultipleProgressUpdates() {
        // Test: Multiple progress updates should only trigger resize once at 100%
        webChromeClient.onProgressChanged(mockWebView, 0);
        webChromeClient.onProgressChanged(mockWebView, 25);
        webChromeClient.onProgressChanged(mockWebView, 50);
        webChromeClient.onProgressChanged(mockWebView, 75);
        webChromeClient.onProgressChanged(mockWebView, 99);
        webChromeClient.onProgressChanged(mockWebView, 100);

        // Should only be called once at 100%
        verify(mockCallbacks, times(1)).runResizeScript();
    }

    @Test
    public void testOnProgressChanged_Multiple100PercentCalls() {
        // Test: If 100% is called multiple times, resize should be called each time
        // (though this is unlikely in practice, we test the behavior)
        webChromeClient.onProgressChanged(mockWebView, 100);
        webChromeClient.onProgressChanged(mockWebView, 100);
        webChromeClient.onProgressChanged(mockWebView, 100);

        verify(mockCallbacks, times(3)).runResizeScript();
    }

    @Test
    public void testOnProgressChanged_ProgressSequence() {
        // Test: Realistic progress sequence from 0 to 100
        int[] progressValues = {0, 10, 25, 50, 75, 90, 95, 99, 100};

        for (int progress : progressValues) {
            webChromeClient.onProgressChanged(mockWebView, progress);
        }

        // Should only be called once at 100%
        verify(mockCallbacks, times(1)).runResizeScript();
    }

    @Test
    public void testOnProgressChanged_EdgeCases() {
        // Test: Edge cases for progress values
        webChromeClient.onProgressChanged(mockWebView, -1); // Invalid negative
        webChromeClient.onProgressChanged(mockWebView, 101); // Invalid > 100
        webChromeClient.onProgressChanged(mockWebView, 100); // Valid 100%

        // Only 100% should trigger
        verify(mockCallbacks, times(1)).runResizeScript();
    }
}

