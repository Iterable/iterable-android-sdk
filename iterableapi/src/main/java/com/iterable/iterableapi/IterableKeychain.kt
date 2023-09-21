package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class IterableKeychain {

    private val TAG = "IterableKeychain"
    private var sharedPrefs: SharedPreferences

    private val encryptedSharedPrefsFileName = "iterable-encrypted-shared-preferences"

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    private var encryptionEnabled = false

    constructor(context: Context, encryptionEnforced: Boolean) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            encryptionEnabled = false
            sharedPrefs = context.getSharedPreferences(
                IterableConstants.SHARED_PREFS_FILE,
                Context.MODE_PRIVATE
            )
            IterableLogger.v(TAG, "SharedPreferences being used")
        } else {
            // See if EncryptedSharedPreferences can be created successfully
            val masterKeyAlias = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            try {
                sharedPrefs = EncryptedSharedPreferences.create(
                    context,
                    encryptedSharedPrefsFileName,
                    masterKeyAlias,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                encryptionEnabled = true
            } catch (e: Exception) {
                if (encryptionEnforced) {
                    //TODO: In memory Or similar solution needs to be implemented in future.
                    IterableLogger.e(TAG, "Error creating EncryptedSharedPreferences", e)
                    throw e.fillInStackTrace()
                } else {
                    sharedPrefs = context.getSharedPreferences(
                        IterableConstants.SHARED_PREFS_FILE,
                        Context.MODE_PRIVATE
                    )
                    encryptionEnabled = false
                }
            }

            //Try to migrate data from SharedPreferences to EncryptedSharedPreferences
            if (encryptionEnabled) {
                migrateAuthDataFromSharedPrefsToKeychain(context)
            }
        }
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun migrateAuthDataFromSharedPrefsToKeychain(context: Context) {
        val oldPrefs: SharedPreferences = context.getSharedPreferences(
            IterableConstants.SHARED_PREFS_FILE,
            Context.MODE_PRIVATE
        )
        val sharedPrefsEmail = oldPrefs.getString(IterableConstants.SHARED_PREFS_EMAIL_KEY, null)
        val sharedPrefsUserId = oldPrefs.getString(IterableConstants.SHARED_PREFS_USERID_KEY, null)
        val sharedPrefsAuthToken =
            oldPrefs.getString(IterableConstants.SHARED_PREFS_AUTH_TOKEN_KEY, null)
        val editor: SharedPreferences.Editor = oldPrefs.edit()
        if (getEmail() == null && sharedPrefsEmail != null) {
            saveEmail(sharedPrefsEmail)
            editor.remove(IterableConstants.SHARED_PREFS_EMAIL_KEY)
            IterableLogger.v(
                TAG,
                "UPDATED: migrated email from SharedPreferences to IterableKeychain"
            )
        } else if (sharedPrefsEmail != null) {
            editor.remove(IterableConstants.SHARED_PREFS_EMAIL_KEY)
        }
        if (getUserId() == null && sharedPrefsUserId != null) {
            saveUserId(sharedPrefsUserId)
            editor.remove(IterableConstants.SHARED_PREFS_USERID_KEY)
            IterableLogger.v(
                TAG,
                "UPDATED: migrated userId from SharedPreferences to IterableKeychain"
            )
        } else if (sharedPrefsUserId != null) {
            editor.remove(IterableConstants.SHARED_PREFS_USERID_KEY)
        }
        if (getAuthToken() == null && sharedPrefsAuthToken != null) {
            saveAuthToken(sharedPrefsAuthToken)
            editor.remove(IterableConstants.SHARED_PREFS_AUTH_TOKEN_KEY)
            IterableLogger.v(
                TAG,
                "UPDATED: migrated authToken from SharedPreferences to IterableKeychain"
            )
        } else if (sharedPrefsAuthToken != null) {
            editor.remove(IterableConstants.SHARED_PREFS_AUTH_TOKEN_KEY)
        }
        editor.apply()
    }
}