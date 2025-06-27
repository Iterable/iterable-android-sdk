package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*

class IterableKeychain {
    companion object {
        private const val TAG = "IterableKeychain"
        const val KEY_EMAIL = "iterable-email"
        const val KEY_USER_ID = "iterable-user-id"
        const val KEY_AUTH_TOKEN = "iterable-auth-token"
        private const val PLAINTEXT_SUFFIX = "_plaintext"
        private const val CRYPTO_OPERATION_TIMEOUT_MS = 2000L
        private const val KEY_ENCRYPTION_ENABLED = "iterable-encryption-enabled"
    }

    private var sharedPrefs: SharedPreferences
    internal var encryptor: IterableDataEncryptor? = null
    private val decryptionFailureHandler: IterableDecryptionFailureHandler?
    private var encryption: Boolean
    private val ioDispatcher: CoroutineDispatcher
    
    // Background scope for I/O operations - can be managed externally
    private val backgroundScope: CoroutineScope
    
    // Simple cache - instant access, no blocking
    @Volatile private var cachedEmail: String? = null
    @Volatile private var cachedUserId: String? = null
    @Volatile private var cachedAuthToken: String? = null
    
    // Deferred for initialization completion
    private val initializationComplete: Deferred<Unit>

    @JvmOverloads
    constructor(
        context: Context,
        decryptionFailureHandler: IterableDecryptionFailureHandler? = null,
        migrator: IterableKeychainEncryptedDataMigrator? = null,
        encryption: Boolean = true,
        scope: CoroutineScope? = null,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        this.decryptionFailureHandler = decryptionFailureHandler
        this.encryption = encryption && sharedPrefs.getBoolean(KEY_ENCRYPTION_ENABLED, true)
        this.ioDispatcher = ioDispatcher
        
        // Use provided scope or create our own with SupervisorJob
        this.backgroundScope = scope ?: CoroutineScope(ioDispatcher + SupervisorJob())

        if (!encryption) {
            IterableLogger.v(TAG, "SharedPreferences being used without encryption")
        } else {
            encryptor = IterableDataEncryptor()
            IterableLogger.v(TAG, "SharedPreferences being used with encryption")

            try {
                val dataMigrator = migrator ?: IterableKeychainEncryptedDataMigrator(context, sharedPrefs, this)
                if (!dataMigrator.isMigrationCompleted()) {
                    dataMigrator.setMigrationCompletionCallback { error ->
                        error?.let {
                            IterableLogger.w(TAG, "Migration failed", it)
                            handleDecryptionError(Exception(it))
                        }
                    }
                    dataMigrator.attemptMigration()
                    IterableLogger.v(TAG, "Migration completed")
                }
            } catch (e: Exception) {
                IterableLogger.w(TAG, "Migration failed", e)
                handleDecryptionError(e)
            }
        }
        
        // Load cache in background
        initializationComplete = backgroundScope.async {
            cachedEmail = retrieve(KEY_EMAIL)
            cachedUserId = retrieve(KEY_USER_ID)
            cachedAuthToken = retrieve(KEY_AUTH_TOKEN)
        }
    }

    private fun handleDecryptionError(e: Exception) {
        IterableLogger.w(TAG, "Decryption failed", e)
        
        // Just call the failure handler - don't clear data
        decryptionFailureHandler?.let { handler ->
            try {
                val mainLooper = android.os.Looper.getMainLooper()
                if (mainLooper != null) {
                    android.os.Handler(mainLooper).post {
                        handler.onDecryptionFailed(e)
                    }
                } else {
                    handler.onDecryptionFailed(e)
                }
            } catch (ex: Exception) {
                handler.onDecryptionFailed(e)
            }
        }
    }

    private suspend fun retrieve(key: String): String? = withContext(ioDispatcher) {
        val hasPlainText = sharedPrefs.getBoolean(key + PLAINTEXT_SUFFIX, false)
        if (!encryption) {
            if (hasPlainText) {
                sharedPrefs.getString(key, null)
            } else {
                null
            }
        } else if (hasPlainText) {
            sharedPrefs.getString(key, null)
        } else {
            val encryptedValue = sharedPrefs.getString(key, null) ?: return@withContext null
            try {
                encryptor?.let { 
                    withTimeout(CRYPTO_OPERATION_TIMEOUT_MS) {
                        it.decrypt(encryptedValue)
                    }
                }
            } catch (e: Exception) {
                IterableLogger.w(TAG, "Failed to decrypt $key, clearing this value", e)
                // Clear this specific item and call failure handler
                sharedPrefs.edit().remove(key).apply()
                handleDecryptionError(e)
                null
            }
        }
    }

    private suspend fun secureSave(key: String, value: String?) = withContext(ioDispatcher) {
        val editor = sharedPrefs.edit()
        if (value == null) {
            editor.remove(key).remove(key + PLAINTEXT_SUFFIX).apply()
            return@withContext
        }

        if (!encryption) {
            editor.putString(key, value).putBoolean(key + PLAINTEXT_SUFFIX, true).apply()
            return@withContext
        }

        try {
            encryptor?.let {
                val encrypted = withTimeout(CRYPTO_OPERATION_TIMEOUT_MS) {
                    it.encrypt(value)
                }
                editor.putString(key, encrypted)
                    .remove(key + PLAINTEXT_SUFFIX)
                    .apply()
            }
        } catch (e: Exception) {
            IterableLogger.w(TAG, "Failed to encrypt $key, saving as plaintext", e)
            // Fallback to plaintext for this specific value
            editor.putString(key, value)
                .putBoolean(key + PLAINTEXT_SUFFIX, true)
                .apply()
        }
    }

    // Async getters - wait for initialization if needed
    suspend fun getEmail(): String? {
        initializationComplete.await()
        return cachedEmail
    }
    
    suspend fun getUserId(): String? {
        initializationComplete.await()
        return cachedUserId
    }

    suspend fun getAuthToken(): String? {
        initializationComplete.await()
        return cachedAuthToken
    }
    
    // Sync setters - instant cache update + background save
    fun saveEmail(email: String?) {
        cachedEmail = email
        backgroundScope.launch { secureSave(KEY_EMAIL, email) }
    }
    
    fun saveUserId(userId: String?) {
        cachedUserId = userId
        backgroundScope.launch { secureSave(KEY_USER_ID, userId) }
    }
    
    fun saveAuthToken(authToken: String?) {
        cachedAuthToken = authToken
        backgroundScope.launch { secureSave(KEY_AUTH_TOKEN, authToken) }
    }
}