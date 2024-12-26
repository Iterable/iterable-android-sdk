package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.annotation.VisibleForTesting

class IterableKeychainEncryptedDataMigrator(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val keychain: IterableKeychain
) {
    private val TAG = "IterableKeychainMigrator"

    private val encryptedSharedPrefsFileName = "iterable-encrypted-shared-preferences"
    private val migrationStartedKey = "iterable-encrypted-migration-started"
    private val migrationCompletedKey = "iterable-encrypted-migration-completed"

    private var migrationCompletionCallback: ((Throwable?) -> Unit)? = null
    private val migrationLock = Object()

    class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private var migrationTimeoutMs = 10000L // Default 10 seconds

    @VisibleForTesting
    fun setMigrationTimeout(timeoutMs: Long) {
        migrationTimeoutMs = timeoutMs
    }

    fun attemptMigration() {
        synchronized(migrationLock) {
            // Skip if running in JVM (for tests) unless mockEncryptedPrefs is present
            if (isRunningInJVM() && mockEncryptedPrefs == null) {
                IterableLogger.v(TAG, "Running in JVM, skipping migration of encrypted shared preferences")
                markMigrationCompleted()
                migrationCompletionCallback?.invoke(null)
                return
            }

            // Skip if migration was already completed
            if (sharedPrefs.getBoolean(migrationCompletedKey, false)) {
                IterableLogger.v(TAG, "Migration was already completed, skipping")
                migrationCompletionCallback?.invoke(null)
                return
            }

            // Only attempt migration on Android M and above
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                markMigrationCompleted()
                migrationCompletionCallback?.invoke(null)
                return
            }

            // If previous migration was interrupted, mark as completed and notify via callback
            if (sharedPrefs.getBoolean(migrationStartedKey, false)) {
                IterableLogger.w(TAG, "Previous migration attempt was interrupted")
                markMigrationCompleted()
                val exception = MigrationException("Previous migration attempt was interrupted")
                migrationCompletionCallback?.invoke(exception)
                return
            }

            // Mark migration as started
            sharedPrefs.edit()
                .putBoolean(migrationStartedKey, true)
                .apply()

            // Move EncryptedSharedPreferences creation to background thread
            Thread {
                val prefs = mockEncryptedPrefs ?: run {
                    try {
                        val masterKeyAlias = MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()

                        EncryptedSharedPreferences.create(
                            context,
                            encryptedSharedPrefsFileName,
                            masterKeyAlias,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (prefs == null) {
                    markMigrationCompleted()
                    val migrationException = MigrationException("Failed to load EncryptedSharedPreferences")
                    migrationCompletionCallback?.invoke(migrationException)
                    return@Thread
                }

                val timeoutThread = Thread {
                    try {
                        Thread.sleep(migrationTimeoutMs)
                        // Only trigger timeout if not interrupted
                        if (!Thread.currentThread().isInterrupted) {
                            markMigrationCompleted()
                            migrationCompletionCallback?.invoke(
                                MigrationException("Migration timed out after ${migrationTimeoutMs}ms")
                            )
                        }
                    } catch (e: InterruptedException) {
                        // Thread was cancelled, do nothing
                    }
                }
                timeoutThread.start()

                try {
                    migrateData(prefs)
                    timeoutThread.interrupt() // Cancel timeout if successful
                    prefs.edit().clear().apply()
                    markMigrationCompleted()
                    migrationCompletionCallback?.invoke(null)
                } catch (e: Throwable) {
                    timeoutThread.interrupt() // Cancel timeout on error
                    IterableLogger.w(TAG, "Failed to migrate data", e)
                    markMigrationCompleted()
                    val migrationException = MigrationException("Failed to migrate data", e)
                    migrationCompletionCallback?.invoke(migrationException)
                }
            }.apply {
                name = "IterableKeychain-Migration"
                start()
            }
        }
    }

	private fun migrateData(encryptedPrefs: SharedPreferences) {
		// Tag for logging
		val TAG = "DataMigration"

		// Fetch and migrate email
		val email = encryptedPrefs.getString("iterable_email", null)
		if (email != null) {
			keychain.saveEmail(email)
			IterableLogger.d(TAG, "Email migrated: $email")
		} else {
			IterableLogger.d(TAG, "No email found to migrate.")
		}

		// Fetch and migrate user ID
		val userId = encryptedPrefs.getString("iterable_user_id", null)
		if (userId != null) {
			keychain.saveUserId(userId)
			IterableLogger.d(TAG, "User ID migrated: $userId")
		} else {
			IterableLogger.w(TAG, "No user ID found to migrate.")
		}

		// Fetch and migrate auth token
		val authToken = encryptedPrefs.getString("iterable_auth_token", null)
		if (authToken != null) {
			keychain.saveAuthToken(authToken)
			IterableLogger.d(TAG, "Auth token migrated: $authToken")
		} else {
			IterableLogger.d(TAG, "No auth token found to migrate.")
		}
	}

    private fun markMigrationCompleted() {
        sharedPrefs.edit()
            .putBoolean(migrationStartedKey, false)
            .putBoolean(migrationCompletedKey, true)
            .apply()
    }

    fun setMigrationCompletionCallback(callback: (Throwable?) -> Unit) {
        migrationCompletionCallback = callback
    }

    // Add a property for tests to inject mock encrypted preferences
    @VisibleForTesting
    var mockEncryptedPrefs: SharedPreferences? = null

    private fun isRunningInJVM(): Boolean {
        return System.getProperty("java.vendor")?.contains("Android") != true
    }
}