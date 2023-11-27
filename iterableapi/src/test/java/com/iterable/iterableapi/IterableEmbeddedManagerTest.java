package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class IterableEmbeddedManagerTest extends BaseTest {
    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);

        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableApi.sharedInstance = new IterableApi();
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder;
            }
        });
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    @Test
    public void testSyncEmbeddedSinglePlacement() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_single_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages(0L).size());
        assertEquals("doibjo4590340oidiobnw", embeddedManager.getMessages(0L).get(0).getMetadata().getMessageId());

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_single_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        assertEquals(1, embeddedManager.getMessages(1L).size());
        assertEquals("dffe4fgfrews3f", embeddedManager.getMessages(1L).get(0).getMetadata().getMessageId());
    }

    @Test
    public void testSyncEmbeddedMultiplePlacements() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages(0L).size());
        assertEquals("doibjo4590340oidiobnw", embeddedManager.getMessages(0L).get(0).getMetadata().getMessageId());
        assertEquals(1, embeddedManager.getMessages(1L).size());
        assertEquals("faert442rjasiri99", embeddedManager.getMessages(1L).get(0).getMetadata().getMessageId());

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages(1L).size());
        assertEquals("ewsd3fdrtj6ty", embeddedManager.getMessages(1L).get(0).getMetadata().getMessageId());

        assertEquals(1, embeddedManager.getMessages(2L).size());
        assertEquals("grewdvb54ut87y", embeddedManager.getMessages(2L).get(0).getMetadata().getMessageId());
    }

    @Test
    public void testReset() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages(0L).size());
        assertEquals(1, embeddedManager.getMessages(1L).size());
        embeddedManager.reset();
        assertEquals(0, embeddedManager.getMessages(0L).size());
        assertEquals(0, embeddedManager.getMessages(1L).size());
    }

    @Test
    public void testOnMessagesUpdated() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        IterableEmbeddedUpdateHandler mockHandler1 = mock(IterableEmbeddedUpdateHandler.class);
        IterableEmbeddedUpdateHandler mockHandler2 = mock(IterableEmbeddedUpdateHandler.class);

        embeddedManager.addUpdateListener(mockHandler1);
        embeddedManager.addUpdateListener(mockHandler2);

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        verify(mockHandler1, times(4)).onMessagesUpdated();
        verify(mockHandler2, times(4)).onMessagesUpdated();
    }

    @Test
    public void testTrackEmbeddedMessageReceived() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));

        IterableApi iterableApiSpy = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = iterableApiSpy;

        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        verify(iterableApiSpy).trackEmbeddedMessageReceived(embeddedManager.getMessages(0L).get(0));
        verify(iterableApiSpy).trackEmbeddedMessageReceived(embeddedManager.getMessages(1L).get(0));

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_3.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        verify(iterableApiSpy).trackEmbeddedMessageReceived(embeddedManager.getMessages(0L).get(1));
        verify(iterableApiSpy, never()).trackEmbeddedMessageReceived(embeddedManager.getMessages(0L).get(0));
        verify(iterableApiSpy, never()).trackEmbeddedMessageReceived(embeddedManager.getMessages(1L).get(0));
    }

    @Test
    public void testOnEmbeddedMessagingDisabled() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setResponseCode(401).setBody(IterableTestUtils.getResourceString("embedded_payload_bad_api_key.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        IterableEmbeddedUpdateHandler mockHandler1 = mock(IterableEmbeddedUpdateHandler.class);

        embeddedManager.addUpdateListener(mockHandler1);

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        verify(mockHandler1).onEmbeddedMessagingDisabled();
    }
}