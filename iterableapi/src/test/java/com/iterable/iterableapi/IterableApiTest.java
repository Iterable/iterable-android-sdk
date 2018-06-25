package com.iterable.iterableapi;

import com.iterable.iterableapi.CommerceItem;
import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.unit.BaseTest;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class IterableApiTest extends BaseTest {

    private MockWebServer server;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    @Test
    public void testSdkInitializedWithoutEmailOrUserId() throws Exception {
        IterableApi.sharedInstance = new IterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setEmail(null);

        // Verify that none of the calls to the API result in a request
        IterableApi.getInstance().track("testEvent");
        IterableApi.getInstance().trackInAppOpen("12345");
        IterableApi.getInstance().inAppConsume("12345");
        IterableApi.getInstance().trackInAppClick("12345", "");
        IterableApi.getInstance().registerDeviceToken("12345");
        IterableApi.getInstance().disablePush("12345", "12345");
        IterableApi.getInstance().updateUser(new JSONObject());
        IterableApi.getInstance().updateEmail("");
        IterableApi.getInstance().trackPurchase(10.0, new ArrayList<CommerceItem>());

        RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
        assertNull(request);
    }

    @Test
    public void testEmailUserIdPersistence() throws Exception {
        IterableApi.sharedInstance = new IterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setEmail("test@email.com");

        IterableApi.sharedInstance = new IterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        assertEquals("test@email.com", IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());

        IterableApi.getInstance().setUserId("testUserId");
        IterableApi.sharedInstance = new IterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        assertEquals("testUserId", IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getEmail());
    }

}
