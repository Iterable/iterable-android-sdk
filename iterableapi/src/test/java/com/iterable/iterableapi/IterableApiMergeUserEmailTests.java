package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IterableApiMergeUserEmailTests extends BaseTest {
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
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
        IterableApi.initialize(getContext(), "apiKey", iterableConfig);
        setCriteria(criteriaMockData);

    }


    private void reInitIterableApi() {
        IterableApi.sharedInstance = new IterableApi();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
        clearEventData();
        IterableApi.getInstance().setUserId(null);
        IterableApi.getInstance().setEmail(null);
    }

    private String getEventData() {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
    }
    private void clearEventData() {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
        editor.apply();
    }
    private void setCriteria(String criteria) {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_CRITERIA, criteria);
        editor.apply();
    }
    private void triggerTrackPurchaseEvent(String id, String name, double price, int quantity) throws JSONException {
        List<CommerceItem> items = new ArrayList<>();
        items.add(new CommerceItem(id, name, price, quantity));
        IterableApi.getInstance().trackPurchase(4, items);
    }
    private void addResponse(String endPoint) {
        dispatcher.enqueueResponse("/" + endPoint, new MockResponse().setResponseCode(200).setBody("{}"));
    }

    // all userId tests
    @Test
    public void testCriteriaNotMetUserIdMergeFalse() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();
        String eventData = getEventData();
        assertNotEquals("", eventData);
        final String userId = "testUser2";
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        IterableApi.getInstance().setUserId(userId, false);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals(eventData, getEventData());
    }

    @Test
    public void testCriteriaNotMetUserIdMergeTrue() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();
        String eventData = getEventData();
        assertNotEquals("", eventData);
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId, true);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals("", getEventData());
    }

    @Test
    public void testCriteriaNotMetUserIdDefault() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();
        String eventData = getEventData();
        assertNotEquals("", eventData);
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals("", getEventData());
    }

    @Test
    public void testCriteriaMetUserIdMergeFalse() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId, false);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaMetUserIdMergeTrue() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId, true);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaMetUserIdDefault() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testIdentifiedUserIdMergeFalse() throws Exception {

        final String userId1 = "testUser1";
        IterableApi.getInstance().setUserId(userId1, false);
        shadowOf(getMainLooper()).idle();
        assertEquals(userId1, IterableApi.getInstance().getUserId());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId2 = "testUser2";
        IterableApi.getInstance().setUserId(userId2, false);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(userId2, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testIdentifiedUserIdMergeTrue() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        final String userId1 = "testUser1";
        IterableApi.getInstance().setUserId(userId1, false);
        shadowOf(getMainLooper()).idle();
        assertEquals(userId1, IterableApi.getInstance().getUserId());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        final String userId2 = "testUser2";
        IterableApi.getInstance().setUserId(userId2, true);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertEquals(userId2, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testIdentifiedUserIdDefault() throws Exception {
        final String userId1 = "testUser1";
        IterableApi.getInstance().setUserId(userId1);
        shadowOf(getMainLooper()).idle();
        assertEquals(userId1, IterableApi.getInstance().getUserId());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String userId2 = "testUser2";
        IterableApi.getInstance().setUserId(userId2);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(userId2, IterableApi.getInstance().getUserId());
    }

    // all email tests
    @Test
    public void testCriteriaNotMetEmailMergeFalse() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();
        String eventData = getEventData();
        assertNotEquals("", eventData);
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email = "testUser@gmail.com";
        IterableApi.getInstance().setEmail(email, false);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(email, IterableApi.getInstance().getEmail());
        assertEquals(eventData, getEventData());
    }

    @Test
    public void testCriteriaNotMetEmailMergeTrue() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();
        String eventData = getEventData();
        assertNotEquals("", eventData);
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email = "testUser@gmail.com";
        IterableApi.getInstance().setEmail(email, true);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(email, IterableApi.getInstance().getEmail());
        assertEquals("", getEventData());
    }

    @Test
    public void testCriteriaNotMetEmailDefault() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();
        String eventData = getEventData();
        assertNotEquals("", eventData);

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(email, IterableApi.getInstance().getEmail());
        assertEquals("", getEventData());
    }

    @Test
    public void testCriteriaMetEmailMergeFalse() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email = "testUser@gmail.com";
        IterableApi.getInstance().setEmail(email, false);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaMetEmailMergeTrue() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email = "testUser@gmail.com";
        IterableApi.getInstance().setEmail(email, true);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaMetEmailDefault() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testIdentifiedEmailMergeFalse() throws Exception {

        final String email1 = "testUser1@gmail.com";
        IterableApi.getInstance().setEmail(email1, false);
        shadowOf(getMainLooper()).idle();
        assertEquals(email1, IterableApi.getInstance().getEmail());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email2 = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email2, false);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(email2, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testIdentifiedEmailMergeTrue() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        final String email1 = "testUser1@gmail.com";
        IterableApi.getInstance().setEmail(email1, false);
        shadowOf(getMainLooper()).idle();
        assertEquals(email1, IterableApi.getInstance().getEmail());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        final String email2 = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email2, true);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertEquals(email2, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testIdentifiedEmailDefault() throws Exception {
        final String email1 = "testUser1@gmail.com";
        IterableApi.getInstance().setEmail(email1);
        shadowOf(getMainLooper()).idle();
        assertEquals(email1, IterableApi.getInstance().getEmail());
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        final String email2 = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email2);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertEquals(email2, IterableApi.getInstance().getEmail());
    }
}
