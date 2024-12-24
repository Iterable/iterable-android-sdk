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

    private val TAG = "IterableKeychain"
    private var sharedPrefs: SharedPreferences
    private val encryptor: IterableDataEncryptor
    private val decryptionFailureHandler: IterableDecryptionFailureHandler?

    internal val emailKey = "iterable-email"
    internal val userIdKey = "iterable-user-id"
    internal val authTokenKey = "iterable-auth-token"

    constructor(context: Context, decryptionFailureHandler: IterableDecryptionFailureHandler? = null) {
        this.decryptionFailureHandler = decryptionFailureHandler
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        encryptor = IterableDataEncryptor()
        IterableLogger.v(TAG, "SharedPreferences being used with encryption")

        try {
            // Attempt migration from encrypted preferences
            IterableKeychainEncryptedDataMigrator(context, sharedPrefs, this).attemptMigration()
        } catch (e: IterableKeychainEncryptedDataMigrator.MigrationException) {
            IterableLogger.w(TAG, "Migration failed, clearing data", e)
            handleDecryptionError(e)
        }
    }

    private fun handleDecryptionError(e: Exception? = null) {
        IterableLogger.w(TAG, "Decryption failed, clearing all data and regenerating key")
        sharedPrefs.edit()
            .remove(emailKey)
            .remove(userIdKey)
            .remove(authTokenKey)
            .apply()
        
        encryptor.clearKeyAndData(sharedPrefs)
        decryptionFailureHandler?.onDecryptionFailed(e ?: Exception("Unknown decryption error"))
    }

    fun getEmail(): String? {
        return try {
            sharedPrefs.getString(emailKey, null)?.let { encryptor.decrypt(it) }
        } catch (e: IterableDataEncryptor.DecryptionException) {
            handleDecryptionError(e)
            null
        }
    }

    fun saveEmail(email: String?) {
        sharedPrefs.edit()
            .putString(emailKey, email?.let { encryptor.encrypt(it) })
            .apply()
    }

    fun getUserId(): String? {
        return try {
            sharedPrefs.getString(userIdKey, null)?.let { encryptor.decrypt(it) }
        } catch (e: IterableDataEncryptor.DecryptionException) {
            handleDecryptionError(e)
            null
        }
    }

    fun saveUserId(userId: String?) {
        sharedPrefs.edit()
            .putString(userIdKey, userId?.let { encryptor.encrypt(it) })
            .apply()
    }

    fun getAuthToken(): String? {
        return try {
            sharedPrefs.getString(authTokenKey, null)?.let { encryptor.decrypt(it) }
        } catch (e: IterableDataEncryptor.DecryptionException) {
            handleDecryptionError(e)
            null
        }
    }

    fun saveAuthToken(authToken: String?) {
        sharedPrefs.edit()
            .putString(authTokenKey, authToken?.let { encryptor.encrypt(it) })
            .apply()
    }
}
