package com.iterable.iterableapi;

import static com.iterable.iterableapi.IterableTestUtils.createIterableApi;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(TestRunner.class)
public class IterableApiRequestTest {

    private MockWebServer server;

    @Before
    public void setUp() {
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
    public void testRequestSerialization() throws Exception {

        String apiKey = "apiKey";
        String resourcePath = "iterable.com/api/testmethod";
        JSONObject data = new JSONObject();
        data.put("SOME_DATA", "somedata");
        String requestType = "api";
        String authToken = "authToken123##";

        IterableHelper.SuccessHandler successHandler = new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                IterableLogger.v("RequestSerializationTest", "Passed");
            }
        };

        IterableHelper.FailureHandler failureHandler = new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                IterableLogger.e("RequestSerializationTest", "Failed");
            }
        };

        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, data, requestType, authToken, successHandler, failureHandler);
        JSONObject requestJSONObject = request.toJSONObject();
        IterableApiRequest newRequest = IterableApiRequest.fromJSON(requestJSONObject, successHandler, failureHandler);
        assert newRequest != null;
        assertEquals(request.apiKey, newRequest.apiKey);
        assertEquals(request.authToken, newRequest.authToken);
        assertEquals(request.requestType, newRequest.requestType);
        assertEquals(request.resourcePath, newRequest.resourcePath);
        assertEquals(request.json.toString(), newRequest.json.toString());
        assertEquals(request.successCallback, newRequest.successCallback);
        assertEquals(request.failureCallback, newRequest.failureCallback);
    }


    @Test
    public void testUpdateCart() throws Exception {
        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);

        IterableApi.sharedInstance.updateCart(items);

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_CART, request.getPath());

        String expectedRequest = new StringBuilder(
            new StringBuffer("{\"user\":{\"email\":\"test_email\"},")
                .append("\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],")
                .append("\"createdAt\":").append(new Date().getTime() / 1000)
                .append("}")).toString();

        String requestBody = request.getBody().readUtf8();
        Assert.assertEquals(expectedRequest, requestBody);
    }

    @Test
    public void testTrackPurchase() throws Exception {
        String expectedRequest = new StringBuilder(new StringBuffer("{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100").append(",\"createdAt\":").append(new Date().getTime() / 1000).append("}")).toString();

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);

        IterableApi.sharedInstance.trackPurchase(100.0, items);

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        Assert.assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Test
    public void testTrackPurchaseWithDataFields() throws Exception {
        String expectedRequest = new StringBuilder(new StringBuffer("{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100,\"dataFields\":{\"field\":\"testValue\"}").append(",\"createdAt\":").append(new Date().getTime() / 1000).append("}")).toString();

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);
        JSONObject dataFields = new JSONObject();
        dataFields.put("field", "testValue");

        IterableApi.sharedInstance.trackPurchase(100.0, items, dataFields, null);

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        Assert.assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Test
    public void testTrackWithoutCampaignIdTemplateId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.sharedInstance.track("testEvent");
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);

        JSONObject requestJson = new JSONObject(request.getBody().readUtf8());
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_TRACK, request.getPath());
        assertFalse("campaignId should not be set in the request", requestJson.has(IterableConstants.KEY_CAMPAIGN_ID));
        assertFalse("templateId should not be set in the request", requestJson.has(IterableConstants.KEY_TEMPLATE_ID));

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.sharedInstance.track("testEvent", 1234, 4321);
        request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);

        requestJson = new JSONObject(request.getBody().readUtf8());
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_TRACK, request.getPath());
        assertTrue("campaignId should be set in the request", requestJson.has(IterableConstants.KEY_CAMPAIGN_ID));
        assertTrue("templateId should be set in the request", requestJson.has(IterableConstants.KEY_TEMPLATE_ID));
        Assert.assertEquals(1234, requestJson.getInt(IterableConstants.KEY_CAMPAIGN_ID));
        Assert.assertEquals(4321, requestJson.getInt(IterableConstants.KEY_TEMPLATE_ID));
    }

    @Test
    public void testTrackPurchaseWithOptionalParameters() throws Exception {
        JSONObject dataFields = new JSONObject();
        dataFields.put("color", "yellow");
        dataFields.put("count", 8);

        CommerceItem item = new CommerceItem("273",
            "Bow and Arrow",
            42,
            1,
            "DIAMOND-IS-UNBREAKABLE",
            "When a living creature is pierced by one of the Arrows, it will catalyze and awaken the individual’s dormant Stand.",
            "placeholderUrl",
            "placeholderImageUrl",
            new String[] {"bow", "arrow"},
            dataFields);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item);

        IterableApi.sharedInstance.trackPurchase(42, items);

        long createdAt = new Date().getTime() / 1000;
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assert request != null;
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());

        String expectedRequest = new StringBuilder(
            new StringBuffer("{\"user\":{\"email\":\"test_email\"},")
                .append("\"items\":[{\"id\":\"273\",\"name\":\"Bow and Arrow\",\"price\":42,\"quantity\":1,\"sku\":\"DIAMOND-IS-UNBREAKABLE\",\"description\":\"When a living creature is pierced by one of the Arrows, it will catalyze and awaken the individual’s dormant Stand.\",\"url\":\"placeholderUrl\",\"imageUrl\":\"placeholderImageUrl\",\"dataFields\":{\"color\":\"yellow\",\"count\":8},\"categories\":[\"bow\",\"arrow\"]}],")
                .append("\"total\":42,").append("\"createdAt\":").append(createdAt)
                .append("}")).toString();

        String requestBody = request.getBody().readUtf8();
        Assert.assertEquals(expectedRequest, requestBody);
    }

    @Test
    public void testGetRequestHeaders() throws Exception {
        IterableApi.sharedInstance.getInAppMessages(1, null);
        RecordedRequest request = server.takeRequest(11, TimeUnit.SECONDS);
        assertTrue(request.getPath().startsWith("/" + IterableConstants.ENDPOINT_GET_INAPP_MESSAGES));
        assertFalse(request.getPath().contains("api_key"));
        Assert.assertEquals("Android", request.getHeader(IterableConstants.HEADER_SDK_PLATFORM));
        Assert.assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, request.getHeader(IterableConstants.HEADER_SDK_VERSION));
        Assert.assertEquals("fake_key", request.getHeader(IterableConstants.HEADER_API_KEY));
    }

    @Test
    public void testPostRequestHeaders() throws Exception {
        IterableApi.sharedInstance.track("customEvent");
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_TRACK, request.getPath());
        Assert.assertEquals("Android", request.getHeader(IterableConstants.HEADER_SDK_PLATFORM));
        Assert.assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, request.getHeader(IterableConstants.HEADER_SDK_VERSION));
        assertNotNull(request.getHeader(IterableConstants.KEY_SENT_AT));
        Assert.assertEquals("fake_key", request.getHeader(IterableConstants.HEADER_API_KEY));
    }

    @Ignore("Ignoring the JWT related test error")
    @Test
    public void testUpdateEmailRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        // Plain request check
        IterableApi.sharedInstance.updateEmail("test@example.com");

        RecordedRequest request1 = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request1);
        Assert.assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_EMAIL, request1.getPath());
        Assert.assertEquals("{\"currentEmail\":\"test_email\",\"newEmail\":\"test@example.com\"}", request1.getBody().readUtf8());
        Thread.sleep(100); // We need the callback to run to verify the internal email field change

        server.enqueue(new MockResponse().setResponseCode(400).setBody("{}"));

        // Check that we handle failures properly
        IterableApi.sharedInstance.updateEmail("invalid_mail!!123");

        RecordedRequest request2 = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request2);
        Assert.assertEquals("{\"currentEmail\":\"test@example.com\",\"newEmail\":\"invalid_mail!!123\"}", request2.getBody().readUtf8());
        Thread.sleep(100); // We need the callback to run to verify the internal email field change

        // Check that we still pass a valid (old) email after trying to update to an invalid one
        IterableApi.sharedInstance.updateEmail("another@email.com");

        RecordedRequest request3 = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request3);
        Assert.assertEquals("{\"currentEmail\":\"test@example.com\",\"newEmail\":\"another@email.com\"}", request3.getBody().readUtf8());
    }
}
