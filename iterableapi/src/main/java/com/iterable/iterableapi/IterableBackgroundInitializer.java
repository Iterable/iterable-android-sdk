package com.iterable.iterableapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handles background initialization of the Iterable SDK to prevent ANRs.
 * This class manages operation queuing, thread safety, and initialization state.
 */
class IterableBackgroundInitializer {
    private static final String TAG = "IterableBackgroundInit";
    
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
    private static final ConcurrentLinkedQueue<AsyncInitializationCallback> pendingCallbacks = new ConcurrentLinkedQueue<>();

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
                                     @Nullable AsyncInitializationCallback callback) {
        // Handle null context early
        if (context == null) {
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onInitializationFailed(new IllegalArgumentException("Context cannot be null"));
                });
            }
            return;
        }
        
        synchronized (initLock) {
            if (isInitializing || isBackgroundInitialized) {
                IterableLogger.w(TAG, "initializeInBackground called but initialization already in progress or completed");
                if (callback != null) {
                    if (isBackgroundInitialized) {
                        // Initialization already complete, call callback immediately
                        new Handler(Looper.getMainLooper()).post(callback::onInitializationComplete);
                    } else {
                        // Initialization in progress, queue callback for later
                        pendingCallbacks.offer(callback);
                    }
                }
                return;
            }
            
            // Set initializing flag inside synchronized block
            isInitializing = true;
        }
        
        // Set essential properties immediately to avoid NullPointerExceptions during queuing
        IterableApi.sharedInstance._applicationContext = context.getApplicationContext();
        IterableApi.sharedInstance._apiKey = apiKey;
        IterableApi.sharedInstance.config = (config != null) ? config : new IterableConfig.Builder().build();
        
        IterableLogger.d(TAG, "Starting background initialization");
        
        Runnable initTask = () -> {
            try {
                // Perform initialization on background thread
                IterableLogger.d(TAG, "Executing initialization on background thread");
                IterableApi.initialize(context, apiKey, config);
                
                // Mark as completed inside synchronized block
                synchronized (initLock) {
                    isBackgroundInitialized = true;
                    isInitializing = false;
                }
                
                IterableLogger.d(TAG, "Background initialization completed successfully");
                
                // Process any queued operations
                operationQueue.processAll(backgroundExecutor);
                
                // Notify completion on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        // Call the original callback
                        if (callback != null) {
                            callback.onInitializationComplete();
                        }
                        
                        // Call all pending callbacks from concurrent initialization attempts
                        AsyncInitializationCallback pendingCallback;
                        while ((pendingCallback = pendingCallbacks.poll()) != null) {
                            try {
                                pendingCallback.onInitializationComplete();
                            } catch (Exception e) {
                                IterableLogger.e(TAG, "Exception in pending initialization completion callback", e);
                            }
                        }
                    } catch (Exception e) {
                        IterableLogger.e(TAG, "Exception in initialization completion callback", e);
                    }
                });
                
            } catch (Exception e) {
                synchronized (initLock) {
                    isInitializing = false;
                }
                IterableLogger.e(TAG, "Background initialization failed", e);
                
                // Clear any queued operations on failure
                operationQueue.clear();
                
                // Notify failure on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        // Call the original callback
                        if (callback != null) {
                            callback.onInitializationFailed(e);
                        }
                        
                        // Call all pending callbacks from concurrent initialization attempts
                        AsyncInitializationCallback pendingCallback;
                        while ((pendingCallback = pendingCallbacks.poll()) != null) {
                            try {
                                pendingCallback.onInitializationFailed(e);
                            } catch (Exception callbackException) {
                                IterableLogger.e(TAG, "Exception in pending initialization failure callback", callbackException);
                            }
                        }
                    } catch (Exception callbackException) {
                        IterableLogger.e(TAG, "Exception in initialization failure callback", callbackException);
                    }
                });
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
     * Reset background initialization state - for testing only
     */
    @VisibleForTesting
    static void resetBackgroundInitializationState() {
        synchronized (initLock) {
            isInitializing = false;
            isBackgroundInitialized = false;
            operationQueue.clear();
            pendingCallbacks.clear();
            
            // Recreate executor if it was shut down
            if (backgroundExecutor == null || backgroundExecutor.isShutdown()) {
                backgroundExecutor = createExecutor();
            }
        }
    }
}
