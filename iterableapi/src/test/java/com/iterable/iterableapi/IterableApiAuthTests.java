package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.iterable.iterableapi.IterableConstants.HEADER_SDK_AUTH_FORMAT;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class IterableApiAuthTests extends BaseTest {

    private IterableUtil.IterableUtilImpl originalIterableUtil;
    private IterableUtil.IterableUtilImpl iterableUtilSpy;
    private MockWebServer server;
    private IterableAuthHandler authHandler;
    private PathBasedQueueDispatcher dispatcher;

    private String validJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjI5MTYyMzkwMjJ9.mYtgSqdUIxK8_RnYBTUP4cmpKw83aKi7cMiixF3qMB4";
    private String newJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE5MTYyMzkwMjJ9.dMD3MLuHTiO-Qy9PvOoMchNM4CzFIgI7jKVrRtlqlM0";
    private String expiredJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyNDkwMjJ9.6Yc3QcBGwCdV1sdKmgOtw4D69P_HUoVqEW3YMuEgH8c";

    @Before
    public void setUp() {

        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        reInitIterableApi();

        originalIterableUtil = IterableUtil.instance;
        iterableUtilSpy = spy(originalIterableUtil);
        IterableUtil.instance = iterableUtilSpy;

        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setAuthHandler(authHandler);
            }
        });
    }

    @After
    public void tearDown() throws IOException {
        IterableUtil.instance = originalIterableUtil;
        iterableUtilSpy = null;
        server.shutdown();
        server = null;
    }

    private void reInitIterableApi() {
        IterableApi.sharedInstance = spy(new IterableApi());
//        IterableAuthManager authManagerMock = mock(IterableAuthManager.class);
//        doReturn(authManagerMock).when(IterableApi.sharedInstance).getAuthManager();
        authHandler = mock(IterableAuthHandler.class);
    }

    @Test
    public void testRefreshToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        Timer timer = IterableApi.getInstance().getAuthManager().timer;

        IterableApi api = IterableApi.getInstance();
        IterableAuthManager authManager = api.getAuthManager();

        String email = "test@example.com";
        IterableApi.getInstance().setEmail(email);
        timer = IterableApi.getInstance().getAuthManager().timer;

        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        timer = IterableApi.getInstance().getAuthManager().timer;

        String email2 = "test2@example.com";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email2);
        timer = IterableApi.getInstance().getAuthManager().timer;
        int timerCounts = timer.purge();

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email2);
        timer = IterableApi.getInstance().getAuthManager().timer;
    }

    @Test
    public void testSetEmailWithToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), null);

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        String email2 = "test2@example.com";
        IterableApi.getInstance().setEmail(email2);
        assertEquals(IterableApi.getInstance().getEmail(), email2);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);
    }

    @Test
    public void testSetEmailWithTokenExpired() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();

        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Test
    public void testSetUserIdWithToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        IterableApi.getInstance().setUserId(userId);
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals(null, IterableApi.getInstance().getAuthToken());

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals(validJWT, IterableApi.getInstance().getAuthToken());

        String userId2 = "testUserId2";
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId2);

        assertEquals(userId2, IterableApi.getInstance().getUserId());
        assertEquals(expiredJWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSameEmailWithNewToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), null);

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), newJWT);
    }

    @Test
    public void testSameUserIdWithNewToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), newJWT);
    }

    @Test
    public void testSetSameEmailAndRemoveToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(null).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameUserIdAndRemoveToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(null).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameEmail() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";

        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());

        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameUserId() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";

        IterableApi.getInstance().setUserId(userId);

        assertNull(IterableApi.getInstance().getEmail());
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());

        IterableApi.getInstance().setUserId(userId);

        assertNull(IterableApi.getInstance().getEmail());
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameEmailWithSameToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        String token = validJWT;

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);
    }

    @Test
    public void testSetSameUserIdWithSameToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        String token = validJWT;

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);
    }

    @Test
    public void testEmailLogOut() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);

        IterableApi.getInstance().setEmail(null);

        assertNull(IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testUserIdLogOut() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);

        IterableApi.getInstance().setUserId(null);

        assertNull(IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testAuthTokenPresentInRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        RecordedRequest getMessagesRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(getMessagesRequest.getHeader("Authorization"));

        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("new@email.com");
        RecordedRequest getMessagesAuthenticatedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest getMessagesAuthenticatedRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(getMessagesAuthenticatedRequest);
        assertEquals(HEADER_SDK_AUTH_FORMAT + expiredJWT, getMessagesAuthenticatedRequest.getHeader("Authorization"));

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().updateEmail("newEmail@gmail.com", null, null);
        RecordedRequest updateEmailRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest getMessagesUpdatedRequest = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(updateEmailRequest);
        assertEquals(HEADER_SDK_AUTH_FORMAT + expiredJWT, updateEmailRequest.getHeader("Authorization"));
        assertNull(getMessagesUpdatedRequest);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("new@email.com");
        RecordedRequest getMessagesSet2Request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals(HEADER_SDK_AUTH_FORMAT + newJWT, getMessagesSet2Request.getHeader("Authorization"));
    }

    @Test
    public void testAuthFailureReturns401() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        dispatcher.enqueueResponse("/events/inAppConsume", new MockResponse().setResponseCode(401).setBody("{\"code\": \"InvalidJwtPayload\"}"));
        IterableApi.getInstance().inAppConsume(new IterableInAppMessage("asd", null, null, null, null, null, null, null, (long) 2), null, null);
        Robolectric.flushForegroundThreadScheduler();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Test
    public void testAuthRequestedOnSetEmail() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("someEmail@domain.com");
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().updateEmail("someNewEmail@domain.com");
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

    }

    @Test
    public void testAuthRequestedOnUpdateEmail() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("someEmail@domain.com");

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().updateEmail("someNewEmail@domain.com");
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        //TODO: Shouldn't the update call also update the authToken in IterableAPI class?
    }

    @Test
    public void testAuthRequestedOnSetUserId() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId("SomeUser");
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Test
    public void testAuthSetToNullOnLogOut() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId("SomeUser");
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        IterableApi.getInstance().setUserId(null);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testRegisterForPushInvokedAfterTokenRefresh() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("someEmail@domain.com");
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        //TODO: Verify if registerForPush is invoked
    }

}
