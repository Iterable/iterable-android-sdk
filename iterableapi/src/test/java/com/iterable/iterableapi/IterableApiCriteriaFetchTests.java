package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IterableApiCriteriaFetchTests extends BaseTest {
    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;

    @Before
    public void setUp() {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);

        reInitIterableApi();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonActivation(true).build();
        IterableApi.initialize(getContext(), "apiKey", iterableConfig);
        IterableApi.getInstance().setVisitorUsageTracked(true);
    }

    private void reInitIterableApi() {
        IterableApi.sharedInstance = new IterableApi();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
        IterableApi.getInstance().setUserId(null);
        IterableApi.getInstance().setEmail(null);

        // Add these cleanup steps
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        // Clear any pending handlers
        shadowOf(getMainLooper()).idle();
    }

    private void addResponse(String endPoint) {
        dispatcher.enqueueResponse("/" + endPoint, new MockResponse().setResponseCode(200).setBody("{}"));
    }

    @Test
    public void testForegroundCriteriaFetchWhenConditionsMet() throws Exception {
        // Clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // Mock responses for expected endpoints
        addResponse(IterableConstants.ENDPOINT_CRITERIA_LIST);

        // Initialize with anon activation and foreground fetch enabled
        IterableConfig config = new IterableConfig.Builder()
            .setEnableAnonActivation(true)
            .setForegroundCriteriaFetch(true)
            .build();

        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        // Initialize API
        IterableApi.initialize(getContext(), "apiKey", config);
        IterableApi.getInstance().setVisitorUsageTracked(true);

        // Verify first criteria fetch when consent is given
        RecordedRequest firstCriteriaRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("First criteria request should be made", firstCriteriaRequest);
        assertTrue("First request URL should contain getCriteria endpoint",
            firstCriteriaRequest.getPath().contains(IterableConstants.ENDPOINT_CRITERIA_LIST));

        // Simulate app coming to foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify criteria fetch request was made
        RecordedRequest secondCriteriaRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Criteria request should be made on foreground", secondCriteriaRequest);
        assertTrue("Request URL should contain getCriteria endpoint",
            secondCriteriaRequest.getPath().contains(IterableConstants.ENDPOINT_CRITERIA_LIST));

        // Clean up
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

    @Test
    public void testCriteriaFetchNotCalledWhenDisabled() throws Exception {
        // Clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // Mock ALL responses in sequence
        addResponse("anonymoususer/list");  // For initial request - 200 OK
        addResponse("anonymoususer/list");  // For second request - 200 OK
        addResponse("mobile/getRemoteConfiguration");  // For foreground - 200 OK

        // Initialize with foreground fetch disabled
        IterableConfig config = new IterableConfig.Builder()
            .setEnableAnonActivation(true)
            .setForegroundCriteriaFetch(false)
            .build();

        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        // Initialize API and set visitor tracking
        IterableApi.initialize(getContext(), "apiKey", config);
        IterableApi.getInstance().setVisitorUsageTracked(true);

        // Take first two anonymous user list requests
        RecordedRequest firstRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Should have first request", firstRequest);
        assertTrue(firstRequest.getPath().contains("/anonymoususer/list"));

        RecordedRequest secondRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Should have second request", secondRequest);
        assertTrue(secondRequest.getPath().contains("/anonymoususer/list"));

        // Simulate app coming to foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Should only get remote config request after foreground
        RecordedRequest configRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Should have remote config request", configRequest);
        assertTrue("Should be a remote configuration request",
            configRequest.getPath().contains("/mobile/getRemoteConfiguration"));

        // No more requests
        RecordedRequest extraRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNull("Should not have any additional requests", extraRequest);

        // Clean up
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

    @Test
    public void testForegroundCriteriaFetchWithCooldown() throws Exception {
        // Clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // Mock responses
        addResponse(IterableConstants.ENDPOINT_CRITERIA_LIST);
        addResponse(IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION);

        // Initialize with required config
        IterableConfig config = new IterableConfig.Builder()
            .setEnableAnonActivation(true)
            .setForegroundCriteriaFetch(true)
            .build();

        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        // Initialize API
        IterableApi.initialize(getContext(), "apiKey", config);
        IterableApi.getInstance().setVisitorUsageTracked(true);

        // Verify first criteria fetch when consent is given
        RecordedRequest firstCriteriaRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("First criteria request should be made", firstCriteriaRequest);
        assertTrue("First request URL should contain getCriteria endpoint",
            firstCriteriaRequest.getPath().contains(IterableConstants.ENDPOINT_CRITERIA_LIST));

        // First foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify second criteria fetch
        RecordedRequest secondCriteriaRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Second criteria request should be made", firstCriteriaRequest);
        assertTrue("Second request URL should contain getCriteria endpoint",
            secondCriteriaRequest.getPath().contains(IterableConstants.ENDPOINT_CRITERIA_LIST));

        // Immediate second foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Verify no criteria requests during cooldown period
        RecordedRequest cooldownRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertFalse("Second request URL should contain getCriteria endpoint",
            cooldownRequest.getPath().contains(IterableConstants.ENDPOINT_CRITERIA_LIST));

        // Clean up
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }
}
