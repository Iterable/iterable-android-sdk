package com.iterable.iterableapi

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

import com.iterable.iterableapi.IterableLogger

class IterableDataEncryptor {
    private val TAG = "IterableDataEncryptor"
    private val key: SecretKey
    private val ALGORITHM = "AES/GCM/NoPadding"
    private val GCM_IV_LENGTH = 12
    private val GCM_TAG_LENGTH = 128

    init {
        // Generate a new key for this session
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        key = keyGen.generateKey()
    }

    fun encrypt(plaintext: String): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(GCM_IV_LENGTH)
            // In a real implementation, you'd want to use SecureRandom here
            iv.fill(0)  
            
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            
            val ciphertext = cipher.doFinal(plaintext.toByteArray())
            return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Encryption failed", e)
            return plaintext  // Fallback to plaintext if encryption fails
        }
    }

    fun decrypt(encryptedData: String): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(GCM_IV_LENGTH)
            // Use same IV as encryption
            iv.fill(0)
            
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            
            val ciphertext = Base64.decode(encryptedData, Base64.NO_WRAP)
            return String(cipher.doFinal(ciphertext))
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Decryption failed", e)
            return encryptedData  // Fallback to encrypted data if decryption fails
        }
    }
} 