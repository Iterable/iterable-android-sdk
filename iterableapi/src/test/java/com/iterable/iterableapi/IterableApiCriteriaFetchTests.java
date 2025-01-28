package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

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

    private final String criteriaMockData = "{\n" +
        "   \"count\":2,\n" +
        "   \"criteriaSets\":[\n" +
        "      {\n" +
        "         \"criteriaId\":43,\n" +
        "         \"searchQuery\":{\n" +
        "            \"combinator\":\"Or\",\n" +
        "            \"searchQueries\":[\n" +
        "               {\n" +
        "                  \"combinator\":\"And\",\n" +
        "                  \"searchQueries\":[\n" +
        "                     {\n" +
        "                        \"dataType\":\"purchase\",\n" +
        "                        \"searchCombo\":{\n" +
        "                           \"combinator\":\"Or\",\n" +
        "                           \"searchQueries\":[\n" +
        "                              {\n" +
        "                                 \"field\":\"shoppingCartItems.price\",\n" +
        "                                 \"fieldType\":\"double\",\n" +
        "                                 \"comparatorType\":\"Equals\",\n" +
        "                                 \"dataType\":\"purchase\",\n" +
        "                                 \"id\":2,\n" +
        "                                 \"value\":\"4.67\"\n" +
        "                              },\n" +
        "                              {\n" +
        "                                 \"field\":\"shoppingCartItems.quantity\",\n" +
        "                                 \"fieldType\":\"long\",\n" +
        "                                 \"comparatorType\":\"GreaterThanOrEqualTo\",\n" +
        "                                 \"dataType\":\"purchase\",\n" +
        "                                 \"id\":3,\n" +
        "                                 \"valueLong\":2,\n" +
        "                                 \"value\":\"2\"\n" +
        "                              }\n" +
        "                           ]\n" +
        "                        }\n" +
        "                     }\n" +
        "                  ]\n" +
        "               }\n" +
        "            ]\n" +
        "         }\n" +
        "      },\n" +
        "      {\n" +
        "         \"criteriaId\":5678,\n" +
        "         \"searchQuery\":{\n" +
        "            \"combinator\":\"Or\",\n" +
        "            \"searchQueries\":[\n" +
        "               {\n" +
        "                  \"combinator\":\"Or\",\n" +
        "                  \"searchQueries\":[\n" +
        "                     {\n" +
        "                        \"dataType\":\"user\",\n" +
        "                        \"searchCombo\":{\n" +
        "                           \"combinator\":\"And\",\n" +
        "                           \"searchQueries\":[\n" +
        "                              {\n" +
        "                                 \"field\":\"itblInternal.emailDomain\",\n" +
        "                                 \"fieldType\":\"string\",\n" +
        "                                 \"comparatorType\":\"Equals\",\n" +
        "                                 \"dataType\":\"user\",\n" +
        "                                 \"id\":6,\n" +
        "                                 \"value\":\"gmail.com\"\n" +
        "                              }\n" +
        "                           ]\n" +
        "                        }\n" +
        "                     },\n" +
        "                     {\n" +
        "                        \"dataType\":\"customEvent\",\n" +
        "                        \"searchCombo\":{\n" +
        "                           \"combinator\":\"And\",\n" +
        "                           \"searchQueries\":[\n" +
        "                              {\n" +
        "                                 \"field\":\"eventName\",\n" +
        "                                 \"fieldType\":\"string\",\n" +
        "                                 \"comparatorType\":\"Equals\",\n" +
        "                                 \"dataType\":\"customEvent\",\n" +
        "                                 \"id\":9,\n" +
        "                                 \"value\":\"processing_cancelled\"\n" +
        "                              },\n" +
        "                              {\n" +
        "                                 \"field\":\"createdAt\",\n" +
        "                                 \"fieldType\":\"date\",\n" +
        "                                 \"comparatorType\":\"GreaterThan\",\n" +
        "                                 \"dataType\":\"customEvent\",\n" +
        "                                 \"id\":10,\n" +
        "                                 \"dateRange\":{\n" +
        "                                    \n" +
        "                                 },\n" +
        "                                 \"isRelativeDate\":false,\n" +
        "                                 \"value\":\"1688194800000\"\n" +
        "                              }\n" +
        "                           ]\n" +
        "                        }\n" +
        "                     }\n" +
        "                  ]\n" +
        "               }\n" +
        "            ]\n" +
        "         }\n" +
        "      }\n" +
        "   ]\n" +
        "}";

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
        setCriteria(criteriaMockData);
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

    private void setCriteria(String criteria) {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_CRITERIA, criteria);
        editor.apply();
    }

    private void addResponse(String endPoint) {
        MockResponse response = new MockResponse().setResponseCode(200);

        // If it's an anonymous user list request, return the criteria mock data
        if (endPoint.contains("anonymoususer/list")) {
            response.setBody(criteriaMockData);
        } else {
            response.setBody("{}");
        }

        dispatcher.enqueueResponse("/" + endPoint, response);
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

        // Set up dispatcher
        dispatcher = new PathBasedQueueDispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("anonymoususer/list")) {
                    return new MockResponse()
                        .setResponseCode(200)
                        .setBody(criteriaMockData);
                }
                return super.dispatch(request);
            }
        };
        server.setDispatcher(dispatcher);

        // Mock remote config
        addResponse("mobile/getRemoteConfiguration");

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

        // Take initial requests (should be two criteria fetches)
        RecordedRequest firstRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest secondRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Should have first criteria request", firstRequest);
        Assert.assertNotNull("Should have second criteria request", secondRequest);
        assertTrue(firstRequest.getPath().contains("anonymoususer/list"));
        assertTrue(secondRequest.getPath().contains("anonymoususer/list"));

        // Simulate app coming to foreground
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();

        // Should only get remote config request after foreground
        RecordedRequest configRequest = server.takeRequest(1, TimeUnit.SECONDS);
        Assert.assertNotNull("Should have remote config request", configRequest);
        assertTrue("Should be a remote configuration request",
            configRequest.getPath().contains("mobile/getRemoteConfiguration"));

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
