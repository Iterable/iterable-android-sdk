package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences

class IterableKeychain {
    companion object {
        private const val TAG = "IterableKeychain"
        const val KEY_EMAIL = "iterable-email"
		const val KEY_USER_ID = "iterable-user-id"
		const val KEY_AUTH_TOKEN = "iterable-auth-token"
    }

    private var sharedPrefs: SharedPreferences
    internal var encryptor: IterableDataEncryptor
    private val decryptionFailureHandler: IterableDecryptionFailureHandler?

    @JvmOverloads
    constructor(
        context: Context,
        decryptionFailureHandler: IterableDecryptionFailureHandler? = null,
        migrator: IterableKeychainEncryptedDataMigrator? = null
    ) {
        this.decryptionFailureHandler = decryptionFailureHandler
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        encryptor = IterableDataEncryptor()
        IterableLogger.v(TAG, "SharedPreferences being used with encryption")

        try {
            val dataMigrator = migrator ?: IterableKeychainEncryptedDataMigrator(context, sharedPrefs, this)
            dataMigrator.setMigrationCompletionCallback { error ->
                error?.let {
                    IterableLogger.w(TAG, "Migration failed", it)
                    handleDecryptionError(Exception(it))
                }
            }
            dataMigrator.attemptMigration()
        } catch (e: Exception) {
            IterableLogger.w(TAG, "Migration failed, clearing data", e)
            handleDecryptionError(e)
        }

        IterableLogger.v(TAG, "Migration completed")
    }

    private fun handleDecryptionError(e: Exception? = null) {
        IterableLogger.w(TAG, "Decryption failed, clearing all data and regenerating key")
        sharedPrefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_TOKEN)
            .apply()

        encryptor.resetKeys()
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
        return try {
            sharedPrefs.getString(key, null)?.let { encryptor.decrypt(it) }
        } catch (e: Exception) {
            handleDecryptionError(e)
            null
        }
    }

    private fun secureSave(key: String, value: String?) {
        sharedPrefs.edit()
            .putString(key, value?.let { encryptor.encrypt(it) })
            .apply()
    }

    fun getEmail() = secureGet(KEY_EMAIL)
    fun saveEmail(email: String?) = secureSave(KEY_EMAIL, email)

    fun getUserId() = secureGet(KEY_USER_ID)
    fun saveUserId(userId: String?) = secureSave(KEY_USER_ID, userId)

    fun getAuthToken() = secureGet(KEY_AUTH_TOKEN)
    fun saveAuthToken(authToken: String?) = secureSave(KEY_AUTH_TOKEN, authToken)
}
