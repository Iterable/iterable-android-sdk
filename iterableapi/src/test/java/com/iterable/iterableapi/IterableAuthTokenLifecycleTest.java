package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.PathBasedQueueDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.LooperMode;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.PAUSED;

/**
 * Covers how the JWT auth token is created, replaced and recovered as the refresh timer,
 * foreground/background transitions and login/logout drive {@link IterableAuthManager}.
 */
@LooperMode(PAUSED)
public class IterableAuthTokenLifecycleTest extends BaseTest {

    /** exp = 2062. */
    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjI5MTYyMzkwMjJ9.mYtgSqdUIxK8_RnYBTUP4cmpKw83aKi7cMiixF3qMB4";
    /** exp = 2030. */
    private static final String NEW_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE5MTYyMzkwMjJ9.dMD3MLuHTiO-Qy9PvOoMchNM4CzFIgI7jKVrRtlqlM0";
    /** exp = 2018, already expired. */
    private static final String EXPIRED_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyNDkwMjJ9.6Yc3QcBGwCdV1sdKmgOtw4D69P_HUoVqEW3YMuEgH8c";

    private static final String EMAIL = "user@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    private MockWebServer server;
    private IterableAuthHandler authHandler;
    private IterableAuthManager authManager;
    private ManualExecutor executor;

    @Before
    public void setUp() {
        server = new MockWebServer();
        server.setDispatcher(new PathBasedQueueDispatcher());
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        IterableApi.sharedInstance = new IterableApi();
        authHandler = mock(IterableAuthHandler.class);
        doReturn(VALID_JWT).when(authHandler).onAuthTokenRequested();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setAuthHandler(authHandler)
                .build());

        authManager = IterableApi.getInstance().getAuthManager();
        executor = new ManualExecutor();
        authManager.executor = executor;
    }

    @After
    public void tearDown() throws IOException {
        executor.shutdownNow();
        server.shutdown();
        server = null;
    }

    // region token creation

    @Test
    public void loginRequestsTokenFromHandlerAndStoresIt() {
        login();

        verify(authHandler).onAuthTokenRequested();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void loginWithSuppliedTokenDoesNotAskTheHandler() {
        IterableApi.getInstance().setEmail(EMAIL, VALID_JWT);
        settle();

        verify(authHandler, never()).onAuthTokenRequested();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void handlerReturningNullLeavesNoTokenStored() {
        doReturn(null).when(authHandler).onAuthTokenRequested();

        login();

        assertNull(IterableApi.getInstance().getAuthToken());
        verify(authHandler).onAuthFailure(failureWithReason(AuthFailureReason.AUTH_TOKEN_NULL));
    }

    @Test
    public void handlerThrowingLeavesNoTokenStored() {
        doThrow(new RuntimeException("backend down")).when(authHandler).onAuthTokenRequested();

        login();

        assertNull(IterableApi.getInstance().getAuthToken());
        verify(authHandler).onAuthFailure(failureWithReason(AuthFailureReason.AUTH_TOKEN_GENERATION_ERROR));
    }

    // endregion

    // region token replacement

    @Test
    public void switchingUserReplacesTheStoredToken() {
        login();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());

        doReturn(NEW_JWT).when(authHandler).onAuthTokenRequested();
        loginAs(OTHER_EMAIL);

        assertEquals(NEW_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void reLoggingInAsTheSameUserKeepsTheStoredToken() {
        login();
        clearInvocations(authHandler);

        doReturn(NEW_JWT).when(authHandler).onAuthTokenRequested();
        login();

        verify(authHandler, never()).onAuthTokenRequested();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void failedRefreshKeepsThePreviousToken() {
        login();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());

        doThrow(new RuntimeException("backend down")).when(authHandler).onAuthTokenRequested();
        authManager.requestNewAuthToken(false, null);
        settle();

        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void logoutClearsTheToken() {
        login();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());

        IterableApi.getInstance().setEmail(null);
        settle();

        assertNull(IterableApi.getInstance().getAuthToken());
        assertNull(IterableApi.getInstance().getEmail());
    }

    // endregion

    // region recovery after failure

    @Test
    public void refreshAfterAFailureRecoversTheToken() {
        doThrow(new RuntimeException("backend down")).when(authHandler).onAuthTokenRequested();
        login();
        assertNull(IterableApi.getInstance().getAuthToken());

        doReturn(NEW_JWT).when(authHandler).onAuthTokenRequested();
        authManager.requestNewAuthToken(false, null);
        settle();

        assertEquals(NEW_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void expiredTokenIsStoredAndRefreshIsRescheduled() {
        doReturn(EXPIRED_JWT).when(authHandler).onAuthTokenRequested();

        login();

        assertEquals(EXPIRED_JWT, IterableApi.getInstance().getAuthToken());
        assertTrue("an expired token must leave a refresh armed", isRefreshScheduled());
    }

    @Test
    public void malformedTokenReportsPayloadInvalidAndKeepsRefreshing() {
        doReturn("not.a.jwt").when(authHandler).onAuthTokenRequested();

        login();

        verify(authHandler).onAuthFailure(failureWithReason(AuthFailureReason.AUTH_TOKEN_PAYLOAD_INVALID));
        assertTrue(isRefreshScheduled());
    }

    // endregion

    // region foreground / background

    @Test
    public void foregroundWithAValidTokenDoesNotRequestANewOne() {
        login();
        clearInvocations(authHandler);

        authManager.onSwitchToBackground();
        authManager.onSwitchToForeground();
        settle();

        verify(authHandler, never()).onAuthTokenRequested();
        assertEquals(VALID_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void backgroundCancelsTheScheduledRefresh() {
        login();
        assertTrue(isRefreshScheduled());

        authManager.onSwitchToBackground();

        assertNull("backgrounding must cancel the refresh timer", authManager.timer);
        assertNull(
                "backgrounding must release refresh ownership",
                authManager.scheduledRefreshTask);
    }

    @Test
    public void repeatedForegroundingDoesNotAmplifyTokenRequests() {
        login();
        clearInvocations(authHandler);

        for (int i = 0; i < 5; i++) {
            authManager.onSwitchToForeground();
        }
        settle();

        verify(authHandler, never()).onAuthTokenRequested();
        assertTrue("foregrounding must leave exactly one refresh armed", isRefreshScheduled());
    }

    @Test
    public void tokenRequestIsSkippedWhileBackgrounded() {
        login();
        clearInvocations(authHandler);

        authManager.onSwitchToBackground();
        authManager.requestNewAuthToken(false, null);
        settle();

        verify(authHandler, never()).onAuthTokenRequested();
    }

    // endregion

    // region concurrent actors

    @Test
    public void concurrentSchedulingArmsOnlyOneRefresh() throws Exception {
        CountingTimer timer = installCountingTimer();

        runConcurrently(
                8,
                () -> authManager.scheduleAuthTokenRefresh(
                        60_000,
                        IterableAuthRefreshReason.TOKEN_EXPIRING,
                        null));

        assertEquals(1, timer.liveTaskCount());
    }

    @Test
    public void aFiringRefreshRequestsOneTokenAndRearmsOnce() {
        login();
        clearInvocations(authHandler);

        CountingTimer timer = armObservableRefresh();
        timer.fireAll();
        settle();

        verify(authHandler, times(1)).onAuthTokenRequested();
        assertTrue("the refreshed token must leave a new refresh armed", isRefreshScheduled());
    }

    @Test
    public void concurrentTokenRequestsCallTheHandlerOnce() throws Exception {
        login();
        clearInvocations(authHandler);

        runConcurrently(8, () -> authManager.requestNewAuthToken(false, null));
        settle();

        verify(authHandler, times(1)).onAuthTokenRequested();
    }

    @Test
    public void loginDuringAnInFlightRequestReusesThatRequest() {
        authManager.requestNewAuthToken(false, null);

        doReturn(NEW_JWT).when(authHandler).onAuthTokenRequested();
        login();

        verify(authHandler, times(1)).onAuthTokenRequested();
        assertEquals(NEW_JWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void tokenArrivingAfterLogoutIsNotStored() {
        login();

        authManager.requestNewAuthToken(false, null);
        IterableApi.getInstance().setEmail(null);
        settle();

        assertNull("a token resolved after logout must not restore the session",
                IterableApi.getInstance().getAuthToken());
    }

    // endregion

    private void login() {
        loginAs(EMAIL);
    }

    private void loginAs(String email) {
        IterableApi.getInstance().setEmail(email);
        settle();
    }

    /** Drains the auth executor and the main looper until both are idle. */
    private void settle() {
        for (int i = 0; i < 10; i++) {
            boolean ranTask = executor.runAll() > 0;
            shadowOf(getMainLooper()).runToEndOfTasks();
            if (!ranTask && !executor.hasPendingTasks()) {
                return;
            }
        }
    }

    private boolean isRefreshScheduled() {
        return authManager.scheduledRefreshTask != null;
    }

    private CountingTimer installCountingTimer() {
        CountingTimer timer = new CountingTimer();
        authManager.timer = timer;
        return timer;
    }

    /** Discards whatever refresh is already armed and arms one the test can fire on demand. */
    private CountingTimer armObservableRefresh() {
        authManager.clearRefreshTimer();
        CountingTimer timer = installCountingTimer();
        authManager.scheduleAuthTokenRefresh(
                60_000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                null);
        assertEquals(1, timer.liveTaskCount());
        return timer;
    }

    private void runConcurrently(int threadCount, Runnable action) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    action.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
    }

    private static AuthFailure failureWithReason(AuthFailureReason reason) {
        return org.mockito.ArgumentMatchers.argThat(failure -> failure != null && failure.failureReason == reason);
    }

    /** Records scheduled tasks instead of running them, so a test can count and fire them. */
    private static class CountingTimer extends Timer {
        private final List<TimerTask> tasks = new CopyOnWriteArrayList<>();

        CountingTimer() {
            super(true);
            super.cancel();
        }

        @Override
        public void schedule(TimerTask task, long delay) {
            tasks.add(task);
        }

        @Override
        public void cancel() {
            tasks.clear();
        }

        int liveTaskCount() {
            return tasks.size();
        }

        void fireAll() {
            for (TimerTask task : tasks) {
                task.run();
            }
        }
    }

    /**
     * Executor that queues submitted work until the test drains it. Unlike Robolectric's
     * InlineExecutorService this never runs the task inside submit(), which would deadlock:
     * requestNewAuthToken submits while holding the auth manager's monitor, and the task
     * re-enters that monitor via queueExpirationRefresh.
     */
    private static class ManualExecutor extends java.util.concurrent.AbstractExecutorService {
        private final java.util.Queue<Runnable> pending = new java.util.concurrent.ConcurrentLinkedQueue<>();
        private volatile boolean shutdown;

        @Override
        public void execute(Runnable command) {
            if (!shutdown) {
                pending.add(command);
            }
        }

        int runAll() {
            int count = 0;
            Runnable task;
            while ((task = pending.poll()) != null) {
                task.run();
                count++;
            }
            return count;
        }

        boolean hasPendingTasks() {
            return !pending.isEmpty();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            pending.clear();
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && pending.isEmpty();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }
    }
}
