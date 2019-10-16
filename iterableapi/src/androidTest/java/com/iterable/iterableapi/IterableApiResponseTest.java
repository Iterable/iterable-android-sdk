package com.iterable.iterableapi;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.iterable.iterableapi.IterableTestUtils.createIterableApi;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class IterableApiResponseTest {

    private MockWebServer server;

    @Before
    public void setUp() {
        createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
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

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(JSONObject data) {
                assertEquals(responseData.toString(), data.toString());
                signal.countDown();
            }
        }, null);
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onSuccess is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode200WithNoData() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(200, (String) null);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                assertEquals("No data received", reason);
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode200WithInvalidJson() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(200, "{'''}}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                assertThat(reason, CoreMatchers.containsString("Could not parse json"));
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode400WithoutMessage() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(400, "{}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                assertEquals("Invalid Request", reason);
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }


    @Test
    public void testResponseCode400WithMessage() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        final JSONObject responseData = new JSONObject("{\"msg\":\"Test error\"}");
        stubAnyRequestReturningStatusCode(400, responseData);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                assertEquals("Test error", reason);
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode401() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(401, "{}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                assertEquals("Invalid API Key", reason);
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testResponseCode500() throws Exception {
        for (int i = 0; i < 5; i++) {
            stubAnyRequestReturningStatusCode(500, "{}");
        }

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, null);
        IterableRequest task = new IterableRequest();
        task.execute(request);

        RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest request2 = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull("Retries after 500 status code", request2);
    }

    @Test
    public void testNon200ResponseCode() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        stubAnyRequestReturningStatusCode(302, "{}");

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                assertThat(reason, CoreMatchers.containsString("Received non-200 response"));
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectionError() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        MockResponse response = new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY);
        server.enqueue(response);

        IterableApiRequest request = new IterableApiRequest("fake_key", "", new JSONObject(), IterableApiRequest.POST, null, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(String reason, JSONObject data) {
                signal.countDown();
            }
        });
        new IterableRequest().execute(request);

        server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue("onFailure is called", signal.await(1, TimeUnit.SECONDS));
    }
}
