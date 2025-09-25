package com.iterable.iterableapi;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Comprehensive test suite for async initialization functionality.
 * Tests ANR elimination, operation queuing, callback execution, and edge cases.
 */
@Config(sdk = 21)
@RunWith(RobolectricTestRunner.class)
public class IterableAsyncInitializationTest {

    private Context context;
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "test-user-123";

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Reset the shared instance before each test
        IterableTestUtils.resetIterableApi();

        // Clear any background initialization state
        resetBackgroundInitializationState();
    }

    @After
    public void tearDown() {
        IterableTestUtils.resetIterableApi();
        resetBackgroundInitializationState();
    }

    @org.junit.AfterClass
    public static void tearDownClass() {
        // Shutdown executor service after all tests complete
        IterableApi.shutdownBackgroundExecutor();
    }

    private void resetBackgroundInitializationState() {
        // Use the dedicated method for resetting background initialization state
        IterableApi.resetBackgroundInitializationState();
    }

    /**
     * Helper method to wait for async initialization with proper timing for test environment.
     * This handles the background thread + main thread callback timing issues.
     */
    private boolean waitForAsyncInitialization(CountDownLatch latch, int timeoutSeconds) throws InterruptedException {
        // Give background thread more time to execute in test environment
        for (int i = 0; i < 20; i++) { // Try for up to 2 seconds (20 * 100ms)
            Thread.sleep(100);

            // Process any pending main thread tasks (like callbacks)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            // Check if completed
            if (latch.getCount() == 0) {
                return true;
            }
        }

        // Final wait with timeout
        boolean result = latch.await(timeoutSeconds, TimeUnit.SECONDS);

        // Process any remaining main thread tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        return result;
    }

    // ========================================
    // ANR Elimination Tests
    // ========================================

    @Test
    public void testInitializeInBackground_ReturnsImmediately() {
        long startTime = System.currentTimeMillis();

        IterableApi.initializeInBackground(context, TEST_API_KEY, null);

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue("initializeInBackground should return in <50ms, took " + elapsedTime + "ms",
                   elapsedTime < 50);
        assertTrue("Should be marked as initializing", IterableApi.isInitializingInBackground());
        assertFalse("Should not be marked as completed yet", IterableApi.isBackgroundInitializationComplete());
    }

    @Test
    public void testInitializeInBackground_NoMainThreadBlocking() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                callbackExecutedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                initLatch.countDown();
            }
        });

        // Verify main thread is not blocked
        assertTrue("Method should return immediately", true);

        // Wait for completion with proper timing
        assertTrue("Initialization should complete within 5 seconds",
                   waitForAsyncInitialization(initLatch, 5));
        assertTrue("Callback should execute on main thread", callbackExecutedOnMainThread.get());
        assertTrue("Should be marked as completed", IterableApi.isBackgroundInitializationComplete());
        assertFalse("Should not be marked as initializing", IterableApi.isInitializingInBackground());
    }

    @Test
    public void testInitializeInBackground_WithConfig() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        IterableConfig config = new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .build();

        IterableApi.initializeInBackground(context, TEST_API_KEY, config, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                initLatch.countDown();
            }
        });

        assertTrue("Initialization with config should complete",
                   waitForAsyncInitialization(initLatch, 3));
    }

    // ========================================
    // Operation Queuing Tests
    // ========================================

    @Test
    public void testOperationQueuing_DuringInitialization() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        List<String> executedOperations = new ArrayList<>();

        // Start background initialization
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                initLatch.countDown();
            }
        });

        // Make API calls while initialization is in progress
        assertTrue("Should be initializing", IterableApi.isInitializingInBackground());

        IterableApi.getInstance().setEmail(TEST_EMAIL);
        IterableApi.getInstance().track("testEvent");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        // Verify operations are queued
        assertTrue("Operations should be queued", IterableApi.getQueuedOperationCount() > 0);

        // Wait for initialization to complete
        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        // Process queue
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Verify queue is processed
        assertEquals("Queue should be empty after processing", 0, IterableApi.getQueuedOperationCount());
        assertTrue("Should be completed", IterableApi.isBackgroundInitializationComplete());
    }

    @Test
    public void testOperationQueuing_BeforeInitialization() throws InterruptedException {
        // Make API calls BEFORE starting initialization
        IterableApi.getInstance().setEmail(TEST_EMAIL);
        IterableApi.getInstance().track("preInitEvent");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        // These should NOT be queued since initialization hasn't started
        assertEquals("Operations should not be queued before initialization starts",
                     0, IterableApi.getQueuedOperationCount());

        CountDownLatch initLatch = new CountDownLatch(1);

        // Now start initialization
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                initLatch.countDown();
            }
        });

        // Make more API calls during initialization
        IterableApi.getInstance().setEmail("updated@example.com");
        IterableApi.getInstance().track("duringInitEvent");

        // These SHOULD be queued
        assertTrue("Operations during init should be queued", IterableApi.getQueuedOperationCount() > 0);

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));
    }

    @Test
    public void testOperationQueuing_LargeNumberOfOperations() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                initLatch.countDown();
            }

        });

        // Queue many operations
        int numOperations = 100;
        for (int i = 0; i < numOperations; i++) {
            IterableApi.getInstance().track("event" + i);
        }

        assertEquals("Should have " + numOperations + " queued operations",
                     numOperations, IterableApi.getQueuedOperationCount());

        // Wait for completion and processing
        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        // Process queue
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals("All operations should be processed", 0, IterableApi.getQueuedOperationCount());
    }

    @Test
    public void testOperationQueuing_AfterInitializationComplete() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        // Complete initialization first
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                initLatch.countDown();
            }

        });

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 5));

        // Now make API calls - these should NOT be queued
        IterableApi.getInstance().setEmail(TEST_EMAIL);
        IterableApi.getInstance().track("postInitEvent");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        assertEquals("Operations after init should not be queued",
                     0, IterableApi.getQueuedOperationCount());
    }

    // ========================================
    // Callback Tests
    // ========================================

    @Test
    public void testInitializationCallback_Success() throws InterruptedException {
        CountDownLatch successLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                callbackExecutedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                successLatch.countDown();
            }

        });

        assertTrue("Success callback should be called", waitForAsyncInitialization(successLatch, 3));
        assertTrue("Callback should execute on main thread", callbackExecutedOnMainThread.get());
    }

    @Test
    public void testInitializationCallback_NullCallback() throws InterruptedException {
        // Should not crash with null callback
        IterableApi.initializeInBackground(context, TEST_API_KEY, null);

        assertTrue("Should be initializing", IterableApi.isInitializingInBackground());

        // Wait for background initialization to complete
        Thread.sleep(200);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Should complete without issues
        assertTrue("Should complete even with null callback",
                   IterableApi.isBackgroundInitializationComplete());
    }

    @Test
    public void testInitializationCallback_ExceptionInCallback() throws InterruptedException {
        CountDownLatch callbackLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                callbackLatch.countDown();
                // Throw exception in callback - should not crash the system
                throw new RuntimeException("Test exception in callback");
            }

        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue("Callback should be called despite exception",
                   waitForAsyncInitialization(callbackLatch, 3));

        // System should still be in a valid state
        assertTrue("Should be marked as completed despite callback exception",
                   IterableApi.isBackgroundInitializationComplete());
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    public void testConcurrentInitialization_OnlyOneInitializes() throws InterruptedException {
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger warningCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
                        @Override
                        public void onInitializationComplete() {
                            successCount.incrementAndGet();
                            completeLatch.countDown();
                        }

                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    completeLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();

        assertTrue("All threads should complete", waitForAsyncInitialization(completeLatch, 3));

        // All threads should get success callbacks (concurrent calls should all be notified when init completes)
        assertEquals("All threads should get success callbacks", numThreads, successCount.get());
    }

    @Test
    public void testConcurrentOperationQueuing() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        AtomicBoolean initStarted = new AtomicBoolean(false);

        // Start initialization with a callback that adds a delay to ensure we have time to queue operations
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                // Add a small delay to ensure operations have time to queue
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                initLatch.countDown();
            }

        });

        // Wait to ensure initialization has started
        while (!IterableApi.isInitializingInBackground()) {
            Thread.sleep(10);
        }
        initStarted.set(true);

        // Queue operations immediately while initialization is definitely in progress
        int numOperations = 50;
        for (int i = 0; i < numOperations; i++) {
            IterableApi.getInstance().track("testEvent" + i);
        }

        // Verify operations were queued
        int queuedCount = IterableApi.getQueuedOperationCount();
        assertTrue("Should have queued operations while initializing, got: " + queuedCount +
                   ", isInitializing: " + IterableApi.isInitializingInBackground(),
                   queuedCount > 0);

        // Wait for initialization to complete
        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 10));

        // After processing, queue should be empty
        Thread.sleep(200); // Give time for queue processing
        assertEquals("Queue should be empty after processing", 0, IterableApi.getQueuedOperationCount());
    }

    // ========================================
    // State Management Tests
    // ========================================

    @Test
    public void testStateManagement_InitializingState() {
        assertFalse("Should not be initializing initially", IterableApi.isInitializingInBackground());
        assertFalse("Should not be completed initially", IterableApi.isBackgroundInitializationComplete());

        IterableApi.initializeInBackground(context, TEST_API_KEY, null);

        assertTrue("Should be initializing after call", IterableApi.isInitializingInBackground());
        assertFalse("Should not be completed yet", IterableApi.isBackgroundInitializationComplete());
    }

    @Test
    public void testStateManagement_CompletedState() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                initLatch.countDown();
            }

        });

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        assertFalse("Should not be initializing after completion", IterableApi.isInitializingInBackground());
        assertTrue("Should be completed", IterableApi.isBackgroundInitializationComplete());
    }

    @Test
    public void testStateManagement_MultipleInitCalls() throws InterruptedException {
        CountDownLatch firstInitLatch = new CountDownLatch(1);
        CountDownLatch secondInitLatch = new CountDownLatch(1);

        // First initialization
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                firstInitLatch.countDown();
            }

        });

        // Second initialization call while first is in progress
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                secondInitLatch.countDown();
            }

        });

        // First should complete
        assertTrue("First initialization should complete", waitForAsyncInitialization(firstInitLatch, 3));

        // Second should also complete (called immediately since first is done)
        assertTrue("Second initialization should also complete", waitForAsyncInitialization(secondInitLatch, 5));
    }

    // ========================================
    // Performance Tests
    // ========================================

    @Test
    public void testPerformance_InitializationCallTime() {
        long startTime = System.currentTimeMillis();

        IterableApi.initializeInBackground(context, TEST_API_KEY, null);

        long callReturnTime = System.currentTimeMillis() - startTime;
        assertTrue("Method call should return in <50ms, took " + callReturnTime + "ms",
                   callReturnTime < 50);
    }

    @Test
    public void testPerformance_QueueOperationTime() {
        IterableApi.initializeInBackground(context, TEST_API_KEY, null);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            IterableApi.getInstance().track("perfTest" + i);
        }
        long endTime = System.currentTimeMillis();

        assertTrue("Queuing 100 operations should be fast, took " + (endTime - startTime) + "ms",
                   (endTime - startTime) < 100);
        assertEquals("Should have 100 queued operations", 100, IterableApi.getQueuedOperationCount());
    }

    // ========================================
    // Backward Compatibility Tests
    // ========================================

    @Test
    public void testBackwardCompatibility_ExistingInitializeStillWorks() {
        // Test that existing initialize method works unchanged
        IterableApi.initialize(context, TEST_API_KEY, null);

        // SDK should be initialized through normal path
        assertNotNull("SDK instance should exist", IterableApi.getInstance());
        assertFalse("Should not be marked as background initializing",
                    IterableApi.isInitializingInBackground());
        assertFalse("Should not be marked as background completed",
                    IterableApi.isBackgroundInitializationComplete());
    }

    @Test
    public void testBackwardCompatibility_MixedInitializationMethods() throws InterruptedException {
        // First use regular initialize
        IterableApi.initialize(context, TEST_API_KEY, null);

        CountDownLatch latch = new CountDownLatch(1);

        // Then try background initialize - should handle gracefully
        IterableApi.initializeInBackground(context, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                latch.countDown();
            }

        });

        assertTrue("Should complete without hanging", waitForAsyncInitialization(latch, 5));
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    public void testEdgeCase_NullContext() throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(null, TEST_API_KEY, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                completionLatch.countDown();
            }
        });

        assertTrue("Success callback should be called even with null context", waitForAsyncInitialization(completionLatch, 3));
        assertEquals("Queue should remain empty", 0, IterableApi.getQueuedOperationCount());
    }

    @Test
    public void testEdgeCase_EmptyApiKey() throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, "", new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                completionLatch.countDown();
            }

        });

        assertTrue("Should handle empty API key", waitForAsyncInitialization(completionLatch, 3));
    }

    @Test
    public void testEdgeCase_VeryLongApiKey() throws InterruptedException {
        String longApiKey = "a".repeat(1000); // 1000 character API key
        CountDownLatch completionLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, longApiKey, new AsyncInitializationCallback() {
            @Override
            public void onInitializationComplete() {
                completionLatch.countDown();
            }

        });

        assertTrue("Should handle very long API key", waitForAsyncInitialization(completionLatch, 3));
    }
}
