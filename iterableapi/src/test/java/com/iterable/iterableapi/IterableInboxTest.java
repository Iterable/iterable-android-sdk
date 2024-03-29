package com.iterable.iterableapi;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class IterableInboxTest extends BaseTest {

    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableCustomActionHandler customActionHandler;
    private IterableUrlHandler urlHandler;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);

        inAppHandler = mock(IterableInAppHandler.class);
        customActionHandler = mock(IterableCustomActionHandler.class);
        urlHandler = mock(IterableUrlHandler.class);
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableApi.sharedInstance = new IterableApi();
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder
                        .setInAppHandler(inAppHandler)
                        .setCustomActionHandler(customActionHandler)
                        .setUrlHandler(urlHandler);
            }
        });
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

    @Test
    public void testInboxMessageOrdering() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertEquals(2, inboxMessages.size());
        assertEquals("message2", inboxMessages.get(0).getMessageId());
        assertEquals("message4", inboxMessages.get(1).getMessageId());
    }

    @Test
    public void testRemoveMessageSuccessCallbackOnSuccessfulResponse() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json")));
        final IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertEquals(2, inboxMessages.size());
        assertEquals(1, inAppManager.getUnreadInboxMessagesCount());

        final JSONObject responseData = new JSONObject("{\"key\":\"value\"}");
        dispatcher.enqueueResponse("/events/inAppConsume", new MockResponse().setResponseCode(200).setBody(responseData.toString()));

        inAppManager.removeMessage(inboxMessages.get(0), null, null, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                signal.countDown();
            }
        }, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertFalse(true);
            }
        });
        shadowOf(getMainLooper()).idle();
        assertTrue("Message remove success callback called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testRemoveMessageFailureCallbackOnFailedResponse() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json")));
        final IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertEquals(2, inboxMessages.size());
        assertEquals(1, inAppManager.getUnreadInboxMessagesCount());

        final JSONObject responseData = new JSONObject("{\"key\":\"value\"}");
        dispatcher.enqueueResponse("/events/inAppConsume", new MockResponse().setResponseCode(500).setBody(responseData.toString()));

        inAppManager.removeMessage(inboxMessages.get(0), null, null, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                assertFalse(true);
            }
        }, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                signal.countDown();
            }
        });
        shadowOf(getMainLooper()).idle();
        assertTrue("Message remove failure callback called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testSetRead() throws Exception {
        // Set up mock response
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json")));

        // Initialize in-app manager and wait for messages to be synced
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Get inbox messages
        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();

        // Verify initial state
        assertEquals(1, inAppManager.getUnreadInboxMessagesCount());
        assertEquals(2, inboxMessages.size());
        assertFalse(inboxMessages.get(0).isRead());
        assertTrue(inboxMessages.get(1).isRead());

        // Set first message as read with a callback
        final boolean[] callbackCalled = { false };
        inAppManager.setRead(inboxMessages.get(0), true, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                callbackCalled[0] = true;
                assertTrue(callbackCalled[0]);
            }
        }, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertFalse(true);
            }
        });

        // Wait for callback to be called
        shadowOf(getMainLooper()).idle();

        // Verify that callback was called and that message is marked as read
        assertTrue(callbackCalled[0]);
        assertEquals(0, inAppManager.getUnreadInboxMessagesCount());
        assertEquals(2, inAppManager.getInboxMessages().size());
        assertTrue(inboxMessages.get(0).isRead());
        assertTrue(inboxMessages.get(1).isRead());
    }

    @Test
    public void testMessageReadStatusFromServer() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertEquals(1, inAppManager.getUnreadInboxMessagesCount());
        assertEquals(2, inboxMessages.size());
        assertTrue(inboxMessages.get(1).isRead());
        assertFalse(inboxMessages.get(0).isRead());
    }

    @Test
    public void testShowInboxMessageImmediate() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_show.json")));

        // Reset the existing IterableApi
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        IterableInAppDisplayer inAppDisplayerMock = mock(IterableInAppDisplayer.class);
        when(inAppDisplayerMock.showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), any(IterableHelper.IterableUrlCallback.class))).thenReturn(true);
        IterableInAppManager inAppManager = spy(new IterableInAppManager(IterableApi.sharedInstance, new IterableDefaultInAppHandler(), 30.0, new IterableInAppMemoryStorage(), IterableActivityMonitor.getInstance(), inAppDisplayerMock));
        IterableApi.sharedInstance = new IterableApi(inAppManager);
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setInAppHandler(inAppHandler).setCustomActionHandler(customActionHandler).setUrlHandler(urlHandler);
            }
        });

        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), any(IterableHelper.IterableUrlCallback.class));

        assertEquals(2, inAppManager.getInboxMessages().size());
        assertEquals(1, inAppManager.getUnreadInboxMessagesCount());
    }

    @Test
    public void testInboxNewMessagesCallback() throws Exception {
        IterableInAppManager.Listener listenerMock = mock(IterableInAppManager.Listener.class);

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        shadowOf(getMainLooper()).idle();
        inAppManager.addListener(listenerMock);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_update.json")));
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getInboxMessages().size());
        assertEquals("message1", inAppManager.getInboxMessages().get(0).getMessageId());
        verify(listenerMock).onInboxUpdated();
        reset(listenerMock);

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_update2.json")));
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(2, inAppManager.getInboxMessages().size());
        assertEquals("message1", inAppManager.getInboxMessages().get(0).getMessageId());
        assertEquals("message2", inAppManager.getInboxMessages().get(1).getMessageId());
        verify(listenerMock).onInboxUpdated();
    }
}
