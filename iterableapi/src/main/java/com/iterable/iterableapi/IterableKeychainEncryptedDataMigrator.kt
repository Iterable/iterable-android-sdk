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
    private val migrationStartedKey = "iterable-encrypted-migration-started"
    private val migrationCompletedKey = "iterable-encrypted-migration-completed"

    class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun attemptMigration() {
        // Skip if migration was already completed
        if (sharedPrefs.getBoolean(migrationCompletedKey, false)) {
            IterableLogger.v(TAG, "Migration was already completed, skipping")
            return
        }

        // Only attempt migration on Android M and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            markMigrationCompleted()
            return
        }

        // If previous migration was interrupted, mark as completed and throw exception
        if (sharedPrefs.getBoolean(migrationStartedKey, false)) {
            IterableLogger.w(TAG, "Previous migration attempt was interrupted")
            markMigrationCompleted()
            throw MigrationException("Previous migration attempt was interrupted")
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
                IterableLogger.w(TAG, "Failed to access encrypted preferences", e)
                markMigrationCompleted() // Mark as completed even on failure
                throw MigrationException("Failed to migrate data", e)
            }
        }.start()
    }

    private fun migrateData(encryptedPrefs: SharedPreferences) {
        // Use keychain methods to ensure proper encryption
        encryptedPrefs.getString(keychain.emailKey, null)?.let { keychain.saveEmail(it) }
        encryptedPrefs.getString(keychain.userIdKey, null)?.let { keychain.saveUserId(it) }
        encryptedPrefs.getString(keychain.authTokenKey, null)?.let { keychain.saveAuthToken(it) }
    }

    private fun markMigrationCompleted() {
        sharedPrefs.edit()
            .putBoolean(migrationStartedKey, false)
            .putBoolean(migrationCompletedKey, true)
            .apply()
    }
} 