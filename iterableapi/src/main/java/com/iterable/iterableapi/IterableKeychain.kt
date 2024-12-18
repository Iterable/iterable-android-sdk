package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import com.iterable.iterableapi.IterableKeychainEncryptedDataMigrator

class IterableKeychain {

    private val TAG = "IterableKeychain"
    private var sharedPrefs: SharedPreferences

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    private var encryptionEnabled = false

    constructor(context: Context, encryptionEnforced: Boolean) {

        sharedPrefs = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        IterableLogger.v(TAG, "SharedPreferences being used")

        // Attempt migration from encrypted preferences
        IterableKeychainEncryptedDataMigrator(context, sharedPrefs).attemptMigration()
    }

    fun getEmail(): String? {
        return sharedPrefs.getString(emailKey, null)
    }

    fun saveEmail(email: String?) {
        sharedPrefs.edit()
            .putString(emailKey, email)
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