package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import com.iterable.iterableapi.IterableKeychainEncryptedDataMigrator
import com.iterable.iterableapi.IterableDataEncryptor

class IterableKeychain {
    companion object {
        private const val TAG = "IterableKeychain"
        
        // Keys for storing encrypted data
        private const val KEY_EMAIL = "iterable-email"
        private const val KEY_USER_ID = "iterable-user-id"
        private const val KEY_AUTH_TOKEN = "iterable-auth-token"
    }

    private var sharedPrefs: SharedPreferences
    internal lateinit var encryptor: IterableDataEncryptor
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
            dataMigrator.attemptMigration()
        } catch (e: Exception) {
            IterableLogger.w(TAG, "Migration failed, clearing data", e)
            handleDecryptionError(e)
        }
    }

    private fun handleDecryptionError(e: Exception? = null) {
        IterableLogger.w(TAG, "Decryption failed, clearing all data and regenerating key")
        sharedPrefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_TOKEN)
            .apply()
        
        encryptor.resetKeys()
        decryptionFailureHandler?.onDecryptionFailed(e ?: Exception("Unknown decryption error"))
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
