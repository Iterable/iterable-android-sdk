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

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    constructor(context: Context) {
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        encryptor = IterableDataEncryptor()
        IterableLogger.v(TAG, "SharedPreferences being used with encryption")

        // Attempt migration from encrypted preferences
        IterableKeychainEncryptedDataMigrator(context, sharedPrefs, this).attemptMigration()
    }

    fun handleDecryptionError() {
        IterableLogger.w(TAG, "Decryption failed, clearing all data and regenerating key")
        sharedPrefs.edit()
            .remove(emailKey)
            .remove(userIdKey)
            .remove(authTokenKey)
            .apply()
        
        encryptor.clearKeyAndData(sharedPrefs)
    }

    fun getEmail(): String? {
        return try {
            sharedPrefs.getString(emailKey, null)?.let { encryptor.decrypt(it) }
        } catch (e: IterableDataEncryptor.DecryptionException) {
            handleDecryptionError()
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
            handleDecryptionError()
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
            handleDecryptionError()
            null
        }
    }

    fun saveAuthToken(authToken: String?) {
        sharedPrefs.edit()
            .putString(authTokenKey, authToken?.let { encryptor.encrypt(it) })
            .apply()
    }
}
