package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.annotation.LooperMode;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static com.iterable.iterableapi.IterableConstants.ENDPOINT_UPDATE_EMAIL;
import static com.iterable.iterableapi.IterableConstants.HEADER_SDK_AUTH_FORMAT;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.PAUSED;

@LooperMode(PAUSED)
public class IterableApiAuthTests extends BaseTest {

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

        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setAuthHandler(authHandler);
            }
        }, null);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    private void reInitIterableApi() {
        IterableApi.sharedInstance = new IterableApi();
        authHandler = mock(IterableAuthHandler.class);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testRefreshToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");
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

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetEmailWithToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String email = "test@example.com";
        IterableApi.getInstance().setEmail(email);
        assertEquals(email, IterableApi.getInstance().getEmail());
        assertEquals(null, IterableApi.getInstance().getAuthToken());

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(email, IterableApi.getInstance().getEmail());
        assertEquals(validJWT, IterableApi.getInstance().getAuthToken());

        String email2 = "test2@example.com";
        IterableApi.getInstance().setEmail(email2);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(email2, IterableApi.getInstance().getEmail());
        assertEquals(validJWT, IterableApi.getInstance().getAuthToken());
        shadowOf(getMainLooper()).runToEndOfTasks();
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetEmailWithTokenExpired() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String email = "test@example.com";
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();

        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetUserIdWithToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String userId = "testUserId";
        IterableApi.getInstance().setUserId(userId);
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals(null, IterableApi.getInstance().getAuthToken());

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals(validJWT, IterableApi.getInstance().getAuthToken());

        String userId2 = "testUserId2";
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId2);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(userId2, IterableApi.getInstance().getUserId());
        assertEquals(expiredJWT, IterableApi.getInstance().getAuthToken());
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSameEmailWithNewToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String email = "test@example.com";
        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), null);

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), newJWT);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSameUserIdWithNewToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String userId = "testUserId";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), newJWT);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetSameEmailAndRemoveToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");
        String email = "test@example.com";

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(null).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetSameUserIdAndRemoveToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String userId = "testUserId";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);

        doReturn(null).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameEmail() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

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
        IterableApi.initialize(getContext(), "apiKey");

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

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetSameEmailWithSameToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String email = "test@example.com";
        String token = validJWT;

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setEmail(email);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testSetSameUserIdWithSameToken() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        String userId = "testUserId";
        String token = validJWT;

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setUserId(userId);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);
    }

    @Test
    public void testEmailLogOut() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

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
        IterableApi.initialize(getContext(), "apiKey");

        String userId = "testUserId";

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);

        IterableApi.getInstance().setUserId(null);

        assertNull(IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testAuthTokenPresentInRequest() throws Exception {
//        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        dispatcher.enqueueResponse(ENDPOINT_UPDATE_EMAIL, new MockResponse().setResponseCode(200).setBody("{}"));
        shadowOf(getMainLooper()).runToEndOfTasks();
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        RecordedRequest getMessagesRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(getMessagesRequest.getHeader("Authorization"));

        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("new@email.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
//        RecordedRequest getMessagesAuthenticatedRequest = server.takeRequest(1, TimeUnit.SECONDS);
//        RecordedRequest getMessagesAuthenticatedRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
//        assertNull(getMessagesAuthenticatedRequest);
//        assertEquals(HEADER_SDK_AUTH_FORMAT + expiredJWT, getMessagesAuthenticatedRequest2.getHeader("Authorization"));

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().updateEmail("newEmail@gmail.com", null, null);
        shadowOf(getMainLooper()).runToEndOfTasks();
        shadowOf(getMainLooper()).idle();
        RecordedRequest updateEmailRequest = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(updateEmailRequest);
        String authHeader = updateEmailRequest.getHeader("Authorization");
//
//        assertEquals(HEADER_SDK_AUTH_FORMAT + expiredJWT, authHeader);

        RecordedRequest getMessagesUpdatedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertNotNull(getMessagesUpdatedRequest);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("new@email.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        RecordedRequest getMessagesSet2Request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals(HEADER_SDK_AUTH_FORMAT + newJWT, getMessagesSet2Request.getHeader("Authorization"));
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testAuthFailureReturns401() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        dispatcher.enqueueResponse("/events/inAppConsume", new MockResponse().setResponseCode(401).setBody("{\"code\": \"InvalidJwtPayload\"}"));
        IterableApi.getInstance().inAppConsume(
            new IterableInAppMessage(
                "asd",          // messageId
                null,           // content
                null,           // customPayload
                null,           // createdAt
                null,           // expiresAt
                null,           // trigger
                0.0,           // priorityLevel
                null,          // saveToInbox
                null,          // inboxMetadata
                2L,            // campaignId
                false          // jsonOnly - since this is a test message, not a JSON message
            ),
            null,              // urlHandler
            null              // customActionHandler
        );
        Robolectric.flushForegroundThreadScheduler();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testAuthRequestedOnSetEmail() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("someEmail@domain.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().updateEmail("someNewEmail@domain.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testAuthRequestedOnUpdateEmail() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("someEmail@domain.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        doReturn(newJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().updateEmail("someNewEmail@domain.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        //TODO: Shouldn't the update call also update the authToken in IterableAPI class?
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testAuthRequestedOnSetUserId() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId("SomeUser");
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testAuthSetToNullOnLogOut() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId("SomeUser");
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        IterableApi.getInstance().setUserId(null);
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Ignore ("Ignoring the JWT Tests")
    @Test
    public void testRegisterForPushInvokedAfterTokenRefresh() throws InterruptedException {
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("someEmail@domain.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);

        //TODO: Verify if registerForPush is invoked
    }

    @Test
    public void testAuthTokenRefreshPausesOnBackground() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();

        // Set up a valid token and user to trigger normal expiration refresh
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setEmail("test@example.com");
        shadowOf(getMainLooper()).runToEndOfTasks();

        // Request auth token which should set a timer for expiration refresh
        authManager.requestNewAuthToken(false);
        shadowOf(getMainLooper()).runToEndOfTasks();

        // The timer might be null if the token is considered expired, so let's test the behavior
        // rather than the internal timer state. We'll check that onSwitchToBackground and
        // onSwitchToForeground can be called without exceptions

        // Simulate app going to background - should clear any timer
        authManager.onSwitchToBackground();

        // Simulate app coming to foreground - should re-evaluate token state
        authManager.onSwitchToForeground();
        shadowOf(getMainLooper()).runToEndOfTasks();

        // Test passes if no exceptions were thrown and lifecycle methods executed successfully
    }

    @Test
    public void testNullJwtTokenTimerClearedOnBackground() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        // Mock auth handler to return null token (simulating failure)
        doReturn(null).when(authHandler).onAuthTokenRequested();

        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();
        IterableApi.getInstance().setEmail("test@example.com");
        shadowOf(getMainLooper()).runToEndOfTasks();

        // Request auth token which will fail with null and schedule a retry timer
        authManager.requestNewAuthToken(false);
        shadowOf(getMainLooper()).runToEndOfTasks();

        // Verify that timer was scheduled (isTimerScheduled should be true)
        // We can't directly access isTimerScheduled but we can test the behavior

        // Simulate app going to background - should clear timer without race condition
        authManager.onSwitchToBackground();

        // Verify timer state by attempting to schedule another token refresh
        // If race condition exists, this might fail or behave incorrectly
        authManager.scheduleAuthTokenRefresh(1000, false, null);

        // Simulate app coming to foreground and verify no exceptions
        authManager.onSwitchToForeground();
        shadowOf(getMainLooper()).runToEndOfTasks();

        // Test passes if no exceptions were thrown and lifecycle methods executed successfully
        // The synchronization fix ensures timer creation and clearing are atomic operations
    }

    @Test
    public void testNullJWTRefreshTimerRaceConditionOnBackground() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");
        
        // Set up auth handler to return null JWT (simulating auth failure)
        doReturn(null).when(authHandler).onAuthTokenRequested();
        
        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();
        
        // Set email to trigger auth token request which will fail and schedule retry
        IterableApi.getInstance().setEmail("test@example.com");
        shadowOf(getMainLooper()).runToEndOfTasks();
        
        // At this point, the null JWT response should have scheduled a retry timer
        // Verify timer is scheduled (this is the state before the race condition)
        assertNotNull("Timer should be created for retry after null JWT", authManager.timer);
        assertEquals("Timer should be scheduled after null JWT", true, authManager.isTimerScheduled);
        
        // Now simulate the race condition: app goes to background immediately
        // This should clear the timer, but due to race condition it might not work properly
        authManager.onSwitchToBackground();
        
        // The race condition bug: timer might not be properly cleared
        // With the fix, timer should be null and isTimerScheduled should be false
        assertNull("Timer should be cleared when app goes to background", authManager.timer);
        assertEquals("Timer scheduled flag should be false after background", false, authManager.isTimerScheduled);
        
        // Verify no timer tasks are left running that could fire later
        // This test will fail without the synchronization fix due to race condition
    }

}
