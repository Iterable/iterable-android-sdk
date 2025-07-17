package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONException;
import org.json.JSONObject;
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
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableUnknownUserActivation(true).build();
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
    public void testCriteriaNotMetUserIdDefault() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId);

        // check that request was sent to purchase endpoint on event replay
        RecordedRequest purchaseRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(purchaseRequest2);
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE), purchaseRequest2.getPath());

        // check that request was not sent to merge endpoint and was sent to the consent tracking endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(consentRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), consentRequest.getPath());
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), consentRequest.getPath());

        // verify track consent request body contains proper user ID and isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());
        assertEquals(userId, consentRequestJson.getString(IterableConstants.KEY_USER_ID));
        assertTrue(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaNotMetUserIdReplayTrueMergeFalse() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setUserId(userId, identityResolution);

        // check that request was sent to purchase endpoint on event replay
        RecordedRequest purchaseRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(purchaseRequest2);
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE), purchaseRequest2.getPath());

        // check that request was not sent to merge endpoint and was sent to the consent tracking endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(consentRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), consentRequest.getPath());
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), consentRequest.getPath());

        // verify track consent request body contains proper user ID and isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());
        assertEquals(userId, consentRequestJson.getString(IterableConstants.KEY_USER_ID));
        assertTrue(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaNotMetUserIdReplayFalseMergeFalse() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(false, false);
        IterableApi.getInstance().setUserId(userId, identityResolution);

        // check that request was not sent to merge endpoint or track consent endpoint
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), request.getPath());
        assertNotEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), request.getPath());

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaNotMetUserIdReplayFalseMergeTrue() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(false, true);
        IterableApi.getInstance().setUserId(userId, identityResolution);

        // check that request was not sent to merge endpoint or track consent endpoint
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), request.getPath());
        assertNotEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), request.getPath());

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaMetUserIdDefault() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if request was sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", unknownSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, unknownSessionRequest.getPath());

        // check if request was sent to track purchase endpoint
        RecordedRequest purchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", purchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, purchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId);

        // check that request was sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());

        // check that a second consent tracking request was not sent
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, request.getPath());

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaMetUserIdMergeFalse() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if request was sent to unknown user session endpoint
        RecordedRequest                                                                                                                                                                              unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", unknownSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, unknownSessionRequest.getPath());

        // check if request was sent to track purchase endpoint
        RecordedRequest purchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", purchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, purchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setUserId(userId, identityResolution);

        // check that request was not sent to merge endpoint or consent tracking endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());
        assertNotEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, mergeRequest.getPath());

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testCriteriaMetUserIdMergeTrue() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if request was sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", unknownSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, unknownSessionRequest.getPath());

        // check if request was sent to track purchase endpoint
        RecordedRequest purchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", purchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, purchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set user id
        final String userId = "testUser2";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, true);
        IterableApi.getInstance().setUserId(userId, identityResolution);

        // check that request was sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());

        // check that a second consent tracking request was not sent
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, request.getPath());

        // check that user id was set
        assertEquals(userId, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testIdentifiedUserIdDefault() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set user id
        final String userId1 = "testUser1";
        IterableApi.getInstance().setUserId(userId1);

        // check that user id was set
        assertEquals(userId1, IterableApi.getInstance().getUserId());

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set different user id
        final String userId2 = "testUser2";
        IterableApi.getInstance().setUserId(userId2);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());

        // check that user id was set
        assertEquals(userId2, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testIdentifiedUserIdMergeFalse() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set user id
        final String userId1 = "testUser1";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setUserId(userId1, identityResolution);

        // check that user id was set
        assertEquals(userId1, IterableApi.getInstance().getUserId());

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set different user id
        final String userId2 = "testUser2";
        IterableIdentityResolution identityResolution2 = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setUserId(userId2, identityResolution2);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());

        // check that user id was set
        assertEquals(userId2, IterableApi.getInstance().getUserId());
    }

    @Test
    public void testIdentifiedUserIdMergeTrue() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set user id
        final String userId1 = "testUser1";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, true);
        IterableApi.getInstance().setUserId(userId1, identityResolution);

        // check that user id was set
        assertEquals(userId1, IterableApi.getInstance().getUserId());

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set different user id
        final String userId2 = "testUser2";
        IterableIdentityResolution identityResolution2 = new IterableIdentityResolution(true, true);
        IterableApi.getInstance().setUserId(userId2, identityResolution2);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());

        // check that user id was set
        assertEquals(userId2, IterableApi.getInstance().getUserId());
    }

    // all email tests
    @Test
    public void testCriteriaNotMetEmailDefault() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email);

        // check that request was sent to purchase endpoint on event replay
        RecordedRequest purchaseRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(purchaseRequest2);
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE), purchaseRequest2.getPath());

        // check that request was not sent to merge endpoint and sent to consent tracking endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(consentRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), consentRequest.getPath());
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), consentRequest.getPath());

        // verify track consent request body contains proper email and isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());
        assertEquals(email, consentRequestJson.getString(IterableConstants.KEY_EMAIL));
        assertTrue(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaNotMetEmailReplayTrueMergeFalse() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email = "testUser@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setEmail(email, identityResolution);

        // check that request was sent to purchase endpoint on event replay
        RecordedRequest purchaseRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(purchaseRequest2);
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE), purchaseRequest2.getPath());

        // check that request was not sent to merge endpoint and sent to consent tracking endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(consentRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), consentRequest.getPath());
        assertEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), consentRequest.getPath());

        // verify track consent request body contains proper email and isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());
        assertEquals(email, consentRequestJson.getString(IterableConstants.KEY_EMAIL));
        assertTrue(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaNotMetEmailReplayFalseMergeFalse() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email = "testUser@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(false, false);
        IterableApi.getInstance().setEmail(email, identityResolution);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertNotEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), mergeRequest.getPath());

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaNotMetEmailReplayFalseMergeTrue() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 5, 1);
        shadowOf(getMainLooper()).idle();

        // check that request was not sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be an unknown user session request", unknownSessionRequest);

        // check that request was not sent to track purchase endpoint
        RecordedRequest purchaseRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("There should not be a purchase request", purchaseRequest1);

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email = "testUser@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(false, true);
        IterableApi.getInstance().setEmail(email, identityResolution);

        // check that request was not sent to merge endpoint
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), request.getPath());
        assertNotEquals(("/" + IterableConstants.ENDPOINT_TRACK_CONSENT), request.getPath());

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaMetEmailDefault() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if request was sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", unknownSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, unknownSessionRequest.getPath());

        // check if request was sent to track purchase endpoint
        RecordedRequest purchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", purchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, purchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email to trigger merging
        final String email = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email);

        // check if request was sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());

        // check that a second consent tracking request was not sent
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, request.getPath());

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaMetEmailMergeFalse() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if request was sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", unknownSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, unknownSessionRequest.getPath());

        // check if request was sent to track purchase endpoint
        RecordedRequest purchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", purchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, purchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email = "testUser@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setEmail(email, identityResolution);

        // check if request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());
        assertNotEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, mergeRequest.getPath());

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaMetEmailMergeTrue() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if request was sent to unknown user session endpoint
        RecordedRequest unknownSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", unknownSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, unknownSessionRequest.getPath());

        // check if request was sent to track purchase endpoint
        RecordedRequest purchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", purchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, purchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock merge response
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email = "testUser@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, true);
        IterableApi.getInstance().setEmail(email, identityResolution);

        // check if request was sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());

        // check that a second consent tracking request was not sent
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNotEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, request.getPath());

        // check that email was set
        assertEquals(email, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testIdentifiedEmailDefault() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email1 = "testUser1@gmail.com";
        IterableApi.getInstance().setEmail(email1);

        // check that email was set
        assertEquals(email1, IterableApi.getInstance().getEmail());

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set different email
        final String email2 = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email2);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());

        // check that email was set
        assertEquals(email2, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testIdentifiedEmailMergeFalse() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email1 = "testUser1@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, false);
        IterableApi.getInstance().setEmail(email1, identityResolution);

        // check that email was set
        assertEquals(email1, IterableApi.getInstance().getEmail());

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set different email
        final String email2 = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email2, identityResolution);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals(("/" + IterableConstants.ENDPOINT_MERGE_USER), mergeRequest.getPath());

        // check that email was set
        assertEquals(email2, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testIdentifiedEmailMergeTrue() throws Exception {
        addResponse(IterableConstants.ENDPOINT_MERGE_USER);

        // set email
        final String email1 = "testUser1@gmail.com";
        IterableIdentityResolution identityResolution = new IterableIdentityResolution(true, true);
        IterableApi.getInstance().setEmail(email1, identityResolution);

        // check that email was set
        assertEquals(email1, IterableApi.getInstance().getEmail());

        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // set different email
        final String email2 = "testUser2@gmail.com";
        IterableApi.getInstance().setEmail(email2, identityResolution);

        // check that request was not sent to merge endpoint
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        assertNotEquals("/" + IterableConstants.ENDPOINT_MERGE_USER, mergeRequest.getPath());

        // check that email was set
        assertEquals(email2, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testCriteriaMetTwice() throws Exception {
        // clear any pending requests
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }

        // mock unknown user session response and track purchase response
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_PURCHASE);
        addResponse(IterableConstants.ENDPOINT_TRACK_CONSENT);

        // trigger track purchase event
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        triggerTrackPurchaseEvent("test", "keyboard", 4.67, 3);
        shadowOf(getMainLooper()).idle();

        // check if only one request was sent to unknown user session endpoint
        RecordedRequest anonSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Unknown user session request should not be null", anonSessionRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_ANON_SESSION, anonSessionRequest.getPath());

        // check if first request was sent to track purchase endpoint
        RecordedRequest firstPurchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", firstPurchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, firstPurchaseRequest .getPath());

        // check if second request was sent to track purchase endpoint
        RecordedRequest secondPurchaseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Purchase request should not be null", secondPurchaseRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, secondPurchaseRequest.getPath());

        // check if request was sent to track consent endpoint
        RecordedRequest consentRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("Consent tracking request should be sent", consentRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_CONSENT, consentRequest.getPath());

        // verify track consent request body contains proper isUserKnown flag
        JSONObject consentRequestJson = new JSONObject(consentRequest.getBody().readUtf8());;
        assertFalse(consentRequestJson.getBoolean(IterableConstants.KEY_IS_USER_KNOWN));
    }
}
