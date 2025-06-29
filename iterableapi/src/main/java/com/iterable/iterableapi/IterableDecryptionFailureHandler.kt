package com.iterable.iterableapi

/**
 * Interface for handling decryption failures
 */
interface IterableDecryptionFailureHandler {
    /**
     * Called when a decryption failure occurs
     * @param exception The exception that caused the decryption failure
     */
    fun onDecryptionFailed(exception: Exception)
}