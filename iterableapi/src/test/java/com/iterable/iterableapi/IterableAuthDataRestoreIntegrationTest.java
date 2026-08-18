package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IterableAuthDataRestoreIntegrationTest extends BaseTest {
    private static final String EMAIL = "user@example.com";
    private static final String NEW_EMAIL = "new@example.com";
    private static final String VALID_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                    + "eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjI5MTYyMzkwMjJ9."
                    + "mYtgSqdUIxK8_RnYBTUP4cmpKw83aKi7cMiixF3qMB4";

    private IterableAuthHandler authHandler;
    private IterableKeychain keychain;
    private ScheduledExecutorService retryExecutor;
    private Queue<Runnable> scheduledTasks;

    @Before
    public void setUp() {
        IterableApi.sharedInstance = new IterableApi();
        authHandler = mock(IterableAuthHandler.class);
        keychain = mock(IterableKeychain.class);
        doReturn(EMAIL).when(keychain).getEmail();

        retryExecutor = mock(ScheduledExecutorService.class);
        scheduledTasks = new ArrayDeque<>();
        when(retryExecutor.schedule(
                any(Runnable.class),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)))
                .thenAnswer(invocation -> {
                    scheduledTasks.add(invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                });

        IterableApi.sharedInstance.keychain = keychain;
        IterableApi.sharedInstance.authDataRestoreExecutor = retryExecutor;
    }

    @Test
    public void repeatedTimeoutsNeverRequestANewTokenWithoutConfirmedAbsence() {
        doReturn(KeychainReadResult.TimedOut.INSTANCE)
                .when(keychain)
                .readAuthToken();

        initialize();
        runNext();
        runNext();

        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();
        assertEquals(IterableAuthManager.AuthState.RESTORING, authManager.getAuthState());
        assertFalse(authManager.isAuthTokenReady());
        verify(authHandler, never()).onAuthTokenRequested();

        authManager.onSwitchToForeground();

        assertEquals(1, scheduledTasks.size());
        verify(authHandler, never()).onAuthTokenRequested();
    }

    @Test
    public void timeoutThenSuccessRestoresTokenWithoutCallingClientHandler() {
        doReturn(
                KeychainReadResult.TimedOut.INSTANCE,
                new KeychainReadResult.Value(VALID_JWT))
                .when(keychain)
                .readAuthToken();

        initialize();
        runNext();

        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();
        assertEquals(EMAIL, IterableApi.getInstance().getEmail());
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
        assertTrue(authManager.isAuthTokenReady());
        assertEquals(
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                authManager.scheduledRefreshReason);
        verify(authHandler, never()).onAuthTokenRequested();
    }

    @Test
    public void confirmedMissingTokenSchedulesOneReasonedRefresh() {
        doReturn(new KeychainReadResult.Value(null))
                .when(keychain)
                .readAuthToken();

        initialize();

        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();
        assertEquals(IterableAuthManager.AuthState.RESTORING, authManager.getAuthState());
        assertFalse(authManager.isAuthTokenReady());
        assertEquals(
                IterableAuthRefreshReason.STORED_TOKEN_MISSING,
                authManager.scheduledRefreshReason);
        verify(authHandler, never()).onAuthTokenRequested();
    }

    @Test
    public void explicitTokenAfterConfirmedAbsenceUnblocksAuth() {
        doReturn(new KeychainReadResult.Value(null))
                .when(keychain)
                .readAuthToken();

        initialize();
        IterableApi.getInstance().setEmail(EMAIL, VALID_JWT);

        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
        assertTrue(authManager.isAuthTokenReady());
        assertEquals(
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                authManager.scheduledRefreshReason);
        verify(authHandler, never()).onAuthTokenRequested();
    }

    @Test
    public void explicitLoginWinsOverAQueuedRestoreRetry() {
        doReturn(
                KeychainReadResult.TimedOut.INSTANCE,
                new KeychainReadResult.Value("old-token"))
                .when(keychain)
                .readAuthToken();

        initialize();
        IterableApi.getInstance().setEmail(NEW_EMAIL, VALID_JWT);
        runNext();

        assertEquals(NEW_EMAIL, IterableApi.getInstance().getEmail());
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
    }

    private void initialize() {
        IterableApi.initialize(
                getContext(),
                "apiKey",
                new IterableConfig.Builder()
                        .setAutoPushRegistration(false)
                        .setAuthHandler(authHandler)
                        .build());
    }

    private void runNext() {
        scheduledTasks.remove().run();
    }
}
