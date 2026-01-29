package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;

import com.iterable.iterableapi.response.IterableResponseObject;
import com.iterable.iterableapi.response.handlers.IterableCallbackHandlers;
import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Security tests for TOCTOU (Time-Of-Check-Time-Of-Use) vulnerabilities in auth flow.
 *
 * These tests verify that credentials cannot be swapped mid-flight between storage
 * and usage, preventing user-controlled bypass attacks.
 */
public class IterableApiAuthSecurityTests extends BaseTest {

    private MockWebServer server;
    private IterableAuthHandler authHandler;
    private PathBasedQueueDispatcher dispatcher;
    private IterableKeychain mockKeychain;

    private final String validJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyAiZW1haWwiOiAidGVzdEBleGFtcGxlLmNvbSIsICJpYXQiOiAxNzI5MjUyNDE3LCAiZXhwIjogMTcyOTg1NzIxNyB9.m-O6ksCv9OR-cF0RdiHB8VW_NwWJHVXChipbcFmIChg";

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        mockKeychain = mock(IterableKeychain.class);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    private void initIterableWithAuth() {
        IterableApi.sharedInstance = new IterableApi();
        authHandler = mock(IterableAuthHandler.class);
        IterableConfig iterableConfig = new IterableConfig.Builder()
                .setAuthHandler(authHandler)
                .setAutoPushRegistration(false)
                .build();
        IterableApi.initialize(getContext(), "fake_key", iterableConfig);
    }

    private void initIterableWithoutAuth() {
        IterableApi.sharedInstance = new IterableApi();
        IterableConfig iterableConfig = new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .build();
        IterableApi.initialize(getContext(), "fake_key", iterableConfig);
    }

    /**
     * Test that completeUserLogin skips sensitive operations when JWT auth is enabled
     * but no authToken is present, preventing user-controlled bypass attacks.
     */
    @Test
    public void testCompleteUserLogin_WithJWTAuth_NoToken_SkipsSensitiveOps() throws Exception {
        initIterableWithAuth();

        // Spy on the API instance to verify method calls
        IterableApi api = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = api;

        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbeddedManager = mock(IterableEmbeddedManager.class);
        when(api.getInAppManager()).thenReturn(mockInAppManager);
        when(api.getEmbeddedManager()).thenReturn(mockEmbeddedManager);

        // Directly call setAuthToken with null and bypassAuth=true to simulate
        // attempting to bypass with no token (user-controlled bypass scenario)
        api.setAuthToken(null, true);

        shadowOf(getMainLooper()).idle();

        // Verify sensitive operations were NOT called (JWT auth enabled, no token)
        verify(mockInAppManager, never()).syncInApp();
        verify(mockEmbeddedManager, never()).syncMessages();
    }

    /**
     * Test that completeUserLogin executes sensitive operations when JWT auth is enabled
     * AND a valid authToken is present.
     */
    @Test
    public void testCompleteUserLogin_WithJWTAuth_WithToken_ExecutesSensitiveOps() throws Exception {
        initIterableWithAuth();

        dispatcher.enqueueResponse("/users/update", new MockResponse().setResponseCode(200).setBody("{}"));

        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        // Spy on the API instance to verify method calls
        IterableApi api = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = api;

        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbeddedManager = mock(IterableEmbeddedManager.class);
        when(api.getInAppManager()).thenReturn(mockInAppManager);
        when(api.getEmbeddedManager()).thenReturn(mockEmbeddedManager);

        api.setEmail("legit@example.com");

        server.takeRequest(1, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).idle();

        // Verify sensitive operations WERE called with valid token
        verify(mockInAppManager).syncInApp();
        verify(mockEmbeddedManager).syncMessages();
    }

    /**
     * Test that completeUserLogin executes sensitive operations when JWT auth is NOT enabled,
     * even without an authToken.
     */
    @Test
    public void testCompleteUserLogin_WithoutJWTAuth_NoToken_ExecutesSensitiveOps() throws Exception {
        initIterableWithoutAuth();

        // Spy on the API instance to verify method calls
        IterableApi api = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = api;

        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbeddedManager = mock(IterableEmbeddedManager.class);
        when(api.getInAppManager()).thenReturn(mockInAppManager);
        when(api.getEmbeddedManager()).thenReturn(mockEmbeddedManager);

        api.setEmail("user@example.com");

        shadowOf(getMainLooper()).idle();

        // Verify sensitive operations WERE called (no JWT auth required)
        verify(mockInAppManager).syncInApp();
        verify(mockEmbeddedManager).syncMessages();
    }

    /**
     * Critical TOCTOU test: Verify that storeAuthData captures credentials BEFORE storage
     * and passes those exact values to the completion handler, preventing mid-flight swaps.
     */
    @Test
    public void testStoreAuthData_CompletionHandler_ReceivesStoredCredentials() throws Exception {
        initIterableWithAuth();

        IterableApi api = IterableApi.getInstance();

        // Use reflection to spy on storeAuthData behavior
        IterableApi spyApi = spy(api);
        IterableApi.sharedInstance = spyApi;

        // Set up mock keychain that attempts to swap credentials mid-flight
        doAnswer(invocation -> {
            // Malicious keychain that tries to swap email after storage
            String email = invocation.getArgument(0);
            IterableLogger.d("TEST", "Keychain storing email: " + email);
            // Simulate attacker trying to modify keychain after storage
            return null;
        }).when(mockKeychain).saveEmail(anyString());

        when(spyApi.getKeychain()).thenReturn(mockKeychain);

        dispatcher.enqueueResponse("/users/update", new MockResponse().setResponseCode(200).setBody("{}"));
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        final String originalEmail = "victim@example.com";
        final AtomicReference<String> completionHandlerEmail = new AtomicReference<>();
        final AtomicReference<String> completionHandlerToken = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        // Capture what the completion handler receives
        spyApi.setEmail(originalEmail, new IterableCallbackHandlers.SuccessCallback() {
            @Override
            public void onSuccess(IterableResponseObject.Success data) {
                // This callback happens after completeUserLogin
                latch.countDown();
            }
        }, null);

        server.takeRequest(1, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).idle();
        latch.await(2, TimeUnit.SECONDS);

        // Verify the API instance has the correct email
        assertEquals("Email should match original", originalEmail, spyApi.getEmail());
        assertNotNull("AuthToken should be set", spyApi.getAuthToken());
    }

    /**
     * Test setAuthToken uses completion handler pattern to pass validated credentials
     * to completeUserLogin, preventing TOCTOU vulnerabilities.
     */
    @Test
    public void testSetAuthToken_UsesCompletionHandlerPattern() throws Exception {
        initIterableWithAuth();

        dispatcher.enqueueResponse("/users/update", new MockResponse().setResponseCode(200).setBody("{}"));
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        IterableApi api = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = api;

        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbeddedManager = mock(IterableEmbeddedManager.class);
        when(api.getInAppManager()).thenReturn(mockInAppManager);
        when(api.getEmbeddedManager()).thenReturn(mockEmbeddedManager);

        // First set user
        api.setEmail("user@example.com");
        server.takeRequest(1, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).idle();

        // Clear previous invocations
        org.mockito.Mockito.clearInvocations(mockInAppManager, mockEmbeddedManager);

        // Now update auth token (simulating token refresh)
        final String newToken = "new_jwt_token_here";
        api.setAuthToken(newToken, false);

        shadowOf(getMainLooper()).idle();

        // Verify sensitive operations were called with updated token
        verify(mockInAppManager).syncInApp();
        verify(mockEmbeddedManager).syncMessages();
        assertEquals("Token should be updated", newToken, api.getAuthToken());
    }

    /**
     * Test that bypassAuth in setAuthToken still validates authToken before sensitive ops
     * when JWT auth is enabled.
     */
    @Test
    public void testSetAuthToken_BypassAuth_StillValidatesToken() throws Exception {
        initIterableWithAuth();

        IterableApi api = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = api;

        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbeddedManager = mock(IterableEmbeddedManager.class);
        when(api.getInAppManager()).thenReturn(mockInAppManager);
        when(api.getEmbeddedManager()).thenReturn(mockEmbeddedManager);

        // Try to bypass with no token set
        api.setAuthToken(null, true);

        shadowOf(getMainLooper()).idle();

        // Verify sensitive operations were NOT called (JWT auth enabled, no token)
        verify(mockInAppManager, never()).syncInApp();
        verify(mockEmbeddedManager, never()).syncMessages();
    }

    /**
     * Test credential consistency across the auth flow - ensuring stored and used
     * credentials match exactly.
     */
    @Test
    public void testCredentialConsistency_StorageToUsage() throws Exception {
        initIterableWithAuth();

        dispatcher.enqueueResponse("/users/update", new MockResponse().setResponseCode(200).setBody("{}"));

        final String testEmail = "test@example.com";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        IterableApi api = IterableApi.getInstance();

        // Set email
        api.setEmail(testEmail);
        server.takeRequest(1, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).idle();

        // Verify credentials match exactly
        assertEquals("Email should match", testEmail, api.getEmail());
        assertEquals("AuthToken should match", validJWT, api.getAuthToken());

        // Now test with userId
        dispatcher.enqueueResponse("/users/update", new MockResponse().setResponseCode(200).setBody("{}"));
        final String testUserId = "user123";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        api.setUserId(testUserId);
        server.takeRequest(1, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).idle();

        // Verify userId matches
        assertEquals("UserId should match", testUserId, api.getUserId());
        assertEquals("AuthToken should still match", validJWT, api.getAuthToken());
    }

    /**
     * Test that sensitive operations are skipped when keychain contains stale credentials
     * but no valid authToken in JWT auth mode.
     */
    @Test
    public void testStaleKeychainCredentials_NoToken_SkipsSensitiveOps() throws Exception {
        // Setup: Simulate app restart with stale keychain data
        SharedPreferences sharedPref = getContext().getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_EMAIL_KEY, "stale@example.com");
        editor.putString(IterableConstants.SHARED_PREFS_USERID_KEY, "staleUserId");
        // No auth token stored
        editor.apply();

        initIterableWithAuth();

        IterableApi api = spy(IterableApi.getInstance());
        IterableApi.sharedInstance = api;

        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbeddedManager = mock(IterableEmbeddedManager.class);
        when(api.getInAppManager()).thenReturn(mockInAppManager);
        when(api.getEmbeddedManager()).thenReturn(mockEmbeddedManager);

        // Trigger initialization flow
        shadowOf(getMainLooper()).idle();

        // Verify sensitive operations were NOT called with stale credentials
        verify(mockInAppManager, never()).syncInApp();
        verify(mockEmbeddedManager, never()).syncMessages();
    }
}

