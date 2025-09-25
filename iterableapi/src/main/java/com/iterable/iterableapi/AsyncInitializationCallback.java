package com.iterable.iterableapi;

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
}
