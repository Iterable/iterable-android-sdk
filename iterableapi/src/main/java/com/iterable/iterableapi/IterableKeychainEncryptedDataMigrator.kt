package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class IterableKeychainEncryptedDataMigrator(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
) {
    private val TAG = "IterableKeychainMigrator"
    
    private val encryptedSharedPrefsFileName = "iterable-encrypted-shared-preferences"
    private val migrationAttemptedKey = "iterable-encrypted-migration-attempted"
    
    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    fun attemptMigration() {
        // Skip if migration was already attempted
        if (sharedPrefs.getBoolean(migrationAttemptedKey, false)) {
            IterableLogger.v(TAG, "Migration was already attempted, skipping")
            return
        }

        // Mark that we attempted migration
        sharedPrefs.edit()
            .putBoolean(migrationAttemptedKey, true)
            .apply()

        // Only attempt migration on Android M and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        // Run migration in background thread
        Thread {
            try {
                val masterKeyAlias = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    encryptedSharedPrefsFileName,
                    masterKeyAlias,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                // Migrate data
                migrateData(encryptedPrefs)
                
                // Clear encrypted prefs after successful migration
                encryptedPrefs.edit().clear().apply()
                
                IterableLogger.v(TAG, "Successfully migrated data from encrypted preferences")
            } catch (e: Throwable) {
                IterableLogger.w(TAG, "Failed to access encrypted preferences, skipping migration", e)
            }
        }.start()
    }

    private fun migrateData(encryptedPrefs: SharedPreferences) {
        val email = encryptedPrefs.getString(emailKey, null)
        val userId = encryptedPrefs.getString(userIdKey, null)
        val authToken = encryptedPrefs.getString(authTokenKey, null)

        // Only migrate non-null values
        sharedPrefs.edit().apply {
            email?.let { putString(emailKey, it) }
            userId?.let { putString(userIdKey, it) }
            authToken?.let { putString(authTokenKey, it) }
        }.apply()
    }
} 