package com.iterable.iterableapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles background initialization of the Iterable SDK to prevent ANRs.
 * This class manages operation queuing, thread safety, and initialization state.
 */
class IterableBackgroundInitializer {
    private static final String TAG = "IterableBackgroundInit";

    // Timeout for initialization to prevent indefinite hangs (5 seconds)
    private static final int INITIALIZATION_TIMEOUT_SECONDS = 5;

    // Callback manager for initialization completion
    private static final IterableInitializationCallbackManager callbackManager = new IterableInitializationCallbackManager();

    /**
     * Represents a queued operation that should be executed after initialization
     */
    interface QueuedOperation {
        /**
         * Execute the operation
         */
        void execute();

        /**
         * Get description for debugging
         */
        String getDescription();
    }

    private static class OperationQueue {
        private final ConcurrentLinkedQueue<QueuedOperation> operations = new ConcurrentLinkedQueue<>();
        private volatile boolean isProcessing = false;
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        void enqueue(QueuedOperation operation) {
            operations.offer(operation);
            IterableLogger.d(TAG, "Queued operation: " + operation.getDescription());
        }

        void processAll(ExecutorService executor) {
            if (!canStartProcessing(executor)) {
                return;
            }
            
            isProcessing = true;
            executor.execute(this::processQueuedOperations);
        }

        private boolean canStartProcessing(ExecutorService executor) {
            if (isProcessing) {
                IterableLogger.w(TAG, "Already processing operations, skipping");
                return false;
            }
            
            if (executor == null || executor.isShutdown()) {
                IterableLogger.e(TAG, "Cannot process operations: executor unavailable");
                return false;
            }
            
            return true;
        }


        private void processQueuedOperations() {
            try {
                IterableLogger.d(TAG, "Starting to process queued operations");
                
                QueuedOperation operation;
                while ((operation = operations.poll()) != null) {
                    executeOperationOnMainThread(operation);
                }
                
                IterableLogger.d(TAG, "Finished processing queued operations");
            } finally {
                isProcessing = false;
                shutdownBackgroundExecutorAsync();
            }
        }

        private void executeOperationOnMainThread(QueuedOperation operation) {
            IterableLogger.d(TAG, "Executing queued operation: " + operation.getDescription());
            
            mainThreadHandler.post(() -> {
                try {
                    operation.execute();
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Failed to execute operation: " + operation.getDescription(), e);
                }
            });
        }

        int size() {
            return operations.size();
        }

        void clear() {
            operations.clear();
            isProcessing = false;
        }
    }

    // Background initialization infrastructure
    private static volatile ExecutorService backgroundExecutor;
    private static final Object initLock = new Object();

    static {
        backgroundExecutor = createExecutor();
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IterableBackgroundInit");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
    }

    private static final OperationQueue operationQueue = new OperationQueue();
    private static volatile boolean isInitializing = false;
    private static volatile boolean isBackgroundInitialized = false;
    private static final ConcurrentLinkedQueue<IterableInitializationCallback> pendingCallbacks = new ConcurrentLinkedQueue<>();

    /**
     * Initialize the Iterable SDK in the background to avoid ANRs.
     * This method returns immediately and performs all initialization work on a background thread.
     * Any API calls made before initialization completes will be queued and executed after initialization.
     *
     * @param context Application context
     * @param apiKey Iterable API key
     * @param config Optional configuration (can be null)
     * @param callback Optional callback for initialization completion (can be null)
     */
    static void initializeInBackground(@NonNull Context context,
                                     @NonNull String apiKey,
                                     @Nullable IterableConfig config,
                                     @Nullable IterableInitializationCallback callback) {
        if (context == null) {
            IterableLogger.e(TAG, "Context cannot be null, but reporting success");
            invokeCallbackOnMainThread(callback);
            return;
        }

        if (!startInitialization(context, apiKey, config, callback)) {
            return; // Already initialized or in progress
        }

        IterableLogger.d(TAG, "Starting background initialization");
        backgroundExecutor.execute(() -> runInitializationTask(context, apiKey, config, callback));
    }

    private static boolean startInitialization(@NonNull Context context,
                                             @NonNull String apiKey,
                                             @Nullable IterableConfig config,
                                             @Nullable IterableInitializationCallback callback) {
        synchronized (initLock) {
            if (isInitializing || isBackgroundInitialized) {
                handleDuplicateInitialization(callback);
                return false;
            }

            // Set initializing flag and configure SDK
            isInitializing = true;
            IterableApi.sharedInstance._applicationContext = context.getApplicationContext();
            IterableApi.sharedInstance._apiKey = apiKey;
            IterableApi.sharedInstance.config = (config != null) ? config : new IterableConfig.Builder().build();
            return true;
        }
    }

    private static void handleDuplicateInitialization(@Nullable IterableInitializationCallback callback) {
        IterableLogger.w(TAG, "Initialization already in progress or completed");
        if (callback != null) {
            if (isBackgroundInitialized) {
                // Already done, call immediately
                invokeCallbackOnMainThread(callback);
            } else {
                // Still running, queue for later
                pendingCallbacks.offer(callback);
            }
        }
    }

    private static void runInitializationTask(@NonNull Context context,
                                             @NonNull String apiKey,
                                             @Nullable IterableConfig config,
                                             @Nullable IterableInitializationCallback callback) {
        long startTime = System.currentTimeMillis();
        ExecutorService initExecutor = createInitExecutor();
        boolean initSucceeded = false;

        try {
            initSucceeded = performInitializationWithTimeout(context, apiKey, config, initExecutor, startTime);
        } finally {
            completeInitialization(callback, startTime, initSucceeded);
            shutdownExecutor(initExecutor);
        }
    }

    private static ExecutorService createInitExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IterableInit");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
    }

    /**
     * @return true if initialization succeeded, false if it timed out or failed
     */
    private static boolean performInitializationWithTimeout(@NonNull Context context,
                                                           @NonNull String apiKey,
                                                           @Nullable IterableConfig config,
                                                           ExecutorService initExecutor,
                                                           long startTime) {
        try {
            IterableLogger.d(TAG, "Starting initialization with " + INITIALIZATION_TIMEOUT_SECONDS + "s timeout");
            
            Future<?> initFuture = initExecutor.submit(() -> {
                IterableLogger.d(TAG, "Executing initialization on background thread");
                IterableApi.initialize(context, apiKey, config);
            });

            initFuture.get(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            long elapsed = System.currentTimeMillis() - startTime;
            IterableLogger.d(TAG, "Initialization completed successfully in " + elapsed + "ms");
            return true;

        } catch (TimeoutException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            IterableLogger.w(TAG, "Initialization timed out after " + elapsed + "ms, continuing anyway");
            initExecutor.shutdownNow();
            return false;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            IterableLogger.e(TAG, "Initialization error after " + elapsed + "ms, continuing anyway", e);
            return false;
        }
    }

    private static void completeInitialization(@Nullable IterableInitializationCallback callback,
                                              long startTime,
                                              boolean succeeded) {
        // Update state
        synchronized (initLock) {
            isBackgroundInitialized = true;
            isInitializing = false;
        }

        // Process queued operations on background thread, each operation runs on main thread
        operationQueue.processAll(backgroundExecutor);

        // Notify callbacks on main thread
        notifyInitializationComplete(callback, startTime, succeeded);
    }

    private static void notifyInitializationComplete(@Nullable IterableInitializationCallback callback,
                                                     long startTime,
                                                     boolean succeeded) {
        new Handler(Looper.getMainLooper()).post(() -> {
            long totalTime = System.currentTimeMillis() - startTime;
            if (succeeded) {
                IterableLogger.d(TAG, "Notifying callbacks after " + totalTime + "ms");
            } else {
                IterableLogger.w(TAG, "Notifying callbacks after timeout/error (" + totalTime + "ms)");
            }

            // Call the original callback
            invokeCallbackSafely(callback);

            // Call all pending callbacks from duplicate initialization attempts
            IterableInitializationCallback pending;
            while ((pending = pendingCallbacks.poll()) != null) {
                invokeCallbackSafely(pending);
            }
        });
    }


    private static void invokeCallbackSafely(@Nullable IterableInitializationCallback callback) {
        if (callback != null) {
            try {
                callback.onSDKInitialized();
            } catch (Exception e) {
                IterableLogger.e(TAG, "Exception in initialization callback", e);
            }
        }
    }


    private static void invokeCallbackOnMainThread(@Nullable IterableInitializationCallback callback) {
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> invokeCallbackSafely(callback));
        }
    }

    private static void shutdownExecutor(ExecutorService executor) {
        try {
            if (!executor.isShutdown()) {
                executor.shutdown();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if background initialization is in progress
     * @return true if initialization is currently running in background
     */
    static boolean isInitializingInBackground() {
        return isInitializing;
    }

    /**
     * Check if background initialization has completed
     * @return true if background initialization completed successfully
     */
    static boolean isBackgroundInitializationComplete() {
        return isBackgroundInitialized;
    }

    /**
     * Queue an operation if initialization is in progress, otherwise execute immediately
     * @param operation The operation to queue or execute
     * @return true if operation was queued, false if executed immediately
     */
    static boolean queueOrExecute(QueuedOperation operation) {
        synchronized (initLock) {
            if (isInitializing && !isBackgroundInitialized) {
                operationQueue.enqueue(operation);
                return true;
            }
        }
        // Execute immediately if not initializing
        operation.execute();
        return false;
    }

    /**
     * Convenient method for one-liner operation queuing
     * @param runnable The operation to execute
     * @param description Description for debugging
     */
    static void queueOrExecute(Runnable runnable, String description) {
        queueOrExecute(new QueuedOperation() {
            @Override
            public void execute() {
                runnable.run();
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
    }


    /**
     * Shutdown the background executor for proper cleanup
     * Should be called during application shutdown or for testing
     */
    @VisibleForTesting
    static void shutdownBackgroundExecutor() {
        synchronized (initLock) {
            if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
                backgroundExecutor.shutdown();
                try {
                    if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        backgroundExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    backgroundExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Shutdown the background executor asynchronously to avoid blocking the executor thread itself
     * Used internally after initialization completes
     */
    private static void shutdownBackgroundExecutorAsync() {
        // Schedule shutdown on a separate thread to avoid blocking the executor thread
        new Thread(() -> {
            synchronized (initLock) {
                if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
                    backgroundExecutor.shutdown();
                    try {
                        if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                            IterableLogger.w(TAG, "Background executor did not terminate gracefully, forcing shutdown");
                            backgroundExecutor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        IterableLogger.w(TAG, "Interrupted while waiting for executor termination");
                        backgroundExecutor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                    IterableLogger.d(TAG, "Background executor shutdown completed");
                }
            }
        }, "IterableExecutorShutdown").start();
    }

    /**
     * Register a callback to be notified when SDK initialization completes.
     * If the SDK is already initialized, the callback is invoked immediately.
     *
     * @param callback The callback to be notified when initialization completes
     */
    static void onSDKInitialized(@NonNull IterableInitializationCallback callback) {
        callbackManager.addSubscriber(callback);
    }

    /**
     * Notify that initialization has completed - called by IterableApi.initialize()
     */
    static void notifyInitializationComplete() {
        callbackManager.notifyInitializationComplete();
    }

    /**
     * Reset background initialization state - for testing only
     */
    @VisibleForTesting
    static void resetBackgroundInitializationState() {
        synchronized (initLock) {
            isInitializing = false;
            isBackgroundInitialized = false;
            operationQueue.clear();
            pendingCallbacks.clear();
            callbackManager.reset();

            // Recreate executor if it was shut down
            if (backgroundExecutor == null || backgroundExecutor.isShutdown()) {
                backgroundExecutor = createExecutor();
            }
        }
    }

    // ========================================
    // Test Support Methods
    // ========================================

    /**
     * Get the number of queued operations (for testing)
     */
    @VisibleForTesting
    static int getQueuedOperationCount() {
        return operationQueue.size();
    }

    /**
     * Clear all queued operations (for testing)
     */
    @VisibleForTesting
    static void clearQueuedOperations() {
        operationQueue.clear();
    }

    /**
     * Get descriptions of all queued operations (for testing PII masking)
     */
    @VisibleForTesting
    static List<String> getQueuedOperationDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (QueuedOperation op : operationQueue.operations) {
            descriptions.add(op.getDescription());
        }
        return descriptions;
    }
}

/**
 * Manages initialization callbacks for the Iterable SDK.
 * Supports multiple subscribers and ensures callbacks are called on the main thread.
 */
class IterableInitializationCallbackManager {
    private static final String TAG = "IterableInitCallbackMgr";

    // Thread-safe collections for callback management
    private final CopyOnWriteArraySet<IterableInitializationCallback> subscribers = new CopyOnWriteArraySet<>();
    private final ConcurrentLinkedQueue<IterableInitializationCallback> oneTimeCallbacks = new ConcurrentLinkedQueue<>();

    private volatile boolean isInitialized = false;
    private final Object initLock = new Object();

    /**
     * Add a callback that will be called every time initialization completes.
     * If initialization has already completed, the callback is called immediately.
     *
     * @param callback The callback to add (must not be null)
     */
    void addSubscriber(@NonNull IterableInitializationCallback callback) {
        if (callback == null) {
            IterableLogger.w(TAG, "Cannot add null callback subscriber");
            return;
        }

        subscribers.add(callback);

        // If already initialized, call immediately on main thread
        synchronized (initLock) {
            if (isInitialized) {
                callCallbackOnMainThread(callback, "subscriber (immediate)");
                subscribers.remove(callback); // Auto-remove after calling
            }
        }
    }

    /**
     * Add a one-time callback that will be called once when initialization completes.
     * If initialization has already completed, the callback is called immediately.
     * This is used for the callback parameter in initialize() methods.
     *
     * @param callback The one-time callback to add (can be null)
     */
    void addOneTimeCallback(@Nullable IterableInitializationCallback callback) {
        if (callback == null) {
            return;
        }

        synchronized (initLock) {
            if (isInitialized) {
                // Call immediately if already initialized
                callCallbackOnMainThread(callback, "one-time (immediate)");
            } else {
                // Queue for later
                oneTimeCallbacks.offer(callback);
            }
        }
    }

    /**
     * Notify all callbacks that initialization has completed.
     * This should be called once when initialization finishes.
     */
    void notifyInitializationComplete() {
        synchronized (initLock) {
            if (isInitialized) {
                IterableLogger.d(TAG, "notifyInitializationComplete called but already initialized");
                return;
            }
            isInitialized = true;
        }

        IterableLogger.d(TAG, "Notifying initialization completion to " +
                         subscribers.size() + " subscribers and " +
                         oneTimeCallbacks.size() + " one-time callbacks");

        // Notify all subscribers and clear the list (auto-remove after calling)
        for (IterableInitializationCallback callback : subscribers) {
            callCallbackOnMainThread(callback, "subscriber");
        }
        subscribers.clear(); // Auto-remove all subscribers after calling them

        // Notify and clear one-time callbacks
        IterableInitializationCallback oneTimeCallback;
        while ((oneTimeCallback = oneTimeCallbacks.poll()) != null) {
            callCallbackOnMainThread(oneTimeCallback, "one-time");
        }
    }

    /**
     * Reset the initialization state - for testing only
     */
    void reset() {
        synchronized (initLock) {
            isInitialized = false;
            subscribers.clear();
            oneTimeCallbacks.clear();
        }
    }

    /**
     * Helper method to ensure callbacks are called on the main thread
     */
    private void callCallbackOnMainThread(@NonNull IterableInitializationCallback callback, String type) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            try {
                callback.onSDKInitialized();
                IterableLogger.d(TAG, "Called " + type + " callback on main thread");
            } catch (Exception e) {
                IterableLogger.e(TAG, "Exception in " + type + " initialization callback", e);
            }
        } else {
            // Post to main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    callback.onSDKInitialized();
                    IterableLogger.d(TAG, "Called " + type + " callback via main thread handler");
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Exception in " + type + " initialization callback", e);
                }
            });
        }
    }
}
