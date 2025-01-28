package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.io.IOException;
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
            .setEnableForegroundCriteriaFetch(true)
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
        // Initialize with foreground fetch disabled
        IterableConfig config = new IterableConfig.Builder()
            .setEnableAnonActivation(true)
            .setEnableForegroundCriteriaFetch(false)
            .build();

        // Initialize API and set visitor tracking
        IterableApi.initialize(getContext(), "apiKey", config);
        IterableApi.getInstance().setVisitorUsageTracked(true);

        // Clear out any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // Simulate app coming to foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Should only get remote config request after foreground
        RecordedRequest configRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Should have remote config request", configRequest);
        assertTrue("Should be a remote configuration request",
            configRequest.getPath().contains(IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION));

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
            .setEnableForegroundCriteriaFetch(true)
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
