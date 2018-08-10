package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;

import com.iterable.iterableapi.unit.BaseTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;

import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@PrepareForTest(IterableUtil.class)
public class IterableInAppManagerTest extends BaseTest {

    private MockWebServer server;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableApi.sharedInstance = new IterableApi();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    @Test
    public void testDoNotShowMultipleTimes() throws Exception {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        boolean shownFirstTime = IterableInAppManager.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());
        boolean shownSecondTime = IterableInAppManager.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());
        assertTrue(shownFirstTime);
        assertFalse(shownSecondTime);
    }

}
