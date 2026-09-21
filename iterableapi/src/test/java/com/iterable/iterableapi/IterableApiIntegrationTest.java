package com.iterable.iterableapi;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowPausedAsyncTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
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
        IterableApi.sharedInstance = IterableTestUtils.newApiWithInlineRequests(inAppManagerMock);

        originalPushRegistrationUtil = IterablePushRegistrationTask.Util.instance;
        pushRegistrationUtilMock = mock(IterablePushRegistrationTask.Util.UtilImpl.class);
        IterablePushRegistrationTask.Util.instance = pushRegistrationUtilMock;
        ShadowPausedAsyncTask.reset(); // Enable real threading in AsyncTask so we keep the execution sequence similar to the real one.
    }

    @After
    public void tearDown() throws Exception {
        waitForRequestWorkToFinish();
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;

        server.shutdown();
        server = null;
    }

    @Test
    public void testDisablePushOnLogout() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
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

    @Test
    public void testRegisterForPushFollowedByDisablePushPreservesRequestOrder() throws Exception {
        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        shadowOf(getMainLooper()).idle();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.getInstance().registerForPush();
        IterableApi.getInstance().disablePush();

        RecordedRequest registrationRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(registrationRequest);
        assertEquals(
                "/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN,
                registrationRequest.getPath()
        );

        RecordedRequest disableRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(disableRequest);
        assertEquals(
                "/" + IterableConstants.ENDPOINT_DISABLE_DEVICE,
                disableRequest.getPath()
        );
    }

    @Test
    public void testDisablePushPreventsFailedRegistrationFromRetrying() throws Exception {
        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        shadowOf(getMainLooper()).idle();
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.getInstance().registerForPush();
        IterableApi.getInstance().disablePush();

        RecordedRequest registrationRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(registrationRequest);
        assertEquals(
                "/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN,
                registrationRequest.getPath()
        );

        RecordedRequest disableRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(disableRequest);
        assertEquals(
                "/" + IterableConstants.ENDPOINT_DISABLE_DEVICE,
                disableRequest.getPath()
        );

        waitForRequestWorkToFinish();

        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void testPushRegistrationRetryDoesNotWaitForHostAsyncTaskSerialExecutor() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(true).build());

        CountDownLatch hostTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseHostTask = new CountDownLatch(1);
        AsyncTask.SERIAL_EXECUTOR.execute(() -> {
            hostTaskStarted.countDown();
            try {
                releaseHostTask.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            assertTrue(hostTaskStarted.await(5, TimeUnit.SECONDS));
            IterableApi.getInstance().setEmail("test@email.com");
            shadowOf(getMainLooper()).idle();
            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            JSONObject requestJson = new JSONObject(request.getBody().readUtf8());
            assertEquals("/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, request.getPath());
            assertEquals("test@email.com", requestJson.getString(IterableConstants.KEY_EMAIL));
            JSONObject deviceJson = requestJson.getJSONObject(IterableConstants.KEY_DEVICE);
            assertEquals(TEST_TOKEN, deviceJson.getString(IterableConstants.KEY_TOKEN));

            waitForExecutor(IterableExecutors.push());
            shadowOf(getMainLooper()).idle();

            RecordedRequest retryRequest = server.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull(retryRequest);
            assertEquals(
                    "/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN,
                    retryRequest.getPath()
            );
        } finally {
            releaseHostTask.countDown();
        }
    }

    private void waitForRequestWorkToFinish() throws InterruptedException {
        waitForExecutor(IterableExecutors.push());
        shadowOf(getMainLooper()).idle();
        waitForExecutor(IterableExecutors.push());
        waitForExecutor(AsyncTask.SERIAL_EXECUTOR);
        shadowOf(getMainLooper()).idle();
    }

    private void waitForExecutor(Executor executor) throws InterruptedException {
        CountDownLatch executorDrained = new CountDownLatch(1);
        executor.execute(executorDrained::countDown);
        assertTrue(executorDrained.await(5, TimeUnit.SECONDS));
    }
}
