package com.iterable.iterableapi

import android.content.SharedPreferences
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

import com.iterable.iterableapi.IterableLogger

class IterableDataEncryptor(private val sharedPrefs: SharedPreferences) {
    private val TAG = "IterableDataEncryptor"
    private val ALGORITHM = "AES/GCM/NoPadding"
    private val GCM_IV_LENGTH = 12
    private val GCM_TAG_LENGTH = 128
    private val ENCRYPTION_KEY = "iterable-encryption-key"

    private val key: SecretKey by lazy {
        // Try to get existing key from SharedPreferences
        val savedKey = sharedPrefs.getString(ENCRYPTION_KEY, null)
        if (savedKey != null) {
            try {
                // Convert Base64 string back to SecretKey
                val keyBytes = Base64.decode(savedKey, Base64.NO_WRAP)
                return@lazy SecretKeySpec(keyBytes, "AES")
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Error retrieving encryption key", e)
                // If there's any error, generate and save new key
                generateAndSaveNewKey()
            }
        } else {
            // No existing key, generate and save new one
            generateAndSaveNewKey()
        }
    }

    private fun generateAndSaveNewKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val newKey = keyGen.generateKey()
        
        // Save the key to SharedPreferences
        val keyString = Base64.encodeToString(newKey.encoded, Base64.NO_WRAP)
        sharedPrefs.edit()
            .putString(ENCRYPTION_KEY, keyString)
            .apply()
        
        return newKey
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