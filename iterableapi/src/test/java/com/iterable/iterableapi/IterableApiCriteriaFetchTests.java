package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableUnknownUserActivation(true).build();
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

        // Initialize with unknown user activation and foreground fetch enabled
        IterableConfig config = new IterableConfig.Builder()
            .setEnableUnknownUserActivation(true)
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
            .setEnableUnknownUserActivation(true)
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
            .setEnableUnknownUserActivation(true)
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

    @Test
    public void testCriteriaCallbackOnSuccess() throws Exception {
        // Flush the criteria fetch triggered by setUp() while no callback is installed,
        // then reset tracking so the only callback-observed fetch is the one below.
        shadowOf(getMainLooper()).idle();
        IterableApi.getInstance().setVisitorUsageTracked(false);

        String criteriaJson = "{\"criteriaSets\":[{\"criteriaId\":\"1\"}]}";
        dispatcher.enqueueResponse("/" + IterableConstants.ENDPOINT_CRITERIA_LIST,
            new MockResponse().setResponseCode(200).setBody(criteriaJson));

        AtomicReference<JSONObject> received = new AtomicReference<>(null);
        AtomicBoolean failed = new AtomicBoolean(false);
        IterableConfig config = new IterableConfig.Builder()
            .setEnableUnknownUserActivation(true)
            .setUnknownUserHandler(new IterableUnknownUserHandler() {
                @Override
                public void onUnknownUserCreated(String userId) {}
                @Override
                public void onCriteriaReceived(JSONObject criteria) {
                    received.set(criteria);
                }
                @Override
                public void onCriteriaFetchFailed(String reason) {
                    failed.set(true);
                }
            })
            .build();

        IterableApi.initialize(getContext(), "apiKey", config);
        IterableApi.getInstance().setVisitorUsageTracked(true);
        shadowOf(getMainLooper()).idle();

        Assert.assertNotNull("onCriteriaReceived should be called with criteria", received.get());
        assertTrue("Criteria should contain the fetched data", received.get().has("criteriaSets"));
        assertFalse("onCriteriaFetchFailed should not be called on a successful fetch", failed.get());
    }

    @Test
    public void testCriteriaCallbackOnFailure() throws Exception {
        shadowOf(getMainLooper()).idle();
        IterableApi.getInstance().setVisitorUsageTracked(false);

        dispatcher.enqueueResponse("/" + IterableConstants.ENDPOINT_CRITERIA_LIST,
            new MockResponse().setResponseCode(400).setBody("{}"));

        AtomicBoolean succeeded = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> failureReason = new AtomicReference<>(null);
        IterableConfig config = new IterableConfig.Builder()
            .setEnableUnknownUserActivation(true)
            .setUnknownUserHandler(new IterableUnknownUserHandler() {
                @Override
                public void onUnknownUserCreated(String userId) {}
                @Override
                public void onCriteriaReceived(JSONObject criteria) {
                    succeeded.set(true);
                }
                @Override
                public void onCriteriaFetchFailed(String reason) {
                    failed.set(true);
                    failureReason.set(reason);
                }
            })
            .build();

        IterableApi.initialize(getContext(), "apiKey", config);
        IterableApi.getInstance().setVisitorUsageTracked(true);
        shadowOf(getMainLooper()).idle();

        assertTrue("onCriteriaFetchFailed should be called when the criteria fetch fails", failed.get());
        assertFalse("onCriteriaReceived should not be called on a failed fetch", succeeded.get());
        assertNotNull("onCriteriaFetchFailed should receive a failure reason", failureReason.get());
        assertFalse("Failure reason should not be empty", failureReason.get().isEmpty());
    }

    @Test
    public void testCriteriaCallbackFiresOnEveryFetch() throws Exception {
        // Flush the criteria fetch triggered by setUp() while no callback is installed,
        // then reset tracking so only the callback-observed fetches below are counted.
        shadowOf(getMainLooper()).idle();
        IterableApi.getInstance().setVisitorUsageTracked(false);

        String criteriaJson = "{\"criteriaSets\":[{\"criteriaId\":\"1\"}]}";
        // One response per fetch.
        dispatcher.enqueueResponse("/" + IterableConstants.ENDPOINT_CRITERIA_LIST,
            new MockResponse().setResponseCode(200).setBody(criteriaJson));
        dispatcher.enqueueResponse("/" + IterableConstants.ENDPOINT_CRITERIA_LIST,
            new MockResponse().setResponseCode(200).setBody(criteriaJson));

        AtomicInteger receivedCount = new AtomicInteger(0);
        IterableConfig config = new IterableConfig.Builder()
            .setEnableUnknownUserActivation(true)
            .setUnknownUserHandler(new IterableUnknownUserHandler() {
                @Override
                public void onUnknownUserCreated(String userId) {}
                @Override
                public void onCriteriaReceived(JSONObject criteria) {
                    receivedCount.incrementAndGet();
                }
            })
            .build();

        IterableApi.initialize(getContext(), "apiKey", config);

        // Each enable of visitor usage tracking triggers a criteria fetch; the callback
        // should fire once per fetch, matching the documented "fires on every fetch" contract.
        IterableApi.getInstance().setVisitorUsageTracked(true);
        shadowOf(getMainLooper()).idle();
        IterableApi.getInstance().setVisitorUsageTracked(true);
        shadowOf(getMainLooper()).idle();

        Assert.assertEquals("onCriteriaReceived should fire once per fetch", 2, receivedCount.get());
    }
}
