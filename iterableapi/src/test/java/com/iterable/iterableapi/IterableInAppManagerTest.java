package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import java.util.List;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.shadows.ShadowDialog;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.junit.Assert.assertNotNull;

public class IterableInAppManagerTest extends BaseTest {

    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableCustomActionHandler customActionHandler;
    private IterableUrlHandler urlHandler;
    private PausedExecutorService backgroundExecutor;

    @Before
    public void setUp() throws IOException {
        backgroundExecutor = new PausedExecutorService();
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
        IterableInAppFragmentHTMLNotification.notification = null;
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

    @Ignore("Ignoring due to stalling")
    @Test
    public void testDoNotShowMultipleTimes() throws Exception {
        ActivityController<FragmentActivity> controller = Robolectric.buildActivity(FragmentActivity.class).create().start().resume();
        FragmentActivity activity = controller.get();
        boolean shownFirstTime = IterableInAppDisplayer.showIterableFragmentNotificationHTML(activity, "", "", null, 0.0, new Rect(), true, new IterableInAppMessage.InAppBgColor(null, 0.0f), false, IterableInAppLocation.IN_APP);
        boolean shownSecondTime = IterableInAppDisplayer.showIterableFragmentNotificationHTML(activity, "", "", null, 0.0, new Rect(), true, new IterableInAppMessage.InAppBgColor(null, 0.0f), false, IterableInAppLocation.IN_APP);
        shadowOf(getMainLooper()).idle();
        assertTrue(shownFirstTime);
        assertFalse(shownSecondTime);
        ShadowDialog.getLatestDialog().dismiss();
        controller.pause().stop().destroy();
    }

    @Ignore("Ignoring due to stalling")
    @Test
    public void testIfDialogDoesNotDestroysAfterConfigurationChange() throws Exception {
        ActivityController<FragmentActivity> controller = Robolectric.buildActivity(FragmentActivity.class).create().start().resume();
        FragmentActivity activity = controller.get();
        assertTrue(IterableInAppDisplayer.showIterableFragmentNotificationHTML(activity, "", "", null, 0.0, new Rect(), true,
                new IterableInAppMessage.InAppBgColor(null, 0.0f), false, IterableInAppLocation.IN_APP));
        shadowOf(getMainLooper()).idle();
        controller.configurationChange();
        assertEquals(1, activity.getFragmentManager().getFragments().size());
        ShadowDialog.getLatestDialog().dismiss();
        controller.pause().stop().destroy();
    }

    @Ignore("Ignoring due to stalling")
    @Test
    public void testIfDialogFragmentExistAfterRotation() throws Exception {
        ActivityController controller = Robolectric.buildActivity(FragmentActivity.class).create().start().resume();
        FragmentActivity activity = (FragmentActivity) controller.get();
        assertTrue(IterableInAppDisplayer.showIterableFragmentNotificationHTML(activity, "", "", null, 0.0, new Rect(), true,
                new IterableInAppMessage.InAppBgColor(null, 0.0f), false, IterableInAppLocation.IN_APP));
        shadowOf(getMainLooper()).idle();
        controller.configurationChange();
        assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
        ShadowDialog.getLatestDialog().dismiss();
        controller.pause().stop().destroy();
    }

    @Test
    public void testSyncInApp() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        assertEquals(0, inAppManager.getMessages().size());

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getMessages().size());
        assertEquals("7kx2MmoGdCpuZao9fDueuQoXVAZuDaVV", inAppManager.getMessages().get(0).getMessageId());

        // Check that we remove the old message if it doesn't exist in the new queue state
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single2.json")));
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getMessages().size());
        assertEquals("Q19mD2NlQUnxnmSGuQu9ujzkKR6c12TogeaGA29", inAppManager.getMessages().get(0).getMessageId());
    }

    @Test
    public void testReset() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        assertEquals(0, inAppManager.getMessages().size());

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getMessages().size());
        inAppManager.reset();
        assertEquals(0, inAppManager.getMessages().size());
    }

    @Test
    public void testProcessAfterForeground() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));

        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();
        shadowOf(getMainLooper()).runToEndOfTasks();

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getMessages().size());

        ArgumentCaptor<IterableInAppMessage> inAppMessageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler).onNewInApp(inAppMessageCaptor.capture());
        assertEquals("7kx2MmoGdCpuZao9fDueuQoXVAZuDaVV", inAppMessageCaptor.getValue().getMessageId());
    }

    @Test
    public void testInAppMessageExpiration() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        JSONObject jsonMessage = jsonArray.getJSONObject(0).put(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, System.currentTimeMillis() + 60 * 1000);
        JSONObject expiredJsonMessage = new JSONObject(jsonMessage.toString()).put(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, System.currentTimeMillis() - 60 * 1000);
        jsonArray.put(expiredJsonMessage);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getMessages().size());

        doReturn(System.currentTimeMillis() + 120 * 1000).when(utilsRule.iterableUtilSpy).currentTimeMillis();
        assertEquals(0, inAppManager.getMessages().size());
    }

    @Test
    public void testNotProcessingNeverTriggerType() throws Exception {
        // Test on a message with trigger = immediate
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        jsonArray.getJSONObject(0).put(IterableConstants.ITERABLE_IN_APP_TRIGGER, triggerWithType("immediate")).put(IterableConstants.KEY_MESSAGE_ID, "messageId1");
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        ArgumentCaptor<IterableInAppMessage> inAppMessageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler).onNewInApp(inAppMessageCaptor.capture());
        assertEquals("messageId1", inAppMessageCaptor.getValue().getMessageId());
        reset(inAppHandler);

        // Test on a message with trigger = never
        jsonArray.getJSONObject(0).put(IterableConstants.ITERABLE_IN_APP_TRIGGER, triggerWithType("never")).put(IterableConstants.KEY_MESSAGE_ID, "messageId2");
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        inAppManager.syncInApp();
        verify(inAppHandler, never()).onNewInApp(inAppMessageCaptor.capture());
    }

    private JSONObject triggerWithType(String triggerType) throws JSONException {
        return new JSONObject().putOpt("type", triggerType);
    }

    @Test
    public void testListenerCalledOnMainThread() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        final IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        shadowOf(getMainLooper()).idle();

        IterableInAppManager.Listener listener = mock(IterableInAppManager.Listener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertSame("Callback is called on the main thread", getMainLooper().getThread(), Thread.currentThread());
                return null;
            }
        }).when(listener).onInboxUpdated();
        inAppManager.addListener(listener);

        //Remove from a background thread and verify that it switches threads for the callback
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                inAppManager.removeMessage("7kx2MmoGdCpuZao9fDueuQoXVAZuDaVV");
            }
        });
        backgroundExecutor.runAll();
        shadowOf(getMainLooper()).idle();

        verify(listener, timeout(100)).onInboxUpdated();
    }

    @Test
    public void testHandleActionLink() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));

        // Reset the existing IterableApi
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        IterableInAppDisplayer inAppDisplayerMock = mock(IterableInAppDisplayer.class);
        IterableInAppManager inAppManager = spy(new IterableInAppManager(IterableApi.sharedInstance, new IterableDefaultInAppHandler(), 30.0, new IterableInAppMemoryStorage(), IterableActivityMonitor.getInstance(), inAppDisplayerMock));
        IterableApi.sharedInstance = new IterableApi(inAppManager);
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setInAppHandler(inAppHandler).setCustomActionHandler(customActionHandler).setUrlHandler(urlHandler);
            }
        });
        doReturn(true).when(urlHandler).handleIterableURL(any(Uri.class), any(IterableActionContext.class));

        // Bring the app into foreground to trigger in-app display
        Robolectric.buildActivity(Activity.class).create().start().resume();
        Robolectric.flushForegroundThreadScheduler();
        ArgumentCaptor<IterableHelper.IterableUrlCallback> callbackCaptor = ArgumentCaptor.forClass(IterableHelper.IterableUrlCallback.class);
        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), callbackCaptor.capture());
        IterableInAppMessage message = inAppManager.getMessages().get(0);
        callbackCaptor.getValue().execute(Uri.parse("action://actionName"));

        // Verify that the action handler was called with the correct action
        ArgumentCaptor<IterableAction> actionCaptor = ArgumentCaptor.forClass(IterableAction.class);
        ArgumentCaptor<IterableActionContext> contextCaptor = ArgumentCaptor.forClass(IterableActionContext.class);
        verify(customActionHandler).handleIterableCustomAction(actionCaptor.capture(), contextCaptor.capture());
        assertEquals("actionName", actionCaptor.getValue().getType());
        assertEquals(IterableActionSource.IN_APP, contextCaptor.getValue().source);

        // Verify that legacy itbl:// links are also routed to the custom action handler
        reset(inAppDisplayerMock);
        reset(customActionHandler);
        inAppManager.showMessage(message);
        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), callbackCaptor.capture());
        callbackCaptor.getValue().execute(Uri.parse("itbl://legacyCustomAction"));
        verify(customActionHandler).handleIterableCustomAction(actionCaptor.capture(), contextCaptor.capture());
        assertEquals("legacyCustomAction", actionCaptor.getValue().getType());
        assertEquals(IterableActionSource.IN_APP, contextCaptor.getValue().source);

        reset(inAppDisplayerMock);
        inAppManager.showMessage(message);
        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), callbackCaptor.capture());
        callbackCaptor.getValue().execute(Uri.parse("https://www.google.com"));
        ArgumentCaptor<Uri> urlCaptor = ArgumentCaptor.forClass(Uri.class);
        verify(urlHandler).handleIterableURL(urlCaptor.capture(), contextCaptor.capture());
        assertEquals("https://www.google.com", urlCaptor.getValue().toString());
        assertEquals(IterableActionSource.IN_APP, contextCaptor.getValue().source);

        // Verify that iterable:// links are not routed to either custom action handler or url handler
        reset(inAppDisplayerMock);
        reset(customActionHandler);
        reset(urlHandler);
        inAppManager.showMessage(message);
        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), callbackCaptor.capture());
        callbackCaptor.getValue().execute(Uri.parse("iterable://someInternalAction"));
        verify(customActionHandler, never()).handleIterableCustomAction(any(IterableAction.class), any(IterableActionContext.class));
        verify(urlHandler, never()).handleIterableURL(any(Uri.class), any(IterableActionContext.class));
    }

    @Test
    public void testHandleCustomActionDelete() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));

        // Reset the existing IterableApi
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        IterableInAppDisplayer inAppDisplayerMock = mock(IterableInAppDisplayer.class);
        IterableInAppManager inAppManager = spy(new IterableInAppManager(IterableApi.sharedInstance, new IterableSkipInAppHandler(), 30.0, new IterableInAppMemoryStorage(), IterableActivityMonitor.getInstance(), inAppDisplayerMock));
        IterableApi.sharedInstance = new IterableApi(inAppManager);
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setInAppHandler(inAppHandler).setCustomActionHandler(customActionHandler).setUrlHandler(urlHandler);
            }
        });
        doReturn(true).when(urlHandler).handleIterableURL(any(Uri.class), any(IterableActionContext.class));

        // Bring the app into foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        Robolectric.flushForegroundThreadScheduler();
        IterableInAppMessage message = inAppManager.getMessages().get(0);

        // Verify that message is not consumed by default if consume = false and iterable://dismiss is clicked
        reset(inAppDisplayerMock);
        reset(customActionHandler);
        reset(urlHandler);
        inAppManager.showMessage(message, false, null);
        ArgumentCaptor<IterableHelper.IterableUrlCallback> callbackCaptor = ArgumentCaptor.forClass(IterableHelper.IterableUrlCallback.class);
        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.IN_APP), callbackCaptor.capture());
        callbackCaptor.getValue().execute(Uri.parse("iterable://dismiss"));
        assertFalse(message.isConsumed());

        // Verify that message is consumed if iterable://delete is called
        reset(inAppDisplayerMock);
        reset(customActionHandler);
        reset(urlHandler);
        inAppManager.showMessage(message, false, null, IterableInAppLocation.INBOX);
        verify(inAppDisplayerMock).showMessage(any(IterableInAppMessage.class), eq(IterableInAppLocation.INBOX), callbackCaptor.capture());
        callbackCaptor.getValue().execute(Uri.parse("iterable://delete"));
        assertTrue(message.isConsumed());
    }

    @Test
    public void testInAppAutoDisplayPause() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        assertEquals(0, inAppManager.getMessages().size());

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();
        assertEquals(1, inAppManager.getMessages().size());

        inAppManager.setAutoDisplayPaused(true);
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).create().start().resume();
        Robolectric.flushForegroundThreadScheduler();
        ArgumentCaptor<IterableInAppMessage> inAppMessageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler, times(0)).onNewInApp(inAppMessageCaptor.capture());

        inAppManager.setAutoDisplayPaused(false);
        verify(inAppHandler, times(1)).onNewInApp(inAppMessageCaptor.capture());
    }

    @Test
    public void testMessagePersistentReadStateFromServer() throws Exception {
        // load the in-app that has not been synchronized with the server yet (read state is set to false)
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_read_state_1.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertFalse(inboxMessages.get(0).isRead());

        // now load the one that has the in-app with read state set to true
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_read_state_2.json")));
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        assertTrue(inboxMessages.get(0).isRead());
    }

    @Test
    public void testJsonOnlyMessageProcessing() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(createJsonOnlyPayload()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        // First sync to get messages
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Bring app to foreground to trigger processing of immediate messages
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify immediate trigger message was processed
        ArgumentCaptor<IterableInAppMessage> messageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler).onNewInApp(messageCaptor.capture());

        IterableInAppMessage message = messageCaptor.getValue();
        assertEquals("message1", message.getMessageId());
        assertEquals("immediate", message.getCustomPayload().getString("key"));
        assertTrue(message.isJsonOnly());

        // Verify never trigger message was not processed
        verify(inAppHandler, never()).onNewInApp(argThat(msg ->
            msg.getMessageId().equals("message2")));
    }

    @Test
    public void testJsonOnlyMessageDisplay() throws Exception {
        // Create payload with only a never-trigger message
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject().put("key", "never"))
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message1")));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        // Create InAppManager with mock displayer
        IterableInAppDisplayer mockDisplayer = mock(IterableInAppDisplayer.class);
        IterableInAppManager inAppManager = spy(new IterableInAppManager(
                IterableApi.sharedInstance,
                new IterableDefaultInAppHandler(),
                30.0,
                new IterableInAppMemoryStorage(),
                IterableActivityMonitor.getInstance(),
                mockDisplayer));
        IterableApi.sharedInstance = new IterableApi(inAppManager);

        // Process messages
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Only the "never" trigger message should remain in queue
        assertEquals(1, inAppManager.getMessages().size());

        // Verify no messages were displayed
        verify(mockDisplayer, never()).showMessage(
            any(IterableInAppMessage.class),
            any(IterableInAppLocation.class),
            any(IterableHelper.IterableUrlCallback.class));
    }

    @Test
    public void testJsonOnlyMessageInboxBehavior() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", true)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject()
                                        .put("key", "value"))
                                .put("inboxMetadata", new JSONObject()
                                        .put("title", "Test Title")
                                        .put("subtitle", "Test Subtitle"))
                                .put("messageId", "message1")));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        // Directly sync messages since we're testing inbox state rather than trigger behavior
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Verify message is not in inbox
        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertEquals(0, inboxMessages.size());
    }

    @Test
    public void testJsonOnlyMessageContentBehavior() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject()
                                        .put("key", "customValue"))
                                .put("content", new JSONObject()
                                        .put("payload", new JSONObject()
                                                .put("key", "contentValue")))
                                .put("trigger", new JSONObject().put("type", "immediate"))
                                .put("messageId", "message1")));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        // First sync to get messages
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Bring app to foreground to trigger immediate message
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify customPayload is used instead of content.payload
        ArgumentCaptor<IterableInAppMessage> messageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler).onNewInApp(messageCaptor.capture());
        assertEquals("customValue", messageCaptor.getValue().getCustomPayload().getString("key"));
    }

    @Test
    public void testJsonOnlyInAppMessageParsing() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("messageType", "Mobile")
                                .put("typeOfContent", "Static")
                                .put("customPayload", new JSONObject()
                                        .put("key1", "value1")
                                        .put("key2", 42)
                                        .put("key3", new JSONObject().put("nested", true)))
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message1")
                                .put("campaignId", 1)));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        List<IterableInAppMessage> messages = inAppManager.getMessages();
        assertEquals(1, messages.size());

        IterableInAppMessage message = messages.get(0);
        assertEquals("value1", message.getCustomPayload().getString("key1"));
        assertEquals(42, message.getCustomPayload().getInt("key2"));
        assertTrue(message.getCustomPayload().getJSONObject("key3").getBoolean("nested"));
    }

    @Test
    public void testJsonOnlyInAppMessageDelegateCallbacks() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject().put("key", "immediate"))
                                .put("trigger", new JSONObject().put("type", "immediate"))
                                .put("messageId", "message1"))
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject().put("key", "never"))
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message2")));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        // Create InAppManager with mock handler
        final IterableInAppHandler inAppHandler = mock(IterableInAppHandler.class);
        IterableInAppManager inAppManager = spy(new IterableInAppManager(
                IterableApi.sharedInstance,
                inAppHandler,
                30.0,
                new IterableInAppMemoryStorage(),
                IterableActivityMonitor.getInstance(),
                mock(IterableInAppDisplayer.class)));
        IterableApi.sharedInstance = new IterableApi(inAppManager);

        // First sync to get messages
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Process messages by bringing app to foreground
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify immediate trigger message was processed
        ArgumentCaptor<IterableInAppMessage> messageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler).onNewInApp(messageCaptor.capture());
        assertEquals("immediate", messageCaptor.getValue().getCustomPayload().getString("key"));
        assertEquals("message1", messageCaptor.getValue().getMessageId());

        // Verify never trigger message was not processed
        verify(inAppHandler, never()).onNewInApp(argThat(msg ->
            msg.getMessageId().equals("message2")));
    }

    @Test
    public void testJsonOnlyInAppMessageWithoutCustomPayload() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("messageType", "Mobile")
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message1")
                                .put("campaignId", 1)));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        List<IterableInAppMessage> messages = inAppManager.getMessages();
        assertEquals(1, messages.size());

        // In Android, when customPayload is not provided, an empty JSONObject is created
        JSONObject customPayload = messages.get(0).getCustomPayload();
        assertNotNull("Custom payload should not be null", customPayload);
        assertEquals("Custom payload should be empty when not provided", 0, customPayload.length());
    }

    @Test
    public void testJsonOnlyMessageWithEmptyPayload() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject())
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message1")));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        List<IterableInAppMessage> messages = inAppManager.getMessages();
        assertEquals(1, messages.size());
        assertTrue("Custom payload should be empty", messages.get(0).getCustomPayload().length() == 0);
    }

    @Test
    public void testJsonOnlyMessageCannotBeSavedToInbox() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", true)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject().put("key", "value"))
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message1")
                                .put("inboxMetadata", new JSONObject()
                                        .put("title", "JSON Message")
                                        .put("subtitle", "Test Subtitle")
                                        .put("icon", "test-icon.png"))));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        List<IterableInAppMessage> inboxMessages = inAppManager.getInboxMessages();
        assertEquals(0, inboxMessages.size());
    }

    @Test
    public void testJsonOnlyMessageIgnoresContentPayload() throws Exception {
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject()
                                        .put("key", "customValue"))
                                .put("content", new JSONObject()
                                        .put("payload", new JSONObject()
                                                .put("key", "contentValue")))
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message1")));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        List<IterableInAppMessage> messages = inAppManager.getMessages();
        assertEquals(1, messages.size());
        assertEquals("customValue", messages.get(0).getCustomPayload().getString("key"));
    }

    @Test
    public void testJsonOnlyInAppMessageProcessingAndDisplay() throws Exception {
        // Create payload similar to iOS test
        JSONObject payload = new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject()
                                        .put("key", "value"))
                                .put("content", new JSONObject()
                                        .put("html", "<meta name=\"viewport\" content=\"width=device-width\">")
                                        .put("inAppDisplaySettings", new JSONObject()
                                                .put("left", new JSONObject().put("percentage", 0))
                                                .put("top", new JSONObject().put("percentage", 0))
                                                .put("right", new JSONObject().put("percentage", 0))
                                                .put("bottom", new JSONObject().put("percentage", 0))))
                                .put("trigger", new JSONObject().put("type", "immediate"))
                                .put("messageId", "message1")
                                .put("campaignId", 1)));

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        // Create InAppManager with mock handler and displayer
        IterableInAppDisplayer mockDisplayer = mock(IterableInAppDisplayer.class);
        final IterableInAppHandler inAppHandler = mock(IterableInAppHandler.class);

        IterableInAppManager inAppManager = spy(new IterableInAppManager(
                IterableApi.sharedInstance,
                inAppHandler,
                30.0,
                new IterableInAppMemoryStorage(),
                IterableActivityMonitor.getInstance(),
                mockDisplayer));
        IterableApi.sharedInstance = new IterableApi(inAppManager);

        // First sync to get messages
        inAppManager.syncInApp();
        shadowOf(getMainLooper()).idle();

        // Process messages by bringing app to foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify handler was called with correct message
        ArgumentCaptor<IterableInAppMessage> messageCaptor = ArgumentCaptor.forClass(IterableInAppMessage.class);
        verify(inAppHandler).onNewInApp(messageCaptor.capture());
        assertEquals("value", messageCaptor.getValue().getCustomPayload().getString("key"));

        // Verify displayer was never called
        verify(mockDisplayer, never()).showMessage(
            any(IterableInAppMessage.class),
            any(IterableInAppLocation.class),
            any(IterableHelper.IterableUrlCallback.class));

        // Verify message was consumed (not in queue)
        assertEquals(0, inAppManager.getMessages().size());
    }

    private String createJsonOnlyPayload() throws JSONException {
        return new JSONObject()
                .put("inAppMessages", new JSONArray()
                        .put(new JSONObject()  // Immediate trigger
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject().put("key", "immediate"))
                                .put("trigger", new JSONObject().put("type", "immediate"))
                                .put("messageId", "message1"))
                        .put(new JSONObject()  // Never trigger
                                .put("saveToInbox", false)
                                .put("jsonOnly", 1)
                                .put("customPayload", new JSONObject().put("key", "never"))
                                .put("trigger", new JSONObject().put("type", "never"))
                                .put("messageId", "message2")))
                .toString();
    }

    private static class IterableSkipInAppHandler implements IterableInAppHandler {
        @NonNull
        @Override
        public InAppResponse onNewInApp(@NonNull IterableInAppMessage message) {
            return InAppResponse.SKIP;
        }
    }
}
