package com.iterable.iterableapi;

import android.content.Context;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowPausedAsyncTask;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class IterableApiIntegrationTest extends BaseTest {
    private static final String TEST_TOKEN = "testToken";

    private MockWebServer server;
    private IterablePushRegistrationTask.Util.UtilImpl originalPushRegistrationUtil;
    private IterablePushRegistrationTask.Util.UtilImpl pushRegistrationUtilMock;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableInAppManager inAppManagerMock = mock(IterableInAppManager.class);
        IterableApi.sharedInstance = new IterableApi(inAppManagerMock);

        originalPushRegistrationUtil = IterablePushRegistrationTask.Util.instance;
        pushRegistrationUtilMock = mock(IterablePushRegistrationTask.Util.UtilImpl.class);
        IterablePushRegistrationTask.Util.instance = pushRegistrationUtilMock;
        ShadowPausedAsyncTask.reset(); // Enable real threading in AsyncTask so we keep the execution sequence similar to the real one.
    }

    @After
    public void tearDown() throws IOException {
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;

        server.shutdown();
        server = null;
    }

    @Test
    public void testDisablePushOnLogout() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        when(pushRegistrationUtilMock.getFirebaseResouceId(any(Context.class))).thenReturn(1);
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(true).build());
        IterableApi.getInstance().setEmail("test@email.com");
        shadowOf(getMainLooper()).idle();
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        JSONObject requestJson = new JSONObject(request.getBody().readUtf8());
        assertEquals("/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, request.getPath());
        assertEquals("test@email.com", requestJson.getString(IterableConstants.KEY_EMAIL));
        JSONObject deviceJson = requestJson.getJSONObject(IterableConstants.KEY_DEVICE);
        assertEquals(TEST_TOKEN, deviceJson.getString(IterableConstants.KEY_TOKEN));

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.getInstance().setEmail(null);
        request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        requestJson = new JSONObject(request.getBody().readUtf8());
        assertEquals("/" + IterableConstants.ENDPOINT_DISABLE_DEVICE, request.getPath());
        assertEquals("test@email.com", requestJson.getString(IterableConstants.KEY_EMAIL));
        assertEquals(TEST_TOKEN, requestJson.getString(IterableConstants.KEY_TOKEN));
    }
}
