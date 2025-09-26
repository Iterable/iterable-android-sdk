package com.iterable.iterableapi;

/**
 * Callback interface for Iterable SDK initialization completion.
 * This callback is called when initialization completes, regardless of whether it was
 * performed in the foreground or background.
 * 
 * Multiple parties can subscribe to initialization completion using 
 * {@link IterableApi#addInitializationCallback(IterableInitializationCallback)}
 */
public interface IterableInitializationCallback {
    /**
     * Called when Iterable SDK initialization has completed.
     * This method is always called on the main thread.
     */
    void onSDKInitialized();
}
