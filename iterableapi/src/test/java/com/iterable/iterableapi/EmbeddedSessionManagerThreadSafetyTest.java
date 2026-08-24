package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EmbeddedSessionManagerThreadSafetyTest extends BaseTest {

    private EmbeddedSessionManager sessionManager;

    @Before
    public void setUp() {
        IterableApi.sharedInstance = new IterableApi();
        sessionManager = new EmbeddedSessionManager();
    }

    @Test
    public void endSessionWithoutImpressionsLeavesSessionRunning() {
        sessionManager.startSession();
        sessionManager.endSession();

        assertTrue(sessionManager.isTracking());
    }

    @Test
    public void endSessionWithImpressionsResetsSession() {
        sessionManager.startSession();
        sessionManager.startImpression("message-1", 1L);
        sessionManager.pauseImpression("message-1");
        sessionManager.endSession();

        assertFalse(sessionManager.isTracking());
    }

    @Test
    public void concurrentEndSessionTracksSessionOnlyOnce() throws Exception {
        BlockingRecordingIterableApi recordingApi = new BlockingRecordingIterableApi();
        IterableApi.sharedInstance = recordingApi;

        sessionManager.startSession();
        sessionManager.startImpression("message-1", 1L);
        sessionManager.pauseImpression("message-1");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Keep the first tracking call open while a second thread ends the same session.
            Future<?> firstEnd = executor.submit(sessionManager::endSession);
            assertTrue(
                    "first endSession did not reach tracking",
                    recordingApi.awaitFirstTrack(5, TimeUnit.SECONDS)
            );

            // The session must already be cleared, even though its first tracking call is blocked.
            // Ending it again must therefore return without tracking the same session twice.
            Future<?> secondEnd = executor.submit(sessionManager::endSession);
            secondEnd.get(5, TimeUnit.SECONDS);

            recordingApi.allowFirstTrackToFinish();
            firstEnd.get(5, TimeUnit.SECONDS);
        } finally {
            recordingApi.allowFirstTrackToFinish();
            executor.shutdownNow();
        }

        List<IterableEmbeddedSession> trackedSessions = recordingApi.getTrackedSessions();
        assertEquals("the active session should be tracked exactly once", 1, trackedSessions.size());

        List<IterableEmbeddedImpression> impressions = trackedSessions.get(0).getImpressions();
        assertNotNull(impressions);
        assertEquals(1, impressions.size());
        assertEquals("message-1", impressions.get(0).getMessageId());
        assertEquals(1, impressions.get(0).getDisplayCount());
    }

    @Test
    public void concurrentSessionAndImpressionUpdatesDoNotThrow() throws Exception {
        final int threadCount = 8;
        final int iterations = 2000;
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch finishGate = new CountDownLatch(threadCount);
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());

        sessionManager.startSession();

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            final int role = threadIndex % 4;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startGate.await();
                        for (int i = 0; i < iterations; i++) {
                            String messageId = "message-" + (i % 4);
                            switch (role) {
                                case 0:
                                    sessionManager.startImpression(messageId, i % 3);
                                    break;
                                case 1:
                                    sessionManager.pauseImpression(messageId);
                                    break;
                                case 2:
                                    sessionManager.endSession();
                                    break;
                                default:
                                    sessionManager.startSession();
                                    break;
                            }
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    } finally {
                        finishGate.countDown();
                    }
                }
            }, "embedded-session-" + threadIndex).start();
        }

        startGate.countDown();

        assertTrue("threads did not finish in time", finishGate.await(30, TimeUnit.SECONDS));
        assertEquals("concurrent access failed: " + failures, 0, failures.size());
    }

    private static class BlockingRecordingIterableApi extends IterableApi {
        private final AtomicInteger trackCallCount = new AtomicInteger();
        private final List<IterableEmbeddedSession> trackedSessions =
                Collections.synchronizedList(new ArrayList<IterableEmbeddedSession>());
        private final CountDownLatch firstTrackStarted = new CountDownLatch(1);
        private final CountDownLatch allowFirstTrackToFinish = new CountDownLatch(1);

        @Override
        public void trackEmbeddedSession(IterableEmbeddedSession session) {
            int callNumber = trackCallCount.incrementAndGet();
            trackedSessions.add(session);

            if (callNumber == 1) {
                firstTrackStarted.countDown();
                try {
                    if (!allowFirstTrackToFinish.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("first tracking call was not released");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while waiting to finish tracking", exception);
                }
            }
        }

        boolean awaitFirstTrack(long timeout, TimeUnit unit) throws InterruptedException {
            return firstTrackStarted.await(timeout, unit);
        }

        void allowFirstTrackToFinish() {
            allowFirstTrackToFinish.countDown();
        }

        List<IterableEmbeddedSession> getTrackedSessions() {
            synchronized (trackedSessions) {
                return new ArrayList<>(trackedSessions);
            }
        }
    }
}
