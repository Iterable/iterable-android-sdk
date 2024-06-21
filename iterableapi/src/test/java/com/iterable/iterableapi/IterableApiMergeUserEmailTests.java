package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IterableApiMergeUserEmailTests extends BaseTest {
    private MockWebServer server;
    private String criteriaMockData = "{\n" +
        "   \"count\":2,\n" +
                "   \"criterias\":[\n" +
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
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        setCriteria(criteriaMockData);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    private String getEventData() {
        SharedPreferences sharedPref = getContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
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

//    @Test
//    public void testCriteriaNotMetUserIdMergeFalse() throws Exception {
//
//        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
//        IterableApi.initialize(getContext(), "apiKey", iterableConfig);
//        IterableApi.getInstance().getKeychain().saveUserIdAnon("testAnonId");
//        assertNotNull(IterableApi.getInstance().getKeychain().getUserIdAnon());
//
//        String userId = "testUserId";
//        IterableApi.getInstance().setUserId(userId, true);
//
//        assertTrue(IterableApi.getInstance().getKeychain().getUserIdAnon() == null);
////        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
////        assertNotNull(recordedRequest);
//        System.out.println("TEST_LOG: " + recordedRequest.getPath());
//        System.out.println("TEST_LOG: " + String.valueOf(IterableApi.getInstance().getKeychain().getUserIdAnon()));
//
//    }

    @Test
    public void testCriteriaNotMetUserIdMergeFalse() throws Exception {
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
        IterableApi.initialize(getContext(), "18845050c4774b7c9dc48beece2f6d8b", iterableConfig);
//        IterableApi.getInstance().setEmail(null);
//        IterableApi.getInstance().setUserId(null);
        while (server.takeRequest(1, TimeUnit.SECONDS) != null);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        RecordedRequest recordedRequest = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        System.out.println("TEST_LOG: " + recordedRequest.getPath());
        String eventData = getEventData();
        String userId = "testUserId";
        IterableApi.getInstance().setUserId(userId, false);
        assertEquals(eventData, getEventData());
        assertEquals(userId, IterableApi.getInstance().getKeychain().getUserId());
    }

    @Test
    public void testCriteriaNotMetUserIdMergeTrue() throws Exception {
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
        IterableApi.initialize(getContext(), "apiKey", iterableConfig);
        IterableApi.getInstance().getKeychain().saveUserId(null);
        IterableApi.getInstance().getKeychain().saveUserIdAnon(null);
        IterableApi.getInstance().getKeychain().saveEmail(null);
//        triggerUpdateCartEvent("test", "piano", 15, 2);

        String userId = "testUserId";
        IterableApi.getInstance().setUserId(userId, true);

        assertEquals(IterableApi.getInstance().getKeychain().getUserIdAnon(), null);
        assertEquals(IterableApi.getInstance().getKeychain().getEmail(), null);
        assertEquals("", getEventData());
        assertEquals(userId, IterableApi.getInstance().getKeychain().getUserId());
    }

    @Test
    public void testCriteriaMetUserIdMergeFalse() throws Exception {
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
        IterableApi.initialize(getContext(), "apiKey", iterableConfig);
        IterableApi.getInstance().getKeychain().saveUserId(null);
        IterableApi.getInstance().getKeychain().saveEmail(null);
        IterableApi.getInstance().getKeychain().saveUserIdAnon("testAnonId");

        String userId = "testUserId";
        IterableApi.getInstance().setUserId(userId, false);

        assertEquals(IterableApi.getInstance().getKeychain().getUserIdAnon(),null);
        assertEquals(userId, IterableApi.getInstance().getKeychain().getUserId());
    }

}
