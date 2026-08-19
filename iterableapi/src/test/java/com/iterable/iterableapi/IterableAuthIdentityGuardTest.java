package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Auth results arrive asynchronously from a developer callback and from encrypted storage. Both can
 * outlive the identity that asked for them, and storing a superseded result puts one user's token on
 * another user's session.
 */
public class IterableAuthIdentityGuardTest extends BaseTest {
    private static final String JWT_HEADER_AND_PAYLOAD =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                    + "eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjI5MTYyMzkwMjJ9.";
    // Only the payload segment is parsed, so a distinct signature is enough to make a distinct token.
    private static final String TOKEN_A = JWT_HEADER_AND_PAYLOAD + "signature-for-user-a";
    private static final String TOKEN_B = JWT_HEADER_AND_PAYLOAD + "signature-for-user-b";

    private IterableApi api;
    private IterableAuthHandler authHandler;
    private IterableAuthManager authManager;
    private IterableKeychain keychain;
    private ScheduledExecutorService retryExecutor;
    private Queue<Runnable> restoreRetries;
    private List<Runnable> submittedAuthWork;

    @Before
    public void setUp() {
        api = mock(IterableApi.class);
        when(api.getEmail()).thenReturn("user-a@example.com");
        authHandler = mock(IterableAuthHandler.class);

        authManager = new IterableAuthManager(
                api,
                authHandler,
                new RetryPolicy(3, 1, RetryPolicy.Type.LINEAR),
                60_000
        );
        authManager.timer = new RetainingTimer();

        submittedAuthWork = new ArrayList<>();
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.submit(any(Runnable.class))).thenAnswer(
                invocation -> {
                    submittedAuthWork.add(invocation.getArgument(0));
                    return mock(Future.class);
                }
        );
        authManager.executor = executor;

        keychain = mock(IterableKeychain.class);
        retryExecutor = mock(ScheduledExecutorService.class);
        restoreRetries = new ArrayDeque<>();
        when(
                retryExecutor.schedule(
                        any(Runnable.class),
                        anyLong(),
                        eq(TimeUnit.MILLISECONDS)
                )
        ).thenAnswer(
                invocation -> {
                    restoreRetries.add(invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                }
        );
    }

    @After
    public void tearDown() {
        authManager.clearRefreshTimer();
    }

    @Test
    public void anOrphanedRestorerCannotStoreItsTokenAfterANewRestoreBegins() {
        doReturn(KeychainReadResult.TimedOut.INSTANCE).when(keychain).readAuthToken();
        authManager.restoreAuthToken(keychain, retryExecutor);
        assertEquals(1, restoreRetries.size());

        doReturn(new KeychainReadResult.Value(TOKEN_B)).when(keychain).readAuthToken();
        authManager.restoreAuthToken(keychain, retryExecutor);
        verify(api).setRestoredAuthToken(TOKEN_B);

        doReturn(new KeychainReadResult.Value(TOKEN_A)).when(keychain).readAuthToken();
        restoreRetries.remove().run();

        verify(api, never()).setRestoredAuthToken(TOKEN_A);
    }

    /**
     * {@code onAuthTokenRequested} is a blocking developer callback, so the user can change while it
     * runs. The token it returns belongs to the user who was signed in when it was called.
     */
    @Test
    public void aHandlerResultIsDiscardedWhenTheIdentityChangesWhileTheHandlerRuns() {
        when(authHandler.onAuthTokenRequested()).thenAnswer(invocation -> {
            authManager.resetForIdentityChange();
            when(api.getEmail()).thenReturn("user-b@example.com");
            return TOKEN_A;
        });
        authManager.requestNewAuthToken(false, null);
        assertEquals(1, submittedAuthWork.size());

        submittedAuthWork.get(0).run();

        verify(api, never()).setAuthToken(TOKEN_A);
        verify(authHandler, never()).onTokenRegistrationSuccessful(anyString());
    }

    /**
     * A request that arrives while this one is in flight is deferred rather than started, and only
     * a stored result replays it. Discarding must replay it too, or the new identity is left with no
     * token and nothing scheduled.
     */
    @Test
    public void discardingAStaleResultStillHonoursTheRefreshDeferredBehindIt() {
        when(authHandler.onAuthTokenRequested()).thenAnswer(invocation -> {
            authManager.requestNewAuthToken(false, null);
            authManager.resetForIdentityChange();
            when(api.getEmail()).thenReturn("user-b@example.com");
            return TOKEN_A;
        });
        authManager.requestNewAuthToken(false, null);

        submittedAuthWork.get(0).run();

        verify(api, never()).setAuthToken(TOKEN_A);
        assertNotNull(
                "the deferred refresh must be armed for the identity that replaced this one",
                authManager.scheduledRefreshTask
        );
    }

    /**
     * The mirror of the test above: an identity change before the handler is called must not discard
     * anything, because {@code pendingAuth} makes the new login reuse this request rather than start
     * its own — discarding here would leave the new user with no token and nothing in flight.
     */
    @Test
    public void anIdentityChangeBeforeTheHandlerRunsIsServedByTheRequestInFlight() {
        when(authHandler.onAuthTokenRequested()).thenReturn(TOKEN_A);
        authManager.requestNewAuthToken(false, null);

        authManager.resetForIdentityChange();
        when(api.getEmail()).thenReturn("user-b@example.com");
        submittedAuthWork.get(0).run();

        verify(api).setAuthToken(TOKEN_A);
    }

    /**
     * A discarded result must still release {@code pendingAuth}, or every later request is refused
     * for the life of the process. The throwing handler matters: on the success path the flag is
     * already cleared before the discard, so only the failure path can leak it.
     */
    @Test
    public void discardingAStaleFailureLeavesAuthAbleToRequestAgain() {
        when(authHandler.onAuthTokenRequested()).thenAnswer(invocation -> {
            authManager.resetForIdentityChange();
            throw new RuntimeException("boom");
        });
        authManager.requestNewAuthToken(false, null);
        submittedAuthWork.get(0).run();

        authManager.requestNewAuthToken(false, null);

        assertEquals(2, submittedAuthWork.size());
    }

    /**
     * An orphaned restorer completing must not clear the field out from under the restore that
     * replaced it — doing so loses the foreground retry that recovers an unresolved read.
     */
    @Test
    public void anOrphanedRestorerDoesNotEvictTheRestoreThatReplacedIt() {
        doReturn(KeychainReadResult.TimedOut.INSTANCE).when(keychain).readAuthToken();
        authManager.restoreAuthToken(keychain, retryExecutor);
        authManager.restoreAuthToken(keychain, retryExecutor);
        assertEquals(2, restoreRetries.size());

        doReturn(new KeychainReadResult.Value(TOKEN_A)).when(keychain).readAuthToken();
        restoreRetries.remove().run();

        authManager.authRetryPolicy = new RetryPolicy(3, 60_000, RetryPolicy.Type.LINEAR);
        authManager.onSwitchToForeground();

        assertNull(authManager.scheduledRefreshTask);
    }

    @Test
    public void aHandlerFailureForASupersededIdentityIsNotReported() {
        when(authHandler.onAuthTokenRequested()).thenAnswer(invocation -> {
            authManager.resetForIdentityChange();
            throw new RuntimeException("boom");
        });
        authManager.requestNewAuthToken(false, null);

        submittedAuthWork.get(0).run();

        verify(authHandler, never()).onAuthFailure(any(AuthFailure.class));
    }

    @Test
    public void anInFlightHandlerResultStillLandsWhenTheIdentityIsUnchanged() {
        when(authHandler.onAuthTokenRequested()).thenReturn(TOKEN_A);
        authManager.requestNewAuthToken(false, null);

        submittedAuthWork.get(0).run();

        verify(api).setAuthToken(TOKEN_A);
        verify(authHandler).onTokenRegistrationSuccessful(TOKEN_A);
    }

    @Test
    public void repeatingAnEqualExplicitTokenDoesNotRearmOrUnblockAnInvalidToken() {
        when(api.getAuthToken()).thenReturn(TOKEN_A);
        authManager.setAuthTokenInvalid();

        authManager.useExplicitAuthToken(new String(TOKEN_A));

        assertEquals(IterableAuthManager.AuthState.INVALID, authManager.getAuthState());
        assertNull(authManager.scheduledRefreshTask);
    }

    @Test
    public void aGenuinelyNewExplicitTokenStillArmsARefresh() {
        when(api.getAuthToken()).thenReturn(TOKEN_A);
        authManager.setAuthTokenInvalid();

        authManager.useExplicitAuthToken(TOKEN_B);

        assertEquals(IterableAuthManager.AuthState.UNKNOWN, authManager.getAuthState());
        assertNotNull(authManager.scheduledRefreshTask);
    }

    @Test
    public void restoringWithoutAnIdentityDoesNotBlockWork() {
        when(api.getEmail()).thenReturn(null);
        when(api.getUserId()).thenReturn(null);

        authManager.resetForIdentityChange();

        assertEquals(IterableAuthManager.AuthState.RESTORING, authManager.getAuthState());
        assertTrue(authManager.isAuthTokenReady());
    }

    @Test
    public void restoringWithAnIdentityStillBlocksWork() {
        when(api.getEmail()).thenReturn("user-b@example.com");

        authManager.resetForIdentityChange();

        assertFalse(authManager.isAuthTokenReady());
    }

    private static class RetainingTimer extends Timer {
        RetainingTimer() {
            super(true);
            super.cancel();
        }

        @Override
        public void schedule(TimerTask task, long delay) {
            // Retain nothing; tests only assert on the manager's ownership field.
        }

        @Override
        public void cancel() {
            // Keep the timer usable across a clearRefreshTimer() call.
        }
    }
}
