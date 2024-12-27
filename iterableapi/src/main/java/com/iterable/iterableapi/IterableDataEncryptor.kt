package com.iterable.iterableapi

import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.os.Build
import java.security.KeyStore.PasswordProtection
import androidx.annotation.VisibleForTesting

class IterableDataEncryptor {
    companion object {
        private const val TAG = "IterableDataEncryptor"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ITERABLE_KEY_ALIAS = "iterable_encryption_key"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private val TEST_KEYSTORE_PASSWORD = "test_password".toCharArray()

        // Make keyStore static so it's shared across instances
        private val keyStore: KeyStore by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                try {
                    KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                        load(null)
                    }
                } catch (e: Exception) {
                    IterableLogger.e(TAG, "Failed to initialize AndroidKeyStore", e)
                    KeyStore.getInstance("PKCS12").apply {
                        load(null, TEST_KEYSTORE_PASSWORD)
                    }
                }
            } else {
                KeyStore.getInstance("PKCS12").apply {
                    load(null, TEST_KEYSTORE_PASSWORD)
                }
            }
        }
    }

    init {
        if (!keyStore.containsAlias(ITERABLE_KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        try {
            if (canUseAndroidKeyStore()) {
                generateAndroidKeyStoreKey()?.let { return }
            }
            generateFallbackKey()
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Failed to generate key", e)
            throw e
        }
    }

    private fun canUseAndroidKeyStore(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
               keyStore.type == ANDROID_KEYSTORE
    }

    private fun generateAndroidKeyStoreKey(): Unit? {
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keySpec = KeyGenParameterSpec.Builder(
                ITERABLE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
            Unit
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Failed to generate key using AndroidKeyStore", e)
            null
        }
    }

    private fun generateFallbackKey() {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // 256-bit AES key
        val secretKey = keyGenerator.generateKey()

        val keyEntry = KeyStore.SecretKeyEntry(secretKey)
        val protParam = if (keyStore.type == "PKCS12") {
            PasswordProtection(TEST_KEYSTORE_PASSWORD)
        } else {
            null
        }
        keyStore.setEntry(ITERABLE_KEY_ALIAS, keyEntry, protParam)
    }

    private fun getKey(): SecretKey {
        val protParam = if (keyStore.type == "PKCS12") {
            PasswordProtection(TEST_KEYSTORE_PASSWORD)
        } else {
            null
        }
        return (keyStore.getEntry(ITERABLE_KEY_ALIAS, protParam) as KeyStore.SecretKeyEntry).secretKey
    }

    class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun resetKeys() {
        try {
            keyStore.deleteEntry(ITERABLE_KEY_ALIAS)
            generateKey()
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Failed to regenerate key", e)
        }
    }

    fun encrypt(value: String?): String? {
        if (value == null) return null

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())

            val iv = cipher.iv
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Encryption failed", e)
            throw e
        }
    }

    fun decrypt(value: String?): String? {
        if (value == null) return null

        try {
            val combined = Base64.decode(value, Base64.NO_WRAP)

            // Extract IV
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Decryption failed", e)
            throw DecryptionException("Failed to decrypt data", e)
        }
    }

    // Add this method for testing purposes
    @VisibleForTesting
    fun getKeyStore(): KeyStore = keyStore
}
