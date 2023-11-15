package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static com.iterable.iterableapi.IterableTestUtils.createIterableApi;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class IterableApiResponseTest {

    private MockWebServer server;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        createIterableApi();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    private void stubAnyRequestReturningStatusCode(int statusCode, JSONObject data) {
        String body = null;
        if (data != null)
            body = data.toString();
        stubAnyRequestReturningStatusCode(statusCode, body);
    }

    private void stubAnyRequestReturningStatusCode(int statusCode, String body) {
        MockResponse response = new MockResponse().setResponseCode(statusCode);
        if (body != null) {
            response.setBody(body);
        }
        server.enqueue(response);
    }

    @Test
    public void testResponseCode200() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        final JSONObject responseData = new JSONObject("{\"key\":\"value\"}");
        stubAnyRequestReturningStatusCode(200, responseData);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                assertEquals(responseData.toString(), data.toString());
                signal.countDown();
            }
        }, null);
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onSuccess is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode200WithNoData() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(200, (String) null);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertEquals("No data received", reason);
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode200WithInvalidJson() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(200, "{'''}}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertThat(reason, CoreMatchers.containsString("Could not parse json"));
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode400WithoutMessage() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(400, "{}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertEquals("Invalid Request", reason);
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }


    @Test
    public void testResponseCode400WithMessage() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        final JSONObject responseData = new JSONObject("{\"msg\":\"Test error\"}");
        stubAnyRequestReturningStatusCode(400, responseData);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertEquals("Test error", reason);
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode401() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(401, "{}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertEquals("Invalid API Key", reason);
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode401AuthError() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(401, "{\"msg\":\"JWT Authorization header error\",\"code\":\"InvalidJwtPayload\"}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertEquals("JWT Authorization header error", reason);
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testRetryOnInvalidJwtPayload() throws Exception {
        final CountDownLatch signal = new CountDownLatch(3);
        stubAnyRequestReturningStatusCode(401, "{\"msg\":\"JWT Authorization header error\",\"code\":\"InvalidJwtPayload\"}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                try {
                    if (data != null && "InvalidJwtPayload".equals(data.optString("code"))) {
                        final JSONObject responseData = new JSONObject("{\n" +
                                "   \"key\":\"Success\",\n" +
                                "   \"message\":\"Event tracked successfully.\"\n" +
                                "}");
                        stubAnyRequestReturningStatusCode(200, responseData);

                        new IterableRequestTask().execute(new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.SuccessHandler() {
                            @Override
                            public void onSuccess(@NonNull JSONObject successData) {
                                try {
                                    assertEquals(responseData.toString(), successData.toString());
                                } catch (AssertionError e) {
                                    e.printStackTrace();
                                } finally {
                                    signal.countDown();
                                }
                            }
                        }, null));
                        server.takeRequest(2, TimeUnit.SECONDS);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    signal.countDown();
                }
            }
        });

        new IterableRequestTask().execute(request);
        server.takeRequest(1, TimeUnit.SECONDS);

        // Await for the background tasks to complete
        signal.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testMaxRetriesOnMultipleInvalidJwtPayloads() throws Exception {
        for (int i = 0; i < 5; i++) {
            stubAnyRequestReturningStatusCode(401, "{\"msg\":\"JWT Authorization header error\",\"code\":\"InvalidJwtPayload\"}");
        }

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, null);
        IterableRequestTask task = new IterableRequestTask();
        task.execute(request);

        RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest request2 = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest request3 = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest request4 = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest request5 = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest request6 = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest request7 = server.takeRequest(5, TimeUnit.SECONDS);
        assertNull("Request should be null since retries hit the max of 5", request7);
    }

    @Test
    public void testResponseCode500() throws Exception {
        for (int i = 0; i < 5; i++) {
            stubAnyRequestReturningStatusCode(500, "{}");
        }

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, null);
        IterableRequestTask task = new IterableRequestTask();
        task.execute(request);

        RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest request2 = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull("Retries after 500 status code", request2);
    }

    @Test
    public void testNon200ResponseCode() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(302, "{}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertThat(reason, CoreMatchers.containsString("Received non-200 response"));
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectionError() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        MockResponse response = new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY);
        server.enqueue(response);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                signal.countDown();
            }
        });
        new IterableRequestTask().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }
}
