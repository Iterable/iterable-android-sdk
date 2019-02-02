package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

@PrepareForTest(IterableUtil.class)
public class IterableInAppManagerTest extends BasePowerMockTest {

    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableUtil.IterableUtilImpl originalIterableUtil;
    private IterableUtil.IterableUtilImpl iterableUtilSpy;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);

        inAppHandler = mock(IterableInAppHandler.class);
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableApi.sharedInstance = new IterableApi();
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setInAppHandler(inAppHandler);
            }
        });

        originalIterableUtil = IterableUtil.instance;
        iterableUtilSpy = spy(originalIterableUtil);
        IterableUtil.instance = iterableUtilSpy;
    }

    @After
    public void tearDown() throws IOException {
        IterableUtil.instance = originalIterableUtil;
        iterableUtilSpy = null;

        server.shutdown();
        server = null;
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(RuntimeEnvironment.application);
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

    @Test
    public void testDoNotShowMultipleTimes() throws Exception {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start().resume();
        Activity activity = controller.get();

        boolean shownFirstTime = IterableInAppManager.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());
        boolean shownSecondTime = IterableInAppManager.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());
        assertTrue(shownFirstTime);
        assertFalse(shownSecondTime);
        ShadowDialog.getLatestDialog().dismiss();
        controller.pause().stop().destroy();
    }

    @Test
    public void testSyncInApp() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        assertEquals(0, inAppManager.getMessages().size());

        inAppManager.syncInApp();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        assertEquals(1, inAppManager.getMessages().size());
        assertEquals("7kx2MmoGdCpuZao9fDueuQoXVAZuDaVV", inAppManager.getMessages().get(0).getMessageId());

        // Check that we remove the old message if it doesn't exist in the new queue state
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single2.json")));
        inAppManager.syncInApp();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        assertEquals(1, inAppManager.getMessages().size());
        assertEquals("Q19mD2NlQUnxnmSGuQu9ujzkKR6c12TogeaGA29", inAppManager.getMessages().get(0).getMessageId());
    }

    @Test
    public void testProcessAfterForeground() throws Exception {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_single.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        assertEquals(0, inAppManager.getMessages().size());

        inAppManager.syncInApp();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        assertEquals(1, inAppManager.getMessages().size());

        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).create().start().resume();
        Robolectric.flushForegroundThreadScheduler();

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
        assertEquals(1, inAppManager.getMessages().size());

        doReturn(System.currentTimeMillis() + 120 * 1000).when(iterableUtilSpy).currentTimeMillis();
        assertEquals(0, inAppManager.getMessages().size());
    }

}
