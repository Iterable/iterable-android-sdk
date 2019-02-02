package com.iterable.iterableapi;

import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeferredDeepLinkTest extends BasePowerMockTest {

    private MockWebServer server;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableTestUtils.resetIterableApi();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    @Test
    public void testDDLCheckEnabled() throws Exception {
        server.enqueue(new MockResponse().setBody(IterableTestUtils.getResourceString("fp_match_success.json")));

        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableActionContext.class))).thenReturn(true);

        IterableConfig config = new IterableConfig.Builder()
                .setUrlHandler(urlHandlerMock)
                .setCheckForDeferredDeeplink(true)
                .build();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", config);

        // Verify that IterableActionRunner was called with openUrl action
        ArgumentCaptor<Uri> capturedUri = ArgumentCaptor.forClass(Uri.class);
        ArgumentCaptor<IterableActionContext> capturedActionContext = ArgumentCaptor.forClass(IterableActionContext.class);
        verify(urlHandlerMock, timeout(5000)).handleIterableURL(capturedUri.capture(), capturedActionContext.capture());
        assertEquals("https://iterable.com", capturedUri.getValue().toString());
        assertEquals(IterableActionSource.APP_LINK, capturedActionContext.getValue().source);
        assertTrue(capturedActionContext.getValue().action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL));

        // Verify that the deferred deep link is only handled once per app install
        server.enqueue(new MockResponse().setBody(IterableTestUtils.getResourceString("fp_match_success.json")));
        reset(urlHandlerMock);
        IterableTestUtils.resetIterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", config);
        verify(urlHandlerMock, after(100).never()).handleIterableURL(any(Uri.class), any(IterableActionContext.class));
    }

    @Test
    public void testDDLCheckDisabled() throws Exception {
        server.enqueue(new MockResponse().setBody(IterableTestUtils.getResourceString("fp_match_success.json")));

        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableActionContext.class))).thenReturn(true);

        IterableConfig config = new IterableConfig.Builder()
                .setUrlHandler(urlHandlerMock)
                .setCheckForDeferredDeeplink(false)
                .build();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", config);

        // Verify that the deferred deep link is never handled since it is disabled
        verify(urlHandlerMock, after(100).never()).handleIterableURL(any(Uri.class), any(IterableActionContext.class));
    }

    @Test
    public void testDDLCheckFailed() throws Exception {
        server.enqueue(new MockResponse().setBody(IterableTestUtils.getResourceString("fp_match_fail.json")));

        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableActionContext.class))).thenReturn(true);

        IterableConfig config = new IterableConfig.Builder()
                .setUrlHandler(urlHandlerMock)
                .setCheckForDeferredDeeplink(true)
                .build();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", config);

        // Verify that the URL handler wasn't called because fingerprint matcher returned no result
        verify(urlHandlerMock, after(100).never()).handleIterableURL(any(Uri.class), any(IterableActionContext.class));
    }

}
