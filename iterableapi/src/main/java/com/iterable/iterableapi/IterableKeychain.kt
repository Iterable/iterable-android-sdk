package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IterableKeychain {
    companion object {
        private const val TAG = "IterableKeychain"
        const val KEY_EMAIL = "iterable-email"
		const val KEY_USER_ID = "iterable-user-id"
		const val KEY_AUTH_TOKEN = "iterable-auth-token"
        private const val PLAINTEXT_SUFFIX = "_plaintext"
        private const val CRYPTO_OPERATION_TIMEOUT_MS = 500L
        private const val KEY_ENCRYPTION_DISABLED = "iterable-encryption-disabled"
        
        private val cryptoExecutor = Executors.newSingleThreadExecutor()
    }

    private var sharedPrefs: SharedPreferences
    internal var encryptor: IterableDataEncryptor? = null
    private val decryptionFailureHandler: IterableDecryptionFailureHandler?
    private val encryptionDisabled: Boolean

    @JvmOverloads
    constructor(
        context: Context,
        decryptionFailureHandler: IterableDecryptionFailureHandler? = null,
        migrator: IterableKeychainEncryptedDataMigrator? = null,
        encryptionDisabled: Boolean = false
    ) {
        this.decryptionFailureHandler = decryptionFailureHandler
        this.encryptionDisabled = encryptionDisabled || sharedPrefs.getBoolean(KEY_ENCRYPTION_DISABLED, false)
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        
        if (encryptionDisabled) {
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
    }

    private fun <T> runWithTimeout(callable: Callable<T>): T {
        return cryptoExecutor.submit(callable).get(CRYPTO_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    private fun handleDecryptionError(e: Exception? = null) {
        IterableLogger.w(TAG, "Decryption failed, permanently disabling encryption for this device please login again"
        
        // Permanently disable encryption for this device
        sharedPrefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_TOKEN)
            .putBoolean(KEY_ENCRYPTION_DISABLED, true)
            .apply()

        encryptionDisabled = true

        try {
            encryptor?.let { runWithTimeout { it.resetKeys(); Unit } }
        } catch (ex: Exception) {
            IterableLogger.e(TAG, "Failed to reset keys with timeout", ex)
        }

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

    private fun secureGet(key: String): String? {
        val hasPlainText = sharedPrefs.getBoolean(key + PLAINTEXT_SUFFIX, false)
        if (encryptionDisabled) {
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

        if (encryptionDisabled) {
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

    fun getEmail() = secureGet(KEY_EMAIL)
    fun saveEmail(email: String?) = secureSave(KEY_EMAIL, email)

    fun getUserId() = secureGet(KEY_USER_ID)
    fun saveUserId(userId: String?) = secureSave(KEY_USER_ID, userId)

    fun getAuthToken() = secureGet(KEY_AUTH_TOKEN)
    fun saveAuthToken(authToken: String?) = secureSave(KEY_AUTH_TOKEN, authToken)
}