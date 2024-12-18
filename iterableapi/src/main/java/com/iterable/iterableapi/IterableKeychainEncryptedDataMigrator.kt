package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class IterableKeychainEncryptedDataMigrator(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val keychain: IterableKeychain
) {
    private val TAG = "IterableKeychainMigrator"
    
    private val encryptedSharedPrefsFileName = "iterable-encrypted-shared-preferences"
    private val migrationAttemptedKey = "iterable-encrypted-migration-attempted"
    private val migrationStartedKey = "iterable-encrypted-migration-started"
    private val migrationCompletedKey = "iterable-encrypted-migration-completed"

    private val emailKey = "iterable-email"
    private val userIdKey = "iterable-user-id"
    private val authTokenKey = "iterable-auth-token"

    fun attemptMigration() {
        // Skip if migration was already attempted and completed
        if (sharedPrefs.getBoolean(migrationCompletedKey, false)) {
            IterableLogger.v(TAG, "Migration was already completed, skipping")
            return
        }

        // Check if migration was started but not completed (potential crash during migration)
        if (sharedPrefs.getBoolean(migrationStartedKey, false)) {
            IterableLogger.w(TAG, "Previous migration attempt was interrupted, clearing data")
            keychain.handleDecryptionError()
            return
        }

        // Only attempt migration on Android M and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            markMigrationCompleted()
            return
        }

        // Mark migration as started
        sharedPrefs.edit()
            .putBoolean(migrationStartedKey, true)
            .apply()

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

                // Migrate data using keychain methods
                migrateData(encryptedPrefs)
                
                // Clear encrypted prefs after successful migration
                encryptedPrefs.edit().clear().apply()
                
                // Mark migration as completed
                markMigrationCompleted()
                
                IterableLogger.v(TAG, "Successfully migrated data from encrypted preferences")
            } catch (e: Throwable) {
                IterableLogger.w(TAG, "Failed to access encrypted preferences, skipping migration", e)
                markMigrationCompleted() // Mark as completed even on failure to prevent retries
            }
        }.start()
    }

    private fun migrateData(encryptedPrefs: SharedPreferences) {
        // Use keychain methods to ensure proper encryption
        encryptedPrefs.getString(emailKey, null)?.let { keychain.saveEmail(it) }
        encryptedPrefs.getString(userIdKey, null)?.let { keychain.saveUserId(it) }
        encryptedPrefs.getString(authTokenKey, null)?.let { keychain.saveAuthToken(it) }
    }

    private fun markMigrationCompleted() {
        sharedPrefs.edit()
            .putBoolean(migrationStartedKey, false)
            .putBoolean(migrationCompletedKey, true)
            .apply()
    }
} 