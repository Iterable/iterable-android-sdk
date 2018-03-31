package com.iterable.iterableapi;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IterableApiRequestsTest extends ApplicationTestCase<Application> {

    public IterableApiRequestsTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createIterableApi();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void createIterableApi() {
        IterableApi.sharedInstanceWithApiKey(getContext(), "fake_key", "test_email");
    }

    public void testTrackPurchase() throws Exception {
        MockWebServer server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        String expectedRequest = "{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100}";

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);

        IterableApi.sharedInstance.trackPurchase(100.0, items);

        RecordedRequest request = server.takeRequest();
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    public void testTrackPurchaseWithDataFields() throws Exception {
        MockWebServer server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        String expectedRequest = "{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100,\"dataFields\":{\"field\":\"testValue\"}}";

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);
        JSONObject dataFields = new JSONObject();
        dataFields.put("field", "testValue");

        IterableApi.sharedInstance.trackPurchase(100.0, items, dataFields);

        RecordedRequest request = server.takeRequest();
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        assertEquals(expectedRequest, request.getBody().readUtf8());
    }
}
