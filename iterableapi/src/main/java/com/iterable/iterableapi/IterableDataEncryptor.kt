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
import java.security.SecureRandom
import javax.crypto.spec.IvParameterSpec
import android.annotation.TargetApi

class IterableDataEncryptor {
    companion object {
        private const val TAG = "IterableDataEncryptor"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION_MODERN = "AES/GCM/NoPadding"
        private const val TRANSFORMATION_LEGACY = "AES/CBC/PKCS5Padding"
        private const val ITERABLE_KEY_ALIAS = "iterable_encryption_key"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val CBC_IV_LENGTH = 16
        private val TEST_KEYSTORE_PASSWORD = "test_password".toCharArray()

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
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM, KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_PKCS7)
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

    fun encrypt(value: String?): String? {
        if (value == null) return null

        try {
            val data = value.toByteArray(Charsets.UTF_8)
            val encryptedData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                encryptModern(data)
            } else {
                encryptLegacy(data)
            }

            // Combine isModern flag, IV length, IV, and encrypted data
            val combined = ByteArray(1 + 1 + encryptedData.iv.size + encryptedData.data.size)
            combined[0] = if (encryptedData.isModernEncryption) 1 else 0
            combined[1] = encryptedData.iv.size.toByte()  // Store IV length
            System.arraycopy(encryptedData.iv, 0, combined, 2, encryptedData.iv.size)
            System.arraycopy(encryptedData.data, 0, combined, 2 + encryptedData.iv.size, encryptedData.data.size)

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
            
            // Extract components
            val isModern = combined[0] == 1.toByte()
            val ivLength = combined[1].toInt()
            val iv = combined.copyOfRange(2, 2 + ivLength)
            val encrypted = combined.copyOfRange(2 + ivLength, combined.size)

            val encryptedData = EncryptedData(encrypted, iv, isModern)
            
            // If it's modern encryption and we're on an old device, fail fast
            if (isModern && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                throw DecryptionException("Modern encryption cannot be decrypted on legacy devices")
            }

            // Use the appropriate decryption method
            val decrypted = if (isModern) {
                decryptModern(encryptedData)
            } else {
                decryptLegacy(encryptedData)
            }

            return String(decrypted, Charsets.UTF_8)
        } catch (e: DecryptionException) {
            // Re-throw DecryptionException directly
            throw e
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Decryption failed", e)
            throw DecryptionException("Failed to decrypt data", e)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun encryptModern(data: ByteArray): EncryptedData {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return encryptLegacy(data)
        }
        
        val cipher = Cipher.getInstance(TRANSFORMATION_MODERN)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return EncryptedData(encrypted, iv, true)
    }

    private fun encryptLegacy(data: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION_LEGACY)
        val iv = generateIV(isModern = false)
        val spec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), spec)
        val encrypted = cipher.doFinal(data)
        return EncryptedData(encrypted, iv, false)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun decryptModern(encryptedData: EncryptedData): ByteArray {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            throw DecryptionException("Cannot decrypt modern encryption on legacy device")
        }
        
        val cipher = Cipher.getInstance(TRANSFORMATION_MODERN)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        return cipher.doFinal(encryptedData.data)
    }

    private fun decryptLegacy(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION_LEGACY)
        val spec = IvParameterSpec(encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        return cipher.doFinal(encryptedData.data)
    }

    private fun generateIV(isModern: Boolean = false): ByteArray {
        val length = if (isModern) GCM_IV_LENGTH else CBC_IV_LENGTH
        val iv = ByteArray(length)
        SecureRandom().nextBytes(iv)
        return iv
    }

    data class EncryptedData(
        val data: ByteArray,
        val iv: ByteArray,
        val isModernEncryption: Boolean
    )

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
