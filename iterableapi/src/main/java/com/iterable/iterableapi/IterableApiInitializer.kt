package com.iterable.iterableapi

import kotlinx.coroutines.*

/**
 * Helper class to handle async operations during IterableApi initialization
 */
internal object IterableApiInitializer {
    
    /**
     * Retrieves email, userId, and authToken from keychain asynchronously and then
     * invokes the callback with the retrieved values on the main thread
     */
    @JvmStatic
    fun retrieveEmailAndUserIdAsync(
        keychain: IterableKeychain?,
        callback: (email: String?, userId: String?, authToken: String?) -> Unit
    ) {
        if (keychain == null) {
            IterableLogger.e("IterableApi", "retrieveEmailAndUserId: Shared preference creation failed. Could not retrieve email/userId")
            callback(null, null, null)
            return
        }
        
        // Use a background scope for the async keychain operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val email = keychain.getEmail()
                val userId = keychain.getUserId()
                val authToken = keychain.getAuthToken()
                
                // Switch back to main thread for the callback
                withContext(Dispatchers.Main) {
                    callback(email, userId, authToken)
                }
            } catch (e: Exception) {
                IterableLogger.e("IterableApi", "Error retrieving keychain data", e)
                withContext(Dispatchers.Main) {
                    callback(null, null, null)
                }
            }
        }
    }
} 