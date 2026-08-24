package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
}
