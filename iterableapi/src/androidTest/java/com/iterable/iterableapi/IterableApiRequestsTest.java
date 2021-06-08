package com.iterable.iterableapi;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.iterable.iterableapi.IterableTestUtils.createIterableApi;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IterableApiRequestsTest {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        server = null;
    }

    @Test
    public void testPostRequestHeaders() throws Exception {
        IterableApi.sharedInstance.track("customEvent");
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK, request.getPath());
        assertEquals("Android", request.getHeader(IterableConstants.HEADER_SDK_PLATFORM));
        assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, request.getHeader(IterableConstants.HEADER_SDK_VERSION));
        assertNotNull(request.getHeader(IterableConstants.KEY_SENT_AT));
        assertEquals("fake_key", request.getHeader(IterableConstants.HEADER_API_KEY));
    }

    @Test
    public void testGetRequestHeaders() throws Exception {
        IterableApi.sharedInstance.getInAppMessages(1, null);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue(request.getPath().startsWith("/" + IterableConstants.ENDPOINT_GET_INAPP_MESSAGES));
        assertFalse(request.getPath().contains("api_key"));
        assertEquals("Android", request.getHeader(IterableConstants.HEADER_SDK_PLATFORM));
        assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, request.getHeader(IterableConstants.HEADER_SDK_VERSION));
        assertEquals("fake_key", request.getHeader(IterableConstants.HEADER_API_KEY));
    }

    @Test
    public void testTrackPurchase() throws Exception {
        String expectedRequest = new StringBuilder(new StringBuffer("{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100").append(",\"createdAt\":").append(new Date().getTime() / 1000).append("}")).toString();

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);

        IterableApi.sharedInstance.trackPurchase(100.0, items);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Test
    public void testTrackPurchaseWithOptionalParameters() throws Exception {
        CommerceItem item = new CommerceItem("273",
                "Bow and Arrow",
                42,
                1,
                "DIAMOND-IS-UNBREAKABLE",
                "When a living creature is pierced by one of the Arrows, it will catalyze and awaken the individual’s dormant Stand.",
                "placeholderUrl",
                "placeholderImageUrl",
                new String[] {""});
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item);

        IterableApi.sharedInstance.trackPurchase(0, items);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());

        String expectedRequest = new StringBuilder(
                new StringBuffer("{\"user\":{\"email\":\"test_email\"},")
                        .append("\"items\":[{\"id\":\"273\",\"name\":\"Bow and Arrow\",\"price\":42,\"quantity\":1,\"sku\":\"DIAMOND-IS-UNBREAKABLE\",\"description\":\"When a living creature is pierced by one of the Arrows, it will catalyze and awaken the individual’s dormant Stand.\",\"url\":\"placeholderUrl\",\"imageUrl\":\"placeholderImageUrl\",\"categories\":[\"\"]}],")
                        .append("\"total\":42,")
                        .append("\"createdAt\":")
                        .append(new Date().getTime() / 1000)
                        .append("}")).toString();

        assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Test
    public void testTrackPurchaseWithDataFields() throws Exception {
        String expectedRequest = new StringBuilder(new StringBuffer("{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100,\"dataFields\":{\"field\":\"testValue\"}").append(",\"createdAt\":").append(new Date().getTime() / 1000).append("}")).toString();

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);
        JSONObject dataFields = new JSONObject();
        dataFields.put("field", "testValue");

        IterableApi.sharedInstance.trackPurchase(100.0, items, dataFields);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Ignore("Ignoring the JWT related test error")
    @Test
    public void testUpdateEmailRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        // Plain request check
        IterableApi.sharedInstance.updateEmail("test@example.com");

        RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request1);
        assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_EMAIL, request1.getPath());
        assertEquals("{\"currentEmail\":\"test_email\",\"newEmail\":\"test@example.com\"}", request1.getBody().readUtf8());
        Thread.sleep(100); // We need the callback to run to verify the internal email field change

        server.enqueue(new MockResponse().setResponseCode(400).setBody("{}"));

        // Check that we handle failures properly
        IterableApi.sharedInstance.updateEmail("invalid_mail!!123");

        RecordedRequest request2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request2);
        assertEquals("{\"currentEmail\":\"test@example.com\",\"newEmail\":\"invalid_mail!!123\"}", request2.getBody().readUtf8());
        Thread.sleep(100); // We need the callback to run to verify the internal email field change

        // Check that we still pass a valid (old) email after trying to update to an invalid one
        IterableApi.sharedInstance.updateEmail("another@email.com");

        RecordedRequest request3 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request3);
        assertEquals("{\"currentEmail\":\"test@example.com\",\"newEmail\":\"another@email.com\"}", request3.getBody().readUtf8());
    }

    @Test
    public void testTrackWithoutCampaignIdTemplateId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.sharedInstance.track("testEvent");
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);

        JSONObject requestJson = new JSONObject(request.getBody().readUtf8());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK, request.getPath());
        assertFalse("campaignId should not be set in the request", requestJson.has(IterableConstants.KEY_CAMPAIGN_ID));
        assertFalse("templateId should not be set in the request", requestJson.has(IterableConstants.KEY_TEMPLATE_ID));

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.sharedInstance.track("testEvent", 1234, 4321);
        request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);

        requestJson = new JSONObject(request.getBody().readUtf8());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK, request.getPath());
        assertTrue("campaignId should be set in the request", requestJson.has(IterableConstants.KEY_CAMPAIGN_ID));
        assertTrue("templateId should be set in the request", requestJson.has(IterableConstants.KEY_TEMPLATE_ID));
        assertEquals(1234, requestJson.getInt(IterableConstants.KEY_CAMPAIGN_ID));
        assertEquals(4321, requestJson.getInt(IterableConstants.KEY_TEMPLATE_ID));
    }
}
