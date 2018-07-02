package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.BaseTest;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.when;

@PrepareForTest(IterableUtil.class)
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

    @Test
    public void testAttributionInfoPersistence() throws Exception {
        IterableApi.sharedInstance = new IterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        IterableAttributionInfo attributionInfo = new IterableAttributionInfo(1234, 4321, "message");
        IterableApi.getInstance().setAttributionInfo(attributionInfo);

        PowerMockito.spy(IterableUtil.class);
        // 23 hours, not expired, still present
        when(IterableUtil.currentTimeMillis()).thenReturn(System.currentTimeMillis() + 3600 * 23 * 1000);
        IterableAttributionInfo storedAttributionInfo = IterableApi.getInstance().getAttributionInfo();
        assertNotNull(storedAttributionInfo);
        assertEquals(attributionInfo.campaignId, storedAttributionInfo.campaignId);
        assertEquals(attributionInfo.templateId, storedAttributionInfo.templateId);
        assertEquals(attributionInfo.messageId, storedAttributionInfo.messageId);

        // 24 hours, expired, attributionInfo should be null
        when(IterableUtil.currentTimeMillis()).thenReturn(System.currentTimeMillis() + 3600 * 24 * 1000);
        storedAttributionInfo = IterableApi.getInstance().getAttributionInfo();
        assertNull(storedAttributionInfo);

        PowerMockito.doCallRealMethod().when(IterableUtil.class, "currentTimeMillis");
    }

}
