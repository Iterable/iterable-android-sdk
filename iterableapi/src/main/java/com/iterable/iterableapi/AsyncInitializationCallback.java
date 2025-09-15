package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Callback interface for background initialization completion.
 * All callbacks are executed on the main thread.
 */
public interface AsyncInitializationCallback {
    /**
     * Called on the main thread when initialization completes successfully.
     * At this point, all queued operations have been processed and the SDK is ready for use.
     */
    void onInitializationComplete();
    
    /**
     * Called on the main thread if initialization fails.
     * Any queued operations will be cleared when initialization fails.
     * 
     * @param exception The exception that caused initialization failure
     */
    void onInitializationFailed(@NonNull Exception exception);
}
