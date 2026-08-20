package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IterableAuthRefreshOwnershipTest extends BaseTest {
    private IterableApi api;
    private RecordingAuthManager authManager;

    @Before
    public void setUp() {
        api = mock(IterableApi.class);
        when(api.getEmail()).thenReturn("user@example.com");
        authManager = new RecordingAuthManager(api);
        when(api.getAuthManager()).thenReturn(authManager);
    }

    @After
    public void tearDown() {
        authManager.clearRefreshTimer();
    }

    @Test
    public void concurrentSchedulingCreatesOneRefreshTask() throws Exception {
        RetainingTimer timer = new RetainingTimer();
        authManager.timer = timer;
        int threadCount = 8;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService callers = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(
                        callers.submit(
                                () -> {
                                    barrier.await();
                                    authManager.scheduleAuthTokenRefresh(
                                            60_000,
                                            IterableAuthRefreshReason.TOKEN_EXPIRING,
                                            null
                                    );
                                    return null;
                                }
                        )
                );
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            callers.shutdownNow();
        }

        assertEquals(1, timer.taskCount());
        assertSame(timer.lastTask(), authManager.scheduledRefreshTask);
        assertEquals(
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                authManager.scheduledRefreshReason
        );
    }

    @Test
    public void staleTaskCannotRunOrClearItsReplacement() {
        RetainingTimer firstTimer = new RetainingTimer();
        authManager.timer = firstTimer;
        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.AUTH_HANDLER_RETRY,
                null
        );
        TimerTask staleTask = firstTimer.lastTask();

        authManager.clearRefreshTimer();
        RetainingTimer replacementTimer = new RetainingTimer();
        authManager.timer = replacementTimer;
        authManager.scheduleAuthTokenRefresh(
                2000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                null
        );
        TimerTask replacementTask = replacementTimer.lastTask();

        staleTask.run();

        assertSame(replacementTask, authManager.scheduledRefreshTask);
        assertEquals(0, authManager.requestCount.get());

        replacementTask.run();

        assertNull(authManager.scheduledRefreshTask);
        assertNull(authManager.scheduledRefreshReason);
        assertEquals(1, authManager.requestCount.get());
    }

    @Test
    public void duplicateScheduleKeepsOriginalCallbackAndPolicy() {
        RetainingTimer timer = new RetainingTimer();
        authManager.timer = timer;
        IterableHelper.SuccessHandler firstCallback =
                mock(IterableHelper.SuccessHandler.class);
        IterableHelper.SuccessHandler secondCallback =
                mock(IterableHelper.SuccessHandler.class);

        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.JWT_401,
                firstCallback
        );
        authManager.scheduleAuthTokenRefresh(
                2000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                secondCallback
        );

        timer.lastTask().run();

        assertEquals(1, timer.taskCount());
        assertSame(firstCallback, authManager.lastSuccessCallback);
        assertFalse(authManager.lastIgnoreRetryPolicy);
    }

    @Test
    public void schedulingFailureReleasesOwnership() {
        authManager.timer = new FailingTimer();

        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.JWT_401,
                null
        );

        assertNull(authManager.timer);
        assertNull(authManager.scheduledRefreshTask);
        assertNull(authManager.scheduledRefreshReason);

        RetainingTimer replacementTimer = new RetainingTimer();
        authManager.timer = replacementTimer;
        authManager.scheduleAuthTokenRefresh(
                2000,
                IterableAuthRefreshReason.JWT_401,
                null
        );

        assertEquals(1, replacementTimer.taskCount());
    }

    @Test
    public void scheduledLifecycleRefreshStillIgnoresPausedRetries() {
        RetainingTimer timer = new RetainingTimer();
        authManager.timer = timer;
        authManager.pauseAuthRetries(true);

        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.JWT_401,
                null
        );
        assertEquals(0, timer.taskCount());

        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                null
        );

        assertEquals(1, timer.taskCount());
        assertTrue(
                IterableAuthRefreshReason.TOKEN_EXPIRING.ignoresRetryPolicy()
        );
    }

    private static class RecordingAuthManager extends IterableAuthManager {
        private final AtomicInteger requestCount = new AtomicInteger();
        private IterableHelper.SuccessHandler lastSuccessCallback;
        private boolean lastIgnoreRetryPolicy;

        RecordingAuthManager(IterableApi api) {
            super(
                    api,
                    mock(IterableAuthHandler.class),
                    new RetryPolicy(3, 1, RetryPolicy.Type.LINEAR),
                    60_000
            );
        }

        @Override
        public synchronized void requestNewAuthToken(
                boolean hasFailedPriorAuth,
                IterableHelper.SuccessHandler successCallback,
                boolean shouldIgnoreRetryPolicy
        ) {
            requestCount.incrementAndGet();
            lastSuccessCallback = successCallback;
            lastIgnoreRetryPolicy = shouldIgnoreRetryPolicy;
        }
    }

    private static class RetainingTimer extends Timer {
        private final List<TimerTask> tasks =
                Collections.synchronizedList(new ArrayList<>());

        RetainingTimer() {
            super(true);
            super.cancel();
        }

        @Override
        public void schedule(TimerTask task, long delay) {
            tasks.add(task);
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void cancel() {
            // Keep tasks available so stale-task behavior can be tested deterministically.
        }

        TimerTask lastTask() {
            return tasks.get(tasks.size() - 1);
        }

        int taskCount() {
            return tasks.size();
        }
    }

    private static class FailingTimer extends Timer {
        FailingTimer() {
            super(true);
        }

        @Override
        public void schedule(TimerTask task, long delay) {
            throw new IllegalStateException("timer rejected task");
        }
    }
}
