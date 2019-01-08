package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;

import com.iterable.iterableapi.unit.BaseTest;
import com.iterable.iterableapi.unit.IterableTestUtils;
import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@PrepareForTest(IterableUtil.class)
public class IterableInAppManagerTest extends BaseTest {

    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        inAppHandler = mock(IterableInAppHandler.class);
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableApi.sharedInstance = new IterableApi();
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setInAppHandler(inAppHandler);
            }
        });

        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
    }

    @After
    public void tearDown() throws IOException {
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

}
