package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

// Helper class for immediate cache access
private class CompletedFuture<T>(private val value: T) : Future<T> {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
    override fun isCancelled(): Boolean = false
    override fun isDone(): Boolean = true
    override fun get(): T = value
    override fun get(timeout: Long, unit: TimeUnit): T = value
}

class IterableKeychain {
    companion object {
        private const val TAG = "IterableKeychain"
        const val KEY_EMAIL = "iterable-email"
		const val KEY_USER_ID = "iterable-user-id"
		const val KEY_AUTH_TOKEN = "iterable-auth-token"
        private const val PLAINTEXT_SUFFIX = "_plaintext"
        private const val CRYPTO_OPERATION_TIMEOUT_MS = 500L
        private const val KEY_ENCRYPTION_ENABLED = "iterable-encryption-enabled"
        
        private val cryptoExecutor = Executors.newSingleThreadExecutor()
    }

    private var sharedPrefs: SharedPreferences
    internal var encryptor: IterableDataEncryptor? = null
    private val decryptionFailureHandler: IterableDecryptionFailureHandler?
    private var encryption: Boolean
    
    // Cached futures for non-blocking access
    @Volatile private var cachedEmail: Future<String?>
    @Volatile private var cachedUserId: Future<String?>
    @Volatile private var cachedAuthToken: Future<String?>

    @JvmOverloads
    constructor(
        context: Context,
        decryptionFailureHandler: IterableDecryptionFailureHandler? = null,
        migrator: IterableKeychainEncryptedDataMigrator? = null,
        encryption: Boolean = true
    ) {
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        this.decryptionFailureHandler = decryptionFailureHandler
        this.encryption = encryption && sharedPrefs.getBoolean(KEY_ENCRYPTION_ENABLED, true)

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
                IterableLogger.w(TAG, "Migration failed, clearing data", e)
                handleDecryptionError(e)
            }
        }
        
        // Start background loading of cached values
        cachedEmail = cryptoExecutor.submit(Callable { retrieve(KEY_EMAIL) })
        cachedUserId = cryptoExecutor.submit(Callable { retrieve(KEY_USER_ID) })
        cachedAuthToken = cryptoExecutor.submit(Callable { retrieve(KEY_AUTH_TOKEN) })
    }

    private fun <T> runWithTimeout(callable: Callable<T>): T {
        return cryptoExecutor.submit(callable).get(CRYPTO_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    private fun handleDecryptionError(e: Exception? = null) {
        IterableLogger.w(TAG, "Decryption failed, permanently disabling encryption for this device. Please login again.")
        
        // Permanently disable encryption for this device
        sharedPrefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_TOKEN)
            .putBoolean(KEY_ENCRYPTION_ENABLED, false)
            .apply()

        encryption = false

        decryptionFailureHandler?.let { handler ->
            val exception = e ?: Exception("Unknown decryption error")
            try {
                val mainLooper = android.os.Looper.getMainLooper()
                if (mainLooper != null) {
                    android.os.Handler(mainLooper).post {
                        handler.onDecryptionFailed(exception)
                    }
                } else {
                    throw IllegalStateException("MainLooper is unavailable")
                }
            } catch (ex: Exception) {
                handler.onDecryptionFailed(exception)
            }
        }
    }

    private fun retrieve(key: String): String? {
        val hasPlainText = sharedPrefs.getBoolean(key + PLAINTEXT_SUFFIX, false)
        if (!encryption) {
            if (hasPlainText) {
                return sharedPrefs.getString(key, null)
            } else {
                return null
            }
        } else if (hasPlainText) {
            return sharedPrefs.getString(key, null)
        }
        
        val encryptedValue = sharedPrefs.getString(key, null) ?: return null
        return try {
            encryptor?.let { runWithTimeout { it.decrypt(encryptedValue) } }
        } catch (e: Exception) {
            handleDecryptionError(e)
            null
        }
    }

    private fun secureSave(key: String, value: String?) {
        val editor = sharedPrefs.edit()
        if (value == null) {
            editor.remove(key).remove(key + PLAINTEXT_SUFFIX).apply()
            return
        }

        if (!encryption) {
            editor.putString(key, value).putBoolean(key + PLAINTEXT_SUFFIX, true).apply()
            return
        }

        try {
            encryptor?.let {
                val encrypted = runWithTimeout { it.encrypt(value) }
                editor.putString(key, encrypted)
                    .remove(key + PLAINTEXT_SUFFIX)
                    .apply()
            }
        } catch (e: Exception) {
            handleDecryptionError(e)
            editor.putString(key, value)
                .putBoolean(key + PLAINTEXT_SUFFIX, true)
                .apply()
        }
    }

    // Helper method to get cached value without blocking
    private fun getCachedValue(future: Future<String?>, valueName: String): String? = try {
        if (future.isDone) future.get() else null
    } catch (e: Exception) {
        IterableLogger.w(TAG, "Failed to get cached $valueName", e)
        null
    }
    
    // Helper method to save value asynchronously
    private fun saveValueAsync(key: String, value: String?, updateCache: (Future<String?>) -> Unit) {
        // Create a completed future for immediate cache access
        val completedFuture = CompletedFuture(value)
        // Update cache immediately
        updateCache(completedFuture)
        // Save to storage asynchronously to avoid blocking
        cryptoExecutor.submit { secureSave(key, value) }
    }

    // Public API methods - clean and DRY
    fun getEmail(): String? = getCachedValue(cachedEmail, "email")
    
    fun saveEmail(email: String?) = saveValueAsync(KEY_EMAIL, email) { future ->
        cachedEmail = future
    }

    fun getUserId(): String? = getCachedValue(cachedUserId, "userId")
    
    fun saveUserId(userId: String?) = saveValueAsync(KEY_USER_ID, userId) { future ->
        cachedUserId = future
    }

    fun getAuthToken(): String? = getCachedValue(cachedAuthToken, "authToken")
    
    fun saveAuthToken(authToken: String?) = saveValueAsync(KEY_AUTH_TOKEN, authToken) { future ->
        cachedAuthToken = future
    }
    
    // Method to check if initialization is complete
    fun isReady(): Boolean = cachedEmail.isDone && cachedUserId.isDone && cachedAuthToken.isDone
}