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
import android.annotation.TargetApi

class IterableDataEncryptor {
    companion object {
        private const val TAG = "IterableDataEncryptor"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ITERABLE_KEY_ALIAS = "iterable_encryption_key"
        private const val GCM_TAG_LENGTH = 128
        private val FALLBACK_KEYSTORE_PASSWORD = "test_password".toCharArray()

        private val keyStore: KeyStore by lazy {
            try {
                KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                    load(null)
                }
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Failed to initialize AndroidKeyStore", e)
                KeyStore.getInstance("PKCS12").apply {
                    load(null, FALLBACK_KEYSTORE_PASSWORD)
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
               keyStore.type == ANDROID_KEYSTORE
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun generateAndroidKeyStoreKey(): Unit? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            } else {
                null
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Failed to generate key using AndroidKeyStore", e)
            null
        }
    }

    private fun generateFallbackKey() {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()

        val keyEntry = KeyStore.SecretKeyEntry(secretKey)
        val protParam = if (keyStore.type == "PKCS12") {
            PasswordProtection(FALLBACK_KEYSTORE_PASSWORD)
        } else {
            null
        }
        keyStore.setEntry(ITERABLE_KEY_ALIAS, keyEntry, protParam)
    }

    private fun getKey(): SecretKey {
        val protParam = if (keyStore.type == "PKCS12") {
            PasswordProtection(FALLBACK_KEYSTORE_PASSWORD)
        } else {
            null
        }
        return (keyStore.getEntry(ITERABLE_KEY_ALIAS, protParam) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(value: String?): String? {
        if (value == null) return null

        try {
            val data = value.toByteArray(Charsets.UTF_8)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data)

            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)

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

            val ivLength = combined[0].toInt()
            val iv = combined.copyOfRange(1, 1 + ivLength)
            val encrypted = combined.copyOfRange(1 + ivLength, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
            val decrypted = cipher.doFinal(encrypted)

            return String(decrypted, Charsets.UTF_8)
        } catch (e: DecryptionException) {
            throw e
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Decryption failed", e)
            throw DecryptionException("Failed to decrypt data", e)
        }
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

    @VisibleForTesting
    fun getKeyStore(): KeyStore = keyStore
}
