package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
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
        assertEquals(1, embeddedManager.getMessages("0").size());
        assertEquals("doibjo4590340oidiobnw", embeddedManager.getMessages("0").get(0).getMetadata().getMessageId());

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_single_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();

        assertEquals(1, embeddedManager.getMessages("1").size());
        assertEquals("dffe4fgfrews3f", embeddedManager.getMessages("1").get(0).getMetadata().getMessageId());
    }

    @Test
    public void testSyncEmbeddedMultiplePlacements() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages("0").size());
        assertEquals("doibjo4590340oidiobnw", embeddedManager.getMessages("0").get(0).getMetadata().getMessageId());
        assertEquals(1, embeddedManager.getMessages("1").size());
        assertEquals("faert442rjasiri99", embeddedManager.getMessages("1").get(0).getMetadata().getMessageId());

        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_2.json")));
        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages("1").size());
        assertEquals("ewsd3fdrtj6ty", embeddedManager.getMessages("1").get(0).getMetadata().getMessageId());

        assertEquals(1, embeddedManager.getMessages("2").size());
        assertEquals("grewdvb54ut87y", embeddedManager.getMessages("2").get(0).getMetadata().getMessageId());
    }

    @Test
    public void testReset() throws Exception {
        dispatcher.enqueueResponse("/embedded-messaging/messages", new MockResponse().setBody(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json")));
        IterableEmbeddedManager embeddedManager = IterableApi.getInstance().getEmbeddedManager();

        embeddedManager.syncMessages();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, embeddedManager.getMessages("0").size());
        assertEquals(1, embeddedManager.getMessages("1").size());
        embeddedManager.reset();
        assertEquals(0, embeddedManager.getMessages("0").size());
        assertEquals(0, embeddedManager.getMessages("1").size());
    }
}