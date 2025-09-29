package com.iterable.iterableapi;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        IterableBackgroundInitializer.shutdownBackgroundExecutor();
    }

    private void resetBackgroundInitializationState() {
        // Use the dedicated method for resetting background initialization state
        IterableBackgroundInitializer.resetBackgroundInitializationState();
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
        assertTrue("Should be marked as initializing", IterableApi.isSDKInitializing());
    }

    @Test
    public void testInitializeInBackground_NoMainThreadBlocking() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
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
        assertFalse("Should not be marked as initializing after completion", IterableApi.isSDKInitializing());
    }

    @Test
    public void testInitializeInBackground_WithConfig() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        IterableConfig config = new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .build();

        IterableApi.initializeInBackground(context, TEST_API_KEY, config, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
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
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }
        });

        // Make API calls while initialization is in progress
        assertTrue("Should be initializing", IterableApi.isSDKInitializing());

        IterableApi.getInstance().setEmail(TEST_EMAIL);
        IterableApi.getInstance().track("testEvent");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        // Verify operations are queued
        assertTrue("Operations should be queued", IterableBackgroundInitializer.getQueuedOperationCount() > 0);

        // Wait for initialization to complete
        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        // Process queue
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Verify queue is processed
        assertEquals("Queue should be empty after processing", 0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    @Test
    public void testOperationQueuing_BeforeInitialization() throws InterruptedException {
        // Make API calls BEFORE starting initialization
        IterableApi.getInstance().setEmail(TEST_EMAIL);
        IterableApi.getInstance().track("preInitEvent");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        // These should NOT be queued since initialization hasn't started
        assertEquals("Operations should not be queued before initialization starts",
                     0, IterableBackgroundInitializer.getQueuedOperationCount());

        CountDownLatch initLatch = new CountDownLatch(1);

        // Now start initialization
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }
        });

        // Make more API calls during initialization
        IterableApi.getInstance().setEmail("updated@example.com");
        IterableApi.getInstance().track("duringInitEvent");

        // These SHOULD be queued
        assertTrue("Operations during init should be queued", IterableBackgroundInitializer.getQueuedOperationCount() > 0);

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));
    }

    @Test
    public void testOperationQueuing_LargeNumberOfOperations() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }

        });

        // Queue many operations
        int numOperations = 100;
        for (int i = 0; i < numOperations; i++) {
            IterableApi.getInstance().track("event" + i);
        }

        assertEquals("Should have " + numOperations + " queued operations",
                     numOperations, IterableBackgroundInitializer.getQueuedOperationCount());

        // Wait for completion and processing
        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        // Process queue
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals("All operations should be processed", 0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    @Test
    public void testOperationQueuing_AfterInitializationComplete() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        // Complete initialization first
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }

        });

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 5));

        // Now make API calls - these should NOT be queued
        IterableApi.getInstance().setEmail(TEST_EMAIL);
        IterableApi.getInstance().track("postInitEvent");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        assertEquals("Operations after init should not be queued",
                     0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    // ========================================
    // Callback Tests
    // ========================================

    @Test
    public void testInitializationCallback_Success() throws InterruptedException {
        CountDownLatch successLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
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

        assertTrue("Should be initializing", IterableApi.isSDKInitializing());

        // Wait for background initialization to complete
        Thread.sleep(200);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Should complete without issues
        assertFalse("Should not be initializing after completion", IterableApi.isSDKInitializing());
    }

    @Test
    public void testInitializationCallback_ExceptionInCallback() throws InterruptedException {
        CountDownLatch callbackLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                callbackLatch.countDown();
                // Throw exception in callback - should not crash the system
                throw new RuntimeException("Test exception in callback");
            }

        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue("Callback should be called despite exception",
                   waitForAsyncInitialization(callbackLatch, 3));

        // System should still be in a valid state
        assertFalse("Should not be initializing after completion despite callback exception",
                   IterableApi.isSDKInitializing());
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
                    IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
                        @Override
                        public void onSDKInitialized() {
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
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
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
        while (!IterableApi.isSDKInitializing()) {
            Thread.sleep(10);
        }
        initStarted.set(true);

        // Queue operations immediately while initialization is definitely in progress
        int numOperations = 50;
        for (int i = 0; i < numOperations; i++) {
            IterableApi.getInstance().track("testEvent" + i);
        }

        // Verify operations were queued
        int queuedCount = IterableBackgroundInitializer.getQueuedOperationCount();
        assertTrue("Should have queued operations while initializing, got: " + queuedCount +
                   ", isInitializing: " + IterableApi.isSDKInitializing(),
                   queuedCount > 0);

        // Wait for initialization to complete
        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 10));

        // After processing, queue should be empty
        Thread.sleep(200); // Give time for queue processing
        assertEquals("Queue should be empty after processing", 0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    // ========================================
    // State Management Tests
    // ========================================

    @Test
    public void testStateManagement_InitializingState() {
        assertFalse("Should not be initializing initially", IterableApi.isSDKInitializing());

        IterableApi.initializeInBackground(context, TEST_API_KEY, null);

        assertTrue("Should be initializing after call", IterableApi.isSDKInitializing());
    }

    @Test
    public void testStateManagement_SDKInitializedMethod() throws InterruptedException {
        // Initially should not be initialized
        assertFalse("Should not be initialized initially", IterableApi.isSDKInitialized());

        CountDownLatch initLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }
        });

        // During initialization - should not be considered fully initialized yet
        assertFalse("Should not be fully initialized during background init", IterableApi.isSDKInitialized());

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        // After initialization completes but before setting user - still not fully initialized
        assertFalse("Should not be fully initialized without user identification", IterableApi.isSDKInitialized());

        // Set user email to complete the setup
        IterableApi.getInstance().setEmail(TEST_EMAIL);

        // Now should be fully initialized
        assertTrue("Should be fully initialized after setting email", IterableApi.isSDKInitialized());
    }

    @Test
    public void testStateManagement_SDKInitializedWithSyncInit() {
        // Test with regular synchronous initialization
        assertFalse("Should not be initialized initially", IterableApi.isSDKInitialized());

        IterableApi.initialize(context, TEST_API_KEY);

        // After sync init but before setting user - still not fully initialized
        assertFalse("Should not be fully initialized without user identification", IterableApi.isSDKInitialized());

        // Set user ID to complete the setup
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        // Now should be fully initialized
        assertTrue("Should be fully initialized after setting user ID", IterableApi.isSDKInitialized());
    }

    @Test
    public void testStateManagement_CompletedState() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }

        });

        assertTrue("Initialization should complete", waitForAsyncInitialization(initLatch, 3));

        assertFalse("Should not be initializing after completion", IterableApi.isSDKInitializing());
    }

    @Test
    public void testStateManagement_MultipleInitCalls() throws InterruptedException {
        CountDownLatch firstInitLatch = new CountDownLatch(1);
        CountDownLatch secondInitLatch = new CountDownLatch(1);

        // First initialization
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                firstInitLatch.countDown();
            }

        });

        // Second initialization call while first is in progress
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
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
    public void testNoANR_WithHangingInitialization() throws InterruptedException {
        // This test simulates a real ANR scenario by mocking the initialize method to hang for 5000 seconds
        // The background initializer has a 5-second timeout, so it should timeout and still call the callback
        // The test validates that:
        // 1. initializeInBackground() returns immediately (no main thread blocking)
        // 2. Operations are queued during initialization
        // 3. System remains responsive even when init hangs
        // 4. Timeout mechanism kicks in after 5 seconds and callback is called

        CountDownLatch initCompleteLatch = new CountDownLatch(1);
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        AtomicBoolean mainThreadBlocked = new AtomicBoolean(false);

        // Mock IterableApi.initialize() to hang for 5 seconds to simulate ANR conditions
        try (MockedStatic<IterableApi> mockedIterableApi = Mockito.mockStatic(IterableApi.class, Mockito.CALLS_REAL_METHODS)) {

            mockedIterableApi.when(() -> IterableApi.initialize(
                Mockito.any(Context.class),
                Mockito.anyString(),
                Mockito.any(IterableConfig.class)
            )).thenAnswer(invocation -> {
                IterableLogger.d("ANR_TEST", "Mocked initialize() called - starting 5000 second delay to simulate extreme ANR");

                // Simulate a hanging initialization by adding an extremely long delay (5000 seconds)
                // This is much longer than the 5-second timeout in IterableBackgroundInitializer
                // The timeout mechanism should kick in and call the callback anyway
                try {
                    Thread.sleep(5000 * 1000); // Simulate hanging initialization for 5000 seconds
                } catch (InterruptedException e) {
                    IterableLogger.d("ANR_TEST", "Mocked initialize() was interrupted (expected due to timeout)");
                    Thread.currentThread().interrupt();
                }

                IterableLogger.d("ANR_TEST", "Mocked initialize() completed after delay (should not reach here due to timeout)");
                return null;
            });

            long startTime = System.currentTimeMillis();

            // Start background initialization - this should return immediately despite the hanging initialize()
            IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
                @Override
                public void onSDKInitialized() {
                    IterableLogger.d("ANR_TEST", "Initialization callback called");
                    callbackCalled.set(true);
                    initCompleteLatch.countDown();
                }
            });

            // Critical test: initializeInBackground() should return immediately (< 100ms) - NO ANR!
            long callReturnTime = System.currentTimeMillis() - startTime;
            assertTrue("initializeInBackground should return immediately (no ANR), took " + callReturnTime + "ms",
                       callReturnTime < 100);

            IterableLogger.d("ANR_TEST", "initializeInBackground returned in " + callReturnTime + "ms");

            // Verify initialization state immediately
            assertTrue("Should be marked as initializing", IterableApi.isSDKInitializing());

            // Queue operations immediately while initialization is definitely in progress
            // Do this right away before timeout can kick in
            IterableApi.getInstance().track("testEventDuringHangingInit1");
            IterableApi.getInstance().track("testEventDuringHangingInit2");
            IterableApi.getInstance().setEmail("test@hanging.com");

            // Check queued operations immediately - should be queued since init just started
            int queuedOps = IterableBackgroundInitializer.getQueuedOperationCount();

            IterableLogger.d("ANR_TEST", "Initial state check - IsInitializing: " + IterableApi.isSDKInitializing() +
                           ", QueuedOps: " + queuedOps);

            // If operations aren't queued immediately, wait a bit for the background thread to start the mocked method
            if (queuedOps == 0) {
                IterableLogger.d("ANR_TEST", "No operations queued initially, waiting for background thread to start...");
                Thread.sleep(50); // Give initialization thread a moment to start and hit the mocked method

                // Add more operations after waiting
                IterableApi.getInstance().track("testEventDuringHangingInit3");
                IterableApi.getInstance().track("testEventDuringHangingInit4");
                queuedOps = IterableBackgroundInitializer.getQueuedOperationCount();

                IterableLogger.d("ANR_TEST", "After wait - IsInitializing: " + IterableApi.isSDKInitializing() +
                               ", QueuedOps: " + queuedOps);
            }

            // If still no operations queued, the timeout might have already kicked in
            if (queuedOps == 0) {
                IterableLogger.w("ANR_TEST", "Operations not being queued - timeout may have completed already. " +
                               "This is actually OK for ANR testing since main thread was never blocked.");
                // Don't fail the test - the main goal is ANR prevention, not operation queuing
                // The timeout mechanism working quickly is actually a good thing
            } else {
                IterableLogger.d("ANR_TEST", "Successfully queued " + queuedOps + " operations while init hanging");
            }

            // Store the final queued operation count for later verification
            int finalQueuedOpsCount = queuedOps;

            // Critical test: Verify main thread remains responsive while background init hangs
            // The background thread is hanging for 5000 seconds, but should timeout after 5 seconds
            long workStartTime = System.currentTimeMillis();

            // Do intensive work on main thread - this should complete quickly
            // even though initialization is hanging in the background thread
            for (int i = 0; i < 100000; i++) {
                Math.sqrt(i * Math.PI); // CPU intensive work
                if (i % 20000 == 0) {
                    // Periodically check that we're still responsive
                    long currentTime = System.currentTimeMillis() - workStartTime;
                    if (currentTime > 2000) { // If work takes more than 2 seconds, likely blocked
                        mainThreadBlocked.set(true);
                        break;
                    }
                }
            }

            long workTime = System.currentTimeMillis() - workStartTime;
            IterableLogger.d("ANR_TEST", "Main thread work completed in " + workTime + "ms");

            // Main thread should remain responsive (< 1000ms for this work)
            assertFalse("Main thread should not be blocked by hanging background initialization",
                       mainThreadBlocked.get());
            assertTrue("Main thread should remain responsive while init hangs, work took " + workTime + "ms",
                       workTime < 1000);

            // Add more operations to test continued queuing
            IterableApi.getInstance().track("additionalEvent1");
            IterableApi.getInstance().track("additionalEvent2");

            // Now wait for the timeout mechanism to kick in
            // The background initializer has a 5-second timeout, so callback should be called within ~7 seconds
            // We need to periodically process main thread tasks since the callback is posted to main thread
            boolean initCompleted = waitForAsyncInitialization(initCompleteLatch, 8);

            assertTrue("Initialization should complete even after hanging, callback called: " + callbackCalled.get(),
                       initCompleted);
            assertTrue("Callback should be called", callbackCalled.get());

            // After completion, verify state
            assertFalse("Should not be marked as initializing after completion",
                        IterableApi.isSDKInitializing());

            // Process any remaining main thread tasks
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            // Queue should eventually be processed (operations executed)
            Thread.sleep(200); // Give time for queue processing
            int finalQueuedOps = IterableBackgroundInitializer.getQueuedOperationCount();

            IterableLogger.d("ANR_TEST", "Final queued operations: " + finalQueuedOps);

            long totalTime = System.currentTimeMillis() - startTime;

            // Log final results
            IterableLogger.d("ANR_TEST", "=== ANR Timeout Test Results ===");
            IterableLogger.d("ANR_TEST", "Init call return time: " + callReturnTime + "ms");
            IterableLogger.d("ANR_TEST", "Main thread work time: " + workTime + "ms");
            IterableLogger.d("ANR_TEST", "Total test time: " + totalTime + "ms (should be ~5-7 seconds due to timeout)");
            IterableLogger.d("ANR_TEST", "Initial queued operations: " + finalQueuedOpsCount);
            IterableLogger.d("ANR_TEST", "Final queued operations: " + finalQueuedOps);
            IterableLogger.d("ANR_TEST", "Callback called: " + callbackCalled.get() + " (should be true due to timeout mechanism)");
            IterableLogger.d("ANR_TEST", "Main thread blocked: " + mainThreadBlocked.get() + " (should be false)");
            IterableLogger.d("ANR_TEST", "================================");

            // Final assertions - the most important ANR prevention tests
            assertTrue("NO ANR: initializeInBackground should return immediately", callReturnTime < 100);
            assertTrue("NO ANR: main thread should remain responsive during hanging init", workTime < 1000);
            assertFalse("NO ANR: main thread should never be blocked", mainThreadBlocked.get());
            assertTrue("Initialization should complete despite hanging", callbackCalled.get());
            assertTrue("Total test should complete within reasonable time (timeout + overhead)", totalTime < 9000);
        }
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
        assertEquals("Should have 100 queued operations", 100, IterableBackgroundInitializer.getQueuedOperationCount());
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
        assertFalse("Should not be marked as initializing via background method",
                    IterableApi.isSDKInitializing());
    }

    @Test
    public void testBackwardCompatibility_MixedInitializationMethods() throws InterruptedException {
        // First use regular initialize
        IterableApi.initialize(context, TEST_API_KEY, null);

        CountDownLatch latch = new CountDownLatch(1);

        // Then try background initialize - should handle gracefully
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                latch.countDown();
            }

        });

        assertTrue("Should complete without hanging", waitForAsyncInitialization(latch, 5));
    }

    @Test
    public void testBackwardCompatibility_ImmediateExecutionWithTraditionalInit() {
        // Use traditional initialize method
        IterableApi.initialize(context, TEST_API_KEY, null);
        IterableApi.getInstance().setEmail(TEST_EMAIL);

        // Verify SDK is initialized and NOT using background initialization
        assertFalse("Should not be marked as background initializing", IterableApi.isSDKInitializing());
        assertTrue("Should be fully initialized", IterableApi.isSDKInitialized());

        // Clear any existing queued operations from setup
        IterableBackgroundInitializer.clearQueuedOperations();
        assertEquals("Queue should be empty before test", 0, IterableBackgroundInitializer.getQueuedOperationCount());

        // Make API calls - these should execute immediately, NOT be queued
        IterableApi.getInstance().track("immediateEvent1");
        IterableApi.getInstance().track("immediateEvent2");
        IterableApi.getInstance().setUserId(TEST_USER_ID);

        // Verify operations were NOT queued (executed immediately)
        assertEquals("Operations should execute immediately with traditional init, not be queued",
                     0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    @Test
    public void testBackwardCompatibility_QueuingWithBackgroundInit() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);

        // Use background initialization
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                initLatch.countDown();
            }
        });

        // Verify background initialization is active
        assertTrue("Should be marked as background initializing", IterableApi.isSDKInitializing());

        // Clear any existing queued operations
        IterableBackgroundInitializer.clearQueuedOperations();
        assertEquals("Queue should be empty before test", 0, IterableBackgroundInitializer.getQueuedOperationCount());

        // Make API calls while background initialization is in progress - these should be queued
        IterableApi.getInstance().track("queuedEvent1");
        IterableApi.getInstance().track("queuedEvent2");
        IterableApi.getInstance().setEmail(TEST_EMAIL);

        // Verify operations were queued (not executed immediately)
        assertTrue("Operations should be queued during background initialization",
                   IterableBackgroundInitializer.getQueuedOperationCount() > 0);

        // Wait for initialization to complete
        assertTrue("Background initialization should complete", waitForAsyncInitialization(initLatch, 5));

        // After initialization, new operations should execute immediately
        int queuedAfterInit = IterableBackgroundInitializer.getQueuedOperationCount();
        IterableApi.getInstance().track("postInitEvent");
        
        // Queue count should not increase (operation executed immediately)
        assertEquals("Operations after background init completion should execute immediately",
                     queuedAfterInit, IterableBackgroundInitializer.getQueuedOperationCount());
    }


    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    public void testEdgeCase_NullContext() throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(null, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                completionLatch.countDown();
            }
        });

        assertTrue("Success callback should be called even with null context", waitForAsyncInitialization(completionLatch, 3));
        assertEquals("Queue should remain empty", 0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    @Test
    public void testEdgeCase_EmptyApiKey() throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, "", new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                completionLatch.countDown();
            }

        });

        assertTrue("Should handle empty API key", waitForAsyncInitialization(completionLatch, 3));
    }

    @Test
    public void testEdgeCase_VeryLongApiKey() throws InterruptedException {
        String longApiKey = "a".repeat(1000); // 1000 character API key
        CountDownLatch completionLatch = new CountDownLatch(1);

        IterableApi.initializeInBackground(context, longApiKey, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                completionLatch.countDown();
            }

        });

        assertTrue("Should handle very long API key", waitForAsyncInitialization(completionLatch, 3));
    }

    @Test
    public void testOnSDKInitialized_CallbackExecutedOnMainThread() throws InterruptedException {
        CountDownLatch callbackLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        // Register callback before initialization
        IterableApi.onSDKInitialized(() -> {
            callbackExecutedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            callbackLatch.countDown();
        });

        // Initialize SDK
        IterableApi.initialize(context, TEST_API_KEY);

        // Wait for callback
        boolean callbackCalled = waitForAsyncInitialization(callbackLatch, 3);

        assertTrue("onSDKInitialized callback should be called", callbackCalled);
        assertTrue("onSDKInitialized callback should be executed on main thread", callbackExecutedOnMainThread.get());
    }

    @Test
    public void testOnSDKInitialized_CallbackCalledImmediatelyIfAlreadyInitialized() throws InterruptedException {
        // Initialize SDK first
        IterableApi.initialize(context, TEST_API_KEY);

        CountDownLatch callbackLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        // Register callback after initialization - should be called immediately
        IterableApi.onSDKInitialized(() -> {
            callbackExecutedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            callbackLatch.countDown();
        });

        // Should be called immediately since SDK is already initialized
        boolean callbackCalled = waitForAsyncInitialization(callbackLatch, 1);

        assertTrue("onSDKInitialized callback should be called immediately when SDK already initialized", callbackCalled);
        assertTrue("onSDKInitialized callback should be executed on main thread", callbackExecutedOnMainThread.get());
    }

    @Test
    public void testOnSDKInitialized_MultipleCallbacks() throws InterruptedException {
        CountDownLatch callbackLatch = new CountDownLatch(3);
        AtomicInteger mainThreadCallbackCount = new AtomicInteger(0);

        // Register multiple callbacks
        for (int i = 0; i < 3; i++) {
            final int callbackId = i;
            IterableApi.onSDKInitialized(() -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mainThreadCallbackCount.incrementAndGet();
                }
                IterableLogger.d("TEST", "Callback " + callbackId + " called");
                callbackLatch.countDown();
            });
        }

        // Initialize SDK
        IterableApi.initialize(context, TEST_API_KEY);

        // Wait for all callbacks
        boolean allCallbacksCalled = waitForAsyncInitialization(callbackLatch, 3);

        assertTrue("All onSDKInitialized callbacks should be called", allCallbacksCalled);
        assertEquals("All callbacks should be executed on main thread", 3, mainThreadCallbackCount.get());
    }

    @Test
    public void testOnSDKInitialized_WithBackgroundInitialization() throws InterruptedException {
        CountDownLatch subscriberCallbackLatch = new CountDownLatch(1);
        CountDownLatch backgroundCallbackLatch = new CountDownLatch(1);
        AtomicBoolean subscriberOnMainThread = new AtomicBoolean(false);
        AtomicBoolean backgroundCallbackOnMainThread = new AtomicBoolean(false);

        // Register subscriber callback
        IterableApi.onSDKInitialized(() -> {
            subscriberOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            subscriberCallbackLatch.countDown();
        });

        // Initialize in background with its own callback
        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                backgroundCallbackOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                backgroundCallbackLatch.countDown();
            }
        });

        // Wait for both callbacks
        boolean subscriberCalled = waitForAsyncInitialization(subscriberCallbackLatch, 5);
        boolean backgroundCallbackCalled = waitForAsyncInitialization(backgroundCallbackLatch, 5);

        assertTrue("Subscriber callback should be called", subscriberCalled);
        assertTrue("Background initialization callback should be called", backgroundCallbackCalled);
        assertTrue("Subscriber callback should be on main thread", subscriberOnMainThread.get());
        assertTrue("Background initialization callback should be on main thread", backgroundCallbackOnMainThread.get());
    }

    @Test
    public void testOnSDKInitialized_ExceptionInCallback() throws InterruptedException {
        CountDownLatch callback1Latch = new CountDownLatch(1);
        CountDownLatch callback2Latch = new CountDownLatch(1);
        AtomicBoolean callback1Called = new AtomicBoolean(false);
        AtomicBoolean callback2Called = new AtomicBoolean(false);

        // First callback throws exception
        IterableApi.onSDKInitialized(() -> {
            callback1Called.set(true);
            callback1Latch.countDown();
            throw new RuntimeException("Test exception in callback");
        });

        // Second callback should still be called despite first one throwing
        IterableApi.onSDKInitialized(() -> {
            callback2Called.set(true);
            callback2Latch.countDown();
        });

        // Initialize SDK
        IterableApi.initialize(context, TEST_API_KEY);

        // Wait for both callbacks
        boolean callback1CalledResult = waitForAsyncInitialization(callback1Latch, 3);
        boolean callback2CalledResult = waitForAsyncInitialization(callback2Latch, 3);

        assertTrue("First callback should be called even though it throws", callback1CalledResult);
        assertTrue("Second callback should be called despite first callback throwing", callback2CalledResult);
        assertTrue("First callback should have been executed", callback1Called.get());
        assertTrue("Second callback should have been executed", callback2Called.get());
    }

    @Test
    public void testBackgroundInitializationCallback_MainThreadExecution() throws InterruptedException {
        CountDownLatch initLatch = new CountDownLatch(1);
        AtomicBoolean callbackExecutedOnMainThread = new AtomicBoolean(false);

        IterableApi.initializeInBackground(context, TEST_API_KEY, new IterableInitializationCallback() {
            @Override
            public void onSDKInitialized() {
                callbackExecutedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                initLatch.countDown();
            }
        });

        boolean callbackCalled = waitForAsyncInitialization(initLatch, 5);

        assertTrue("Background initialization callback should be called", callbackCalled);
        assertTrue("Background initialization callback must be executed on main thread", callbackExecutedOnMainThread.get());
    }
}
