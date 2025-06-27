package com.iterable.iterableapi

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import com.iterable.iterableapi.util.DeviceInfoUtils

/**
 * Kotlin coroutine-based initialization helper for IterableApi
 * This allows us to modernize initialization while keeping the main API in Java
 */
internal object IterableApiInitializer {
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Performs async keychain loading using coroutines
     */
    @JvmStatic
    fun retrieveEmailAndUserIdAsync(
        api: IterableApi,
        keychain: IterableKeychain?,
        onComplete: (email: String?, userId: String?, authToken: String?) -> Unit
    ) {
        if (keychain == null) {
            IterableLogger.e("IterableApi", "retrieveEmailAndUserId: Keychain is null")
            onComplete(null, null, null)
            return
        }
        
        initScope.launch {
            try {
                val email = keychain.getEmail()
                val userId = keychain.getUserId()
                val authToken = keychain.getAuthToken()
                
                // Switch back to main thread for the callback
                withContext(Dispatchers.Main) {
                    onComplete(email, userId, authToken)
                }
            } catch (e: Exception) {
                IterableLogger.e("IterableApi", "retrieveEmailAndUserId: Failed to retrieve from keychain", e)
                withContext(Dispatchers.Main) {
                    onComplete(null, null, null)
                }
            }
        }
    }
    
    /**
     * Performs the full background initialization using coroutines
     */
    @JvmStatic
    fun doBackgroundInitializationAsync(
        api: IterableApi,
        context: Context,
        onComplete: () -> Unit
    ) {
        initScope.launch {
            try {
                IterableLogger.v("IterableApi", "Starting background initialization")
                
                // Wait for keychain data to load first
                val keychain = api.keychain
                if (keychain != null) {
                    val email = keychain.getEmail()
                    val userId = keychain.getUserId()
                    val authToken = keychain.getAuthToken()
                    
                    // Update API fields on main thread
                    withContext(Dispatchers.Main) {
                        api.setEmailInternal(email)
                        api.setUserIdInternal(userId)
                        api.setAuthTokenInternal(authToken)
                        
                        // Handle auth token refresh after values are loaded
                        if (api.config.authHandler != null) {
                            if (authToken != null) {
                                api.authManager.queueExpirationRefresh(authToken)
                            } else {
                                IterableLogger.d("IterableApi", "Auth token found as null. Rescheduling auth token refresh")
                                api.authManager.scheduleAuthTokenRefresh(api.authManager.nextRetryInterval, true, null)
                            }
                        }
                    }
                }

                // Load configuration
                withContext(Dispatchers.Main) {
                    IterableApi.loadLastSavedConfiguration(context)
                }
                
                // Mark as initialized and flush pending operations on main thread
                withContext(Dispatchers.Main) {
                    onComplete()
                }
                
            } catch (e: Exception) {
                IterableLogger.e("IterableApi", "Background initialization failed", e)
                // Even if initialization fails, mark as initialized to prevent hanging
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
} 