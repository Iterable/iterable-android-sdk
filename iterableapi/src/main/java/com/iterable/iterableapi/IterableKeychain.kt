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
    private var encryptionEnabled: Boolean = true
    private val encryptor: IterableDataEncryptor

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    constructor(context: Context) {
        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        encryptor = IterableDataEncryptor(sharedPrefs)
        IterableLogger.v(TAG, "SharedPreferences being used with encoding")

        // Attempt migration from encrypted preferences
        IterableKeychainEncryptedDataMigrator(context, sharedPrefs).attemptMigration()
    }

    fun getEmail(): String? {
        return sharedPrefs.getString(emailKey, null)?.let { encryptor.decrypt(it) }
    }

    fun saveEmail(email: String?) {
        sharedPrefs.edit()
            .putString(emailKey, email?.let { encryptor.encrypt(it) })
            .apply()
    }

    fun getUserId(): String? {
        return sharedPrefs.getString(userIdKey, null)?.let { encryptor.decrypt(it) }
    }

    fun saveUserId(userId: String?) {
        sharedPrefs.edit()
            .putString(userIdKey, userId?.let { encryptor.encrypt(it) })
            .apply()
    }

    fun getAuthToken(): String? {
        return sharedPrefs.getString(authTokenKey, null)?.let { encryptor.decrypt(it) }
    }

    fun saveAuthToken(authToken: String?) {
        sharedPrefs.edit()
            .putString(authTokenKey, authToken?.let { encryptor.encrypt(it) })
            .apply()
    }
}
