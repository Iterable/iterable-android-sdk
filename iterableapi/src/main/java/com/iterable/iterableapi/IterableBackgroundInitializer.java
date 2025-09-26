package com.iterable.iterableapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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

    /**
     * Queue for operations called before initialization completes
     */
    private static class OperationQueue {
        private final ConcurrentLinkedQueue<QueuedOperation> operations = new ConcurrentLinkedQueue<>();
        private volatile boolean isProcessing = false;

        void enqueue(QueuedOperation operation) {
            operations.offer(operation);
            IterableLogger.d(TAG, "Queued operation: " + operation.getDescription());
        }

        void processAll(ExecutorService executor) {
            if (isProcessing) return;
            isProcessing = true;

            executor.execute(() -> {
                QueuedOperation operation;
                while ((operation = operations.poll()) != null) {
                    try {
                        IterableLogger.d(TAG, "Executing queued operation: " + operation.getDescription());
                        operation.execute();
                    } catch (Exception e) {
                        IterableLogger.e(TAG, "Failed to execute queued operation", e);
                    }
                }
                isProcessing = false;

                // After processing all operations, shut down the executor
                IterableLogger.d(TAG, "All queued operations processed, shutting down background executor");
                shutdownBackgroundExecutorAsync();
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
        // Handle null context early - still report success but log error
        if (context == null) {
            IterableLogger.e(TAG, "Context cannot be null, but reporting success");
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(callback::onSDKInitialized);
            }
            return;
        }

        synchronized (initLock) {
            if (isInitializing || isBackgroundInitialized) {
                IterableLogger.w(TAG, "initializeInBackground called but initialization already in progress or completed");
                if (callback != null) {
                    if (isBackgroundInitialized) {
                        // Initialization already complete, call callback immediately
                        new Handler(Looper.getMainLooper()).post(callback::onSDKInitialized);
                    } else {
                        // Initialization in progress, queue callback for later
                        pendingCallbacks.offer(callback);
                    }
                }
                return;
            }

            // Set initializing flag and essential properties inside synchronized block
            isInitializing = true;
            IterableApi.sharedInstance._applicationContext = context.getApplicationContext();
            IterableApi.sharedInstance._apiKey = apiKey;
            IterableApi.sharedInstance.config = (config != null) ? config : new IterableConfig.Builder().build();
        }

        IterableLogger.d(TAG, "Starting background initialization");

        // Create a separate executor for the actual initialization to enable timeout
        ExecutorService initExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IterableInit");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });

        Runnable initTask = () -> {
            long startTime = System.currentTimeMillis();
            boolean initSucceeded = false;
            
            try {
                IterableLogger.d(TAG, "Starting initialization with " + INITIALIZATION_TIMEOUT_SECONDS + " second timeout");
                
                // Submit the actual initialization task
                Future<?> initFuture = initExecutor.submit(() -> {
                    IterableLogger.d(TAG, "Executing initialization on background thread");
                    IterableApi.initialize(context, apiKey, config);
                });
                
                // Wait for initialization with timeout
                initFuture.get(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                initSucceeded = true;
                
                long elapsedTime = System.currentTimeMillis() - startTime;
                IterableLogger.d(TAG, "Background initialization completed successfully in " + elapsedTime + "ms");
                
            } catch (TimeoutException e) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                IterableLogger.w(TAG, "Background initialization timed out after " + elapsedTime + "ms, continuing anyway");
                // Cancel the hanging initialization task
                initExecutor.shutdownNow();
                
            } catch (Exception e) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                IterableLogger.e(TAG, "Background initialization encountered error after " + elapsedTime + "ms, but continuing", e);
            }
            
            // Always mark as completed and call callbacks regardless of success/timeout/failure
            synchronized (initLock) {
                isBackgroundInitialized = true;
                isInitializing = false;
            }
            
            // Process any queued operations
            operationQueue.processAll(backgroundExecutor);
            
            // Notify completion on main thread (always success)
            final boolean finalInitSucceeded = initSucceeded;
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    long totalTime = System.currentTimeMillis() - startTime;
                    if (finalInitSucceeded) {
                        IterableLogger.d(TAG, "Initialization completed successfully, notifying callbacks after " + totalTime + "ms");
                    } else {
                        IterableLogger.w(TAG, "Initialization timed out or failed, but notifying callbacks anyway after " + totalTime + "ms");
                    }
                    
                    // Call the original callback directly
                    if (callback != null) {
                        try {
                            callback.onSDKInitialized();
                        } catch (Exception e) {
                            IterableLogger.e(TAG, "Exception in initialization callback", e);
                        }
                    }
                    
                    // Call all pending callbacks from concurrent initialization attempts
                    IterableInitializationCallback pendingCallback;
                    while ((pendingCallback = pendingCallbacks.poll()) != null) {
                        try {
                            pendingCallback.onSDKInitialized();
                        } catch (Exception e) {
                            IterableLogger.e(TAG, "Exception in pending initialization callback", e);
                        }
                    }
                    
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Exception in initialization completion notification", e);
                }
            });
            
             // Clean up the init executor
             try {
                 if (!initExecutor.isShutdown()) {
                     initExecutor.shutdown();
                     if (!initExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                         initExecutor.shutdownNow();
                     }
                 }
             } catch (InterruptedException e) {
                 initExecutor.shutdownNow();
                 Thread.currentThread().interrupt();
             }
        };

        backgroundExecutor.execute(initTask);
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
     * Get the number of operations currently queued
     * @return number of queued operations
     */
    @VisibleForTesting
    static int getQueuedOperationCount() {
        return operationQueue.size();
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
        
        // Notify all subscribers
        for (IterableInitializationCallback callback : subscribers) {
            callCallbackOnMainThread(callback, "subscriber");
        }
        
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
