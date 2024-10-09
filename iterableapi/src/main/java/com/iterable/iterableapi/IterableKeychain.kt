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
    private val userIdAnonKey = "iterable-user-id-anon"
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
            try {
                val masterKeyAlias = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                sharedPrefs = EncryptedSharedPreferences.create(
                    context,
                    encryptedSharedPrefsFileName,
                    masterKeyAlias,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                encryptionEnabled = true
            } catch (e: Throwable) {
                if (e is Error) {
                    IterableLogger.e(
                        TAG,
                        "EncryptionSharedPreference creation failed with Error. Attempting to continue"
                    )
                }

                if (encryptionEnforced) {
                    //TODO: In-memory or similar solution needs to be implemented in the future.
                    IterableLogger.w(
                        TAG,
                        "Encryption is enforced. PII will not be persisted due to EncryptionSharedPreference failure. Email/UserId and Auth token will have to be passed for every app session.",
                        e
                    )
                    throw e.fillInStackTrace()
                } else {
                    sharedPrefs = context.getSharedPreferences(
                        IterableConstants.SHARED_PREFS_FILE,
                        Context.MODE_PRIVATE
                    )
                    IterableLogger.w(
                        TAG,
                        "Using SharedPreference as EncryptionSharedPreference creation failed."
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
    fun getUserIdAnon(): String? {
        return sharedPrefs.getString(userIdAnonKey,null)
    }
    fun saveUserIdAnon(userId: String?) {
        sharedPrefs.edit()
            .putString(userIdAnonKey, userId)
            .apply()
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
        val sharedPrefsUserIdAnon = oldPrefs.getString(IterableConstants.SHARED_PREFS_USERIDANON_KEY, null)
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

        if (getUserIdAnon() == null && sharedPrefsUserIdAnon != null) {
            saveUserIdAnon(sharedPrefsUserIdAnon)
            editor.remove(IterableConstants.SHARED_PREFS_USERIDANON_KEY)
            IterableLogger.v(
                TAG,
                "UPDATED: migrated userIdAnon from SharedPreferences to IterableKeychain"
            )
        } else if (sharedPrefsUserIdAnon != null) {
            editor.remove(IterableConstants.SHARED_PREFS_USERIDANON_KEY)
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