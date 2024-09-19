package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IterableApiCustomEventTests extends BaseTest {
    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;

    @Before
    public void setUp() {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
        reInitIterableApi();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
        IterableApi.initialize(getContext(), "apiKey", iterableConfig);

        String criteriaMockData = "{\n" +
                "    \"count\": 1,\n" +
                "    \"criteriaSets\": [\n" +
                "        {\n" +
                "            \"criteriaId\": \"423\",\n" +
                "            \"name\": \"animal-found Test Cases\",\n" +
                "            \"createdAt\": 1726648931809,\n" +
                "            \"updatedAt\": 1726648931809,\n" +
                "            \"searchQuery\": {\n" +
                "                \"combinator\": \"And\",\n" +
                "                \"searchQueries\": [\n" +
                "                    {\n" +
                "                        \"combinator\": \"And\",\n" +
                "                        \"searchQueries\": [\n" +
                "                            {\n" +
                "                                \"dataType\": \"customEvent\",\n" +
                "                                \"searchCombo\": {\n" +
                "                                    \"combinator\": \"And\",\n" +
                "                                    \"searchQueries\": [\n" +
                "                                        {\n" +
                "                                            \"dataType\": \"customEvent\",\n" +
                "                                            \"field\": \"animal-found.count\",\n" +
                "                                            \"comparatorType\": \"Equals\",\n" +
                "                                            \"value\": \"6\",\n" +
                "                                            \"fieldType\": \"long\"\n" +
                "                                        },\n" +
                "                                        {\n" +
                "                                            \"dataType\": \"customEvent\",\n" +
                "                                            \"field\": \"animal-found.type\",\n" +
                "                                            \"comparatorType\": \"Equals\",\n" +
                "                                            \"value\": \"cat\",\n" +
                "                                            \"fieldType\": \"string\"\n" +
                "                                        },\n" +
                "                                        {\n" +
                "                                            \"dataType\": \"customEvent\",\n" +
                "                                            \"field\": \"animal-found.vaccinated\",\n" +
                "                                            \"comparatorType\": \"Equals\",\n" +
                "                                            \"value\": \"true\",\n" +
                "                                            \"fieldType\": \"boolean\"\n" +
                "                                        },\n" +
                "                                        {\n" +
                "                                            \"dataType\": \"customEvent\",\n" +
                "                                            \"field\": \"eventName\",\n" +
                "                                            \"comparatorType\": \"Equals\",\n" +
                "                                            \"value\": \"animal-found\",\n" +
                "                                            \"fieldType\": \"string\"\n" +
                "                                        }\n" +
                "                                    ]\n" +
                "                                }\n" +
                "                            }\n" +
                "                        ]\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";

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
    private void addResponse(String endPoint) {
        dispatcher.enqueueResponse("/" + endPoint, new MockResponse().setResponseCode(200).setBody("{}"));
    }

    @Test
    public void testCustomEventTrackApi() throws Exception {
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        addResponse(IterableConstants.ENDPOINT_TRACK_ANON_SESSION);
        addResponse(IterableConstants.ENDPOINT_TRACK);
        final String userId = "testUser2";
        IterableApi.getInstance().setUserId(userId, false);
        while (server.takeRequest(1, TimeUnit.SECONDS) != null) { }
        JSONObject customEventItem = new JSONObject("{\n" +
                "  \"dataFields\": " +
                "   {\n" +
                "       \"type\":\"cat\",\n" +
                "       \"count\":6,\n" +
                "       \"vaccinated\":true\n" +
                "  }\n" +
                "}");

        JSONObject items = new JSONObject(String.valueOf(customEventItem.getJSONObject(IterableConstants.KEY_DATA_FIELDS)));
        IterableApi.getInstance().track("animal-found", items);
        RecordedRequest mergeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(mergeRequest);
        shadowOf(getMainLooper()).idle();
        JSONObject requestJson = new JSONObject(mergeRequest.getBody().readUtf8());

        assertEquals("/" + IterableConstants.ENDPOINT_TRACK, mergeRequest.getPath());
        assertTrue("dataField should be set in the request", requestJson.has(IterableConstants.KEY_DATA_FIELDS));

        JSONObject dataFieldObject = requestJson.getJSONObject(IterableConstants.KEY_DATA_FIELDS);

        assertTrue(dataFieldObject.has("type"));
        assertTrue(dataFieldObject.has("count"));
        assertTrue(dataFieldObject.has("vaccinated"));

        assertFalse(dataFieldObject.has("animal-found.type"));
        assertFalse(dataFieldObject.has("animal-found.count"));
        assertFalse(dataFieldObject.has("animal-found.vaccinated"));

        assertEquals(dataFieldObject.getString("type"), "cat");
        assertEquals(dataFieldObject.getInt("count"), 6);
        assertTrue(dataFieldObject.getBoolean("vaccinated"));
    }
}
