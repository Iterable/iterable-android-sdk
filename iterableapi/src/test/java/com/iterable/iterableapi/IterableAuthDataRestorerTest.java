package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IterableAuthDataRestorerTest {
    private IterableKeychain keychain;
    private ScheduledExecutorService retryExecutor;
    private Queue<Runnable> scheduledTasks;
    private RecordingCallback callback;
    private IterableAuthDataRestorer restorer;

    @Before
    public void setUp() {
        keychain = mock(IterableKeychain.class);
        retryExecutor = mock(ScheduledExecutorService.class);
        scheduledTasks = new ArrayDeque<>();
        when(
                retryExecutor.schedule(
                        any(Runnable.class),
                        anyLong(),
                        eq(TimeUnit.MILLISECONDS)
                )
        ).thenAnswer(
                invocation -> {
                    scheduledTasks.add(invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                }
        );

        callback = new RecordingCallback();
        restorer = new IterableAuthDataRestorer(keychain, retryExecutor);
    }

    @Test
    public void timeoutThenSuccessfulReadRestoresToken() {
        doReturn(new KeychainReadResult.Value("stored-token"))
                .when(keychain)
                .readAuthToken();

        restorer.restore(KeychainReadResult.TimedOut.INSTANCE, callback);
        runNext();

        assertEquals("stored-token", callback.restoredToken);
        assertEquals(0, callback.unavailableCount);
    }

    @Test
    public void exhaustingTimeoutRetriesRemainsUnavailable() {
        doReturn(KeychainReadResult.TimedOut.INSTANCE)
                .when(keychain)
                .readAuthToken();

        restorer.restore(KeychainReadResult.TimedOut.INSTANCE, callback);
        runNext();
        runNext();

        verify(keychain, times(IterableAuthDataRestorer.MAX_TIMEOUT_RETRIES))
                .readAuthToken();
        assertNull(callback.restoredToken);
        assertEquals(1, callback.unavailableCount);
        assertTrue(restorer.resumeIfUnresolved());
        assertEquals(1, scheduledTasks.size());
    }

    @Test
    public void foregroundRetryCanRecoverAfterAnUnavailableCycle() {
        doReturn(
                KeychainReadResult.TimedOut.INSTANCE,
                KeychainReadResult.TimedOut.INSTANCE,
                new KeychainReadResult.Value("stored-token")
        )
                .when(keychain)
                .readAuthToken();

        restorer.restore(KeychainReadResult.TimedOut.INSTANCE, callback);
        runNext();
        runNext();
        assertEquals(1, callback.unavailableCount);

        assertTrue(restorer.resumeIfUnresolved());
        runNext();

        assertEquals("stored-token", callback.restoredToken);
    }

    @Test
    public void cancellationPreventsAQueuedRetryFromRestoringAStaleToken() {
        restorer.restore(KeychainReadResult.TimedOut.INSTANCE, callback);
        restorer.cancel("identity_changed");
        runNext();

        assertNull(callback.restoredToken);
        assertEquals(0, callback.unavailableCount);
        verify(keychain, never()).readAuthToken();
    }

    private void runNext() {
        scheduledTasks.remove().run();
    }

    private static class RecordingCallback implements IterableAuthDataRestorer.Callback {
        String restoredToken;
        int unavailableCount;

        @Override
        public void onAuthTokenRestored(String authToken) {
            restoredToken = authToken;
        }

        @Override
        public void onAuthTokenUnavailable() {
            unavailableCount++;
        }
    }
}
