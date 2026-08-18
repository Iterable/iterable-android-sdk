package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IterableAuthRefreshOwnershipTest extends BaseTest {
    private IterableApi api;
    private IterableAuthManager authManager;
    private ExecutorService executor;

    @Before
    public void setUp() {
        api = mock(IterableApi.class);
        when(api.getEmail()).thenReturn("user@example.com");

        authManager = new IterableAuthManager(
                api,
                mock(IterableAuthHandler.class),
                new RetryPolicy(3, 1, RetryPolicy.Type.LINEAR),
                60_000);
        executor = mock(ExecutorService.class);
        authManager.executor = executor;
    }

    @After
    public void tearDown() {
        authManager.clearRefreshTimer();
    }

    @Test
    public void staleTaskCannotRunOrClearItsReplacement() {
        RetainingTimer firstTimer = new RetainingTimer();
        authManager.timer = firstTimer;
        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.AUTH_HANDLER_RETRY,
                null);
        TimerTask staleTask = firstTimer.lastTask();

        authManager.clearRefreshTimer();
        RetainingTimer replacementTimer = new RetainingTimer();
        authManager.timer = replacementTimer;
        authManager.scheduleAuthTokenRefresh(
                2000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                null);
        TimerTask replacementTask = replacementTimer.lastTask();

        staleTask.run();

        assertSame(replacementTask, authManager.scheduledRefreshTask);
        assertEquals(
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                authManager.scheduledRefreshReason);
        verify(executor, never()).submit(any(Runnable.class));

        replacementTask.run();

        assertNull(authManager.scheduledRefreshTask);
        assertNull(authManager.scheduledRefreshReason);
        verify(executor).submit(any(Runnable.class));
    }

    @Test
    public void firingTaskReleasesOwnershipBeforeAnotherRefreshIsScheduled() {
        RetainingTimer timer = new RetainingTimer();
        authManager.timer = timer;
        authManager.scheduleAuthTokenRefresh(
                1000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                null);

        timer.lastTask().run();
        assertNull(authManager.scheduledRefreshTask);

        authManager.scheduleAuthTokenRefresh(
                2000,
                IterableAuthRefreshReason.TOKEN_EXPIRING,
                null);

        assertEquals(2, timer.taskCount());
        assertSame(timer.lastTask(), authManager.scheduledRefreshTask);
    }

    private static class RetainingTimer extends Timer {
        private final List<TimerTask> tasks = new ArrayList<>();

        RetainingTimer() {
            super(true);
            super.cancel();
        }

        @Override
        public void schedule(TimerTask task, long delay) {
            tasks.add(task);
        }

        @Override
        public void cancel() {
            // Retain tasks so a test can run a task after cancellation.
        }

        TimerTask lastTask() {
            return tasks.get(tasks.size() - 1);
        }

        int taskCount() {
            return tasks.size();
        }
    }
}
