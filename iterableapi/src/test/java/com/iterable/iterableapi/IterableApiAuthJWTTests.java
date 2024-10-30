package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static com.iterable.iterableapi.IterableConstants.HEADER_SDK_AUTH_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

public class IterableApiAuthJWTTests extends BaseTest {

    private MockWebServer server;
    private IterableAuthHandler authHandler;
    private PathBasedQueueDispatcher dispatcher;

    private final String validJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyAiZW1haWwiOiAidGVzdEBleGFtcGxlLmNvbSIsICJpYXQiOiAxNzI5MjUyNDE3LCAiZXhwIjogMTcyOTg1NzIxNyB9.m-O6ksCv9OR-cF0RdiHB8VW_NwWJHVXChipbcFmIChg";
    private final String newJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE5MTYyMzkwMjJ9.dMD3MLuHTiO-Qy9PvOoMchNM4CzFIgI7jKVrRtlqlM0";

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
    public void setUp() throws IOException {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        reInitIterableApi();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    private void reInitIterableApi() {
        IterableApi.sharedInstance = new IterableApi();
        authHandler = mock(IterableAuthHandler.class);
        IterableTestUtils.createIterableApiNew(builder -> builder.setAuthHandler(authHandler), null);
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).setAuthHandler(authHandler).build();
        IterableApi.initialize(getContext(), "fake_key", iterableConfig);
        IterableApi.getInstance().setVisitorUsageTracked(true);
        setCriteria(criteriaMockData);
    }

    private void setCriteria(String criteria) {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_CRITERIA, criteria);
        editor.apply();
    }

    private void addResponse(String endPoint) {
        dispatcher.enqueueResponse("/" + endPoint, new MockResponse().setResponseCode(200).setBody("{}"));
    }

    private String getEventData() {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
    }

    private void triggerTrackPurchaseEvent(String id, String name, double price, int quantity) throws JSONException {
        List<CommerceItem> items = new ArrayList<>();
        items.add(new CommerceItem(id, name, price, quantity));
        IterableApi.getInstance().trackPurchase(4, items);
    }

    @Test
    public void testCriteriaUserIdTokenCheckPass() throws Exception {
        String userId = "testUserId";
        IterableApi.getInstance().setUserId(userId);
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) {
        }
        final String userId1 = "testUser1";

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId1);
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals(userId1, IterableApi.getInstance().getUserId());
        assertEquals(HEADER_SDK_AUTH_FORMAT + validJWT, recordedRequest.getHeader("Authorization"));

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) {
        }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) {
        }
        final String userId2 = "testUser2";

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId2);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals(userId2, IterableApi.getInstance().getUserId());
        assertEquals(HEADER_SDK_AUTH_FORMAT + newJWT, mergeRequest.getHeader("Authorization"));
    }

    @Test
    public void testCriteriaEmailIdTokenCheckPass() throws Exception {
        String emailId = "testUserId@example.com";
        IterableApi.getInstance().setEmail(emailId);
        assertEquals(emailId, IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getAuthToken());

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) {
        }
        final String emailId1 = "testUser1@example.com";

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(emailId1);
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals(emailId1, IterableApi.getInstance().getEmail());
        assertEquals(HEADER_SDK_AUTH_FORMAT + validJWT, recordedRequest.getHeader("Authorization"));

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) {
        }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();
        assertEquals("", getEventData());

        while (server.takeRequest(1, TimeUnit.SECONDS) != null) {
        }
        final String emailId2 = "testUser2@example.com";

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(emailId2);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        assertEquals(emailId2, IterableApi.getInstance().getEmail());
        assertEquals(HEADER_SDK_AUTH_FORMAT + newJWT, mergeRequest.getHeader("Authorization"));
    }
}
