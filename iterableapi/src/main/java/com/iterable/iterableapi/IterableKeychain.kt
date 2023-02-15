package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class IterableKeychain {
    private var sharedPrefs: SharedPreferences

    private val encryptedSharedPrefsFileName = "iterable-encrypted-shared-preferences"

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    @RequiresApi(Build.VERSION_CODES.M)
    constructor(context: Context) {

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        sharedPrefs = try {
            EncryptedSharedPreferences.create(
                encryptedSharedPrefsFileName,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        } catch (e: Exception) {
            context.getSharedPreferences(encryptedSharedPrefsFileName, Context.MODE_PRIVATE)
        }
        sharedPrefs = EncryptedSharedPreferences.create(
            encryptedSharedPrefsFileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
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