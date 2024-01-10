package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import java.util.Objects;
import java.util.Optional;

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
    public void testSyncEmptyPlacementsPayload() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        IterableEmbeddedUpdateHandler mockHandler1 = mock(IterableEmbeddedUpdateHandler.class);
        IterableEmbeddedUpdateHandler mockHandler2 = mock(IterableEmbeddedUpdateHandler.class);

        embeddedManager.addUpdateListener(mockHandler1);
        embeddedManager.addUpdateListener(mockHandler2);

        verify(mockHandler1, times(0)).onMessagesUpdated();
        verify(mockHandler2, times(0)).onMessagesUpdated();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages(0L).size());
        assertEquals(1, embeddedManager.getMessages(1L).size());

        verify(mockHandler1, times(2)).onMessagesUpdated();
        verify(mockHandler2, times(2)).onMessagesUpdated();

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_empty.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertNull(embeddedManager.getMessages(0L));
        assertNull(embeddedManager.getMessages(1L));

        verify(mockHandler1, times(4)).onMessagesUpdated();
        verify(mockHandler2, times(4)).onMessagesUpdated();
    }

    @Test
    public void testOnMessagesUpdatedWithEmptyLocalStorage() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_empty.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        IterableEmbeddedUpdateHandler mockHandler1 = mock(IterableEmbeddedUpdateHandler.class);
        IterableEmbeddedUpdateHandler mockHandler2 = mock(IterableEmbeddedUpdateHandler.class);

        embeddedManager.addUpdateListener(mockHandler1);
        embeddedManager.addUpdateListener(mockHandler2);

        verify(mockHandler1, times(0)).onMessagesUpdated();
        verify(mockHandler2, times(0)).onMessagesUpdated();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_empty.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        verify(mockHandler1, times(0)).onMessagesUpdated();
        verify(mockHandler2, times(0)).onMessagesUpdated();
    }

    @Test
    public void testMessagesUpdatedWithMessagesDiff() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages(0L).size());
        assertEquals("doibjo4590340oidiobnw", embeddedManager.getMessages(0L).get(0).getMetadata().getMessageId());
        assertEquals(1, embeddedManager.getMessages(1L).size());
        assertEquals("faert442rjasiri99", embeddedManager.getMessages(1L).get(0).getMetadata().getMessageId());

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_4.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        assertEquals(2, embeddedManager.getMessages(0L).size());
        assertEquals(0, embeddedManager.getMessages(0L).get(0).getMetadata().getPlacementId());
        assertEquals(0, embeddedManager.getMessages(0L).get(1).getMetadata().getPlacementId());
        assertEquals("gere453tsfkh698sfreqd", embeddedManager.getMessages(0L).get(1).getMetadata().getMessageId());
        assertEquals(1, embeddedManager.getMessages(1L).size());
        assertEquals("faert442rjasiri99", embeddedManager.getMessages(1L).get(0).getMetadata().getMessageId());

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_5.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        assertEquals(4, embeddedManager.getMessages(0L).size());
        assertEquals(0, embeddedManager.getMessages(0L).get(1).getMetadata().getPlacementId());
        assertEquals("bbv3dwfqhbt54rfrtjktu", embeddedManager.getMessages(0L).get(1).getMetadata().getMessageId());
        assertEquals("cdsw3frtyhj544rfryu", embeddedManager.getMessages(0L).get(2).getMetadata().getMessageId());

        assertEquals(2, embeddedManager.getMessages(1L).size());
        assertEquals(1, embeddedManager.getMessages(1L).get(0).getMetadata().getPlacementId());
        assertEquals("dwef4gtrh5ewq2ryiort5t", embeddedManager.getMessages(1L).get(0).getMetadata().getMessageId());
    }

    @Test
    public void testReset() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_empty.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        IterableEmbeddedUpdateHandler mockHandler1 = mock(IterableEmbeddedUpdateHandler.class);
        IterableEmbeddedUpdateHandler mockHandler2 = mock(IterableEmbeddedUpdateHandler.class);

        embeddedManager.addUpdateListener(mockHandler1);
        embeddedManager.addUpdateListener(mockHandler2);

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        embeddedManager.reset();
        assertNull(embeddedManager.getMessages(0L));
        assertNull(embeddedManager.getMessages(1L));
    }

    @Test
    public void testOnMessagesUpdated() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        IterableEmbeddedUpdateHandler mockHandler1 = mock(IterableEmbeddedUpdateHandler.class);
        IterableEmbeddedUpdateHandler mockHandler2 = mock(IterableEmbeddedUpdateHandler.class);

        embeddedManager.addUpdateListener(mockHandler1);
        embeddedManager.addUpdateListener(mockHandler2);

        verify(mockHandler1, times(0)).onMessagesUpdated();
        verify(mockHandler2, times(0)).onMessagesUpdated();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        verify(mockHandler1, times(2)).onMessagesUpdated();
        verify(mockHandler2, times(2)).onMessagesUpdated();

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        verify(mockHandler1, times(5)).onMessagesUpdated();
        verify(mockHandler2, times(5)).onMessagesUpdated();
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
    public void testPlacementRemoval() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        assertNull(embeddedManager.getMessages(0L));
        assertEquals(1, embeddedManager.getMessages(1L).size());
        assertEquals(1, embeddedManager.getMessages(2L).size());
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