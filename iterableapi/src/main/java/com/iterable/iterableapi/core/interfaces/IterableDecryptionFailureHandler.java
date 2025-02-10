package com.iterable.iterableapi.core.interfaces;

/**
 * Interface for handling decryption failures
 */
public interface IterableDecryptionFailureHandler {
    /**
     * Called when a decryption failure occurs
     * @param exception The exception that caused the decryption failure
     */
    void onDecryptionFailed(Exception exception);
}