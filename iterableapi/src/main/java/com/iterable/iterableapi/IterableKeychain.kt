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
    private var encryptionEnabled: Boolean = false
    private val encryptor = IterableDataEncryptor()

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    constructor(context: Context) {

        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        encryptionEnabled = true
        IterableLogger.v(TAG, "SharedPreferences being used with encryption: $encryptionEnabled")

        // Attempt migration from encrypted preferences
        IterableKeychainEncryptedDataMigrator(context, sharedPrefs).attemptMigration()
    }

    fun getEmail(): String? {
        val value = sharedPrefs.getString(emailKey, null)
        return if (encryptionEnabled && value != null) {
            encryptor.decrypt(value)
        } else {
            value
        }
    }

    fun saveEmail(email: String?) {
        val valueToSave = if (encryptionEnabled && email != null) {
            encryptor.encrypt(email)
        } else {
            email
        }
        sharedPrefs.edit()
            .putString(emailKey, valueToSave)
            .apply()
    }

    fun getUserId(): String? {
        return sharedPrefs.getString(userIdKey, null)
    }

    fun saveUserId(userId: String?) {
        sharedPrefs.edit()
            .putString(userIdKey, userId)
            .apply()
    }

    fun getAuthToken(): String? {
        return sharedPrefs.getString(authTokenKey, null)
    }

    fun saveAuthToken(authToken: String?) {
        sharedPrefs.edit()
            .putString(authTokenKey, authToken)
            .apply()
    }
}