package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Base64
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.isNull
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.clearInvocations
import org.mockito.ArgumentMatchers.matches
import org.mockito.Mockito.never

class IterableKeychainTest {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockSharedPrefs: SharedPreferences
    @Mock private lateinit var mockEditor: SharedPreferences.Editor
    @Mock private lateinit var mockDecryptionFailureHandler: IterableDecryptionFailureHandler
    @Mock private lateinit var mockEncryptor: IterableDataEncryptor

    private lateinit var keychain: IterableKeychain
    private lateinit var mockedLog: MockedStatic<Log>
    private lateinit var mockedBase64: MockedStatic<Base64>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // Mock Android Log
        mockedLog = mockStatic(Log::class.java)
        
        // Mock Android Base64
        mockedBase64 = mockStatic(Base64::class.java)
        `when`(Base64.encodeToString(any(), anyInt())).thenReturn("mocked_base64_string")
        
        `when`(mockContext.getSharedPreferences(
            any<String>(),
            eq(Context.MODE_PRIVATE)
        )).thenReturn(mockSharedPrefs)
        
        `when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any<String>(), any())).thenReturn(mockEditor)
        `when`(mockEditor.remove(any<String>())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(any<String>(), anyBoolean())).thenReturn(mockEditor)

        // Mock migration-related SharedPreferences calls
        `when`(mockSharedPrefs.contains(any<String>())).thenReturn(false)
        // Mock encryption flag to be true by default
        `when`(mockSharedPrefs.getBoolean(eq("iterable-encryption-enabled"), anyBoolean())).thenReturn(true)
        `when`(mockSharedPrefs.getString(any<String>(), any())).thenReturn(null)
        
        // Mock editor.apply() to do nothing
        Mockito.doNothing().`when`(mockEditor).apply()

        // Create keychain with encryption enabled (default)
        keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler
        )
        // Directly set the mock encryptor
        keychain.encryptor = mockEncryptor

        // Setup encrypt/decrypt behavior
        `when`(mockEncryptor.encrypt(any())).thenAnswer { invocation ->
            val input = invocation.arguments[0] as String?
            input?.let { "encrypted_$it" }
        }
        
        `when`(mockEncryptor.decrypt(any())).thenAnswer { invocation ->
            val encrypted = invocation.arguments[0] as String?
            if (encrypted == null) {
                null
            } else if (encrypted.startsWith("encrypted_")) {
                encrypted.substring("encrypted_".length)
            } else {
                throw IterableDataEncryptor.DecryptionException("Invalid encrypted value")
            }
        }

        // Reset the verification count after setup
        Mockito.clearInvocations(mockEditor)
    }

    @After
    fun tearDown() {
        mockedLog.close()
        mockedBase64.close()
    }

    @Test
    fun testSaveAndGetEmail() {
        val testEmail = "test@example.com"
        
        // Update mock to return the encrypted value
        `when`(mockSharedPrefs.getString(eq("iterable-email"), isNull()))
            .thenReturn("encrypted_$testEmail")
        
        keychain.saveEmail(testEmail)
        
        verify(mockEditor).putString(eq("iterable-email"), eq("encrypted_$testEmail"))
        verify(mockEditor).apply()
        
        val retrievedEmail = keychain.getEmail()
        assertEquals(testEmail, retrievedEmail)
    }

    @Test
    fun testSaveAndGetUserId() {
        val testUserId = "user123"
        
        // Update mock to return the encrypted value
        `when`(mockSharedPrefs.getString(eq("iterable-user-id"), isNull()))
            .thenReturn("encrypted_$testUserId")
        
        keychain.saveUserId(testUserId)
        
        verify(mockEditor).putString(eq("iterable-user-id"), eq("encrypted_$testUserId"))
        verify(mockEditor).apply()
        
        val retrievedUserId = keychain.getUserId()
        assertEquals(testUserId, retrievedUserId)
    }

    @Test
    fun testSaveAndGetAuthToken() {
        val testToken = "auth-token-123"
        
        // Update mock to return the encrypted value
        `when`(mockSharedPrefs.getString(eq("iterable-auth-token"), isNull()))
            .thenReturn("encrypted_$testToken")
        
        keychain.saveAuthToken(testToken)
        
        verify(mockEditor).putString(eq("iterable-auth-token"), eq("encrypted_$testToken"))
        verify(mockEditor).apply()
        
        val retrievedToken = keychain.getAuthToken()
        assertEquals(testToken, retrievedToken)
    }

    @Test
    fun testDecryptionFailure() {
        // Setup mock to throw runtime exception instead
        `when`(mockEncryptor.decrypt(any())).thenAnswer { 
            throw RuntimeException("Test decryption failed")
        }
        `when`(mockSharedPrefs.getString(eq("iterable-email"), isNull()))
            .thenReturn("any_encrypted_value")

        val result = keychain.getEmail()

        // Verify data was cleared
        verify(mockEditor).remove("iterable-email")
        verify(mockEditor).remove("iterable-user-id")
        verify(mockEditor).remove("iterable-auth-token")
        verify(mockEditor).apply()

        // Verify failure handler was called with any exception
        verify(mockDecryptionFailureHandler).onDecryptionFailed(any())

        assertNull(result)
    }

    @Test
    fun testDecryptionFailureForAllOperations() {
        // Setup mock to throw runtime exception
        `when`(mockEncryptor.decrypt(any())).thenAnswer { 
            throw RuntimeException("Test decryption failed")
        }
        `when`(mockSharedPrefs.getString(any(), isNull())).thenReturn("any_encrypted_value")
        
        // Test all getter methods
        assertNull(keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
        
        // Verify failure handler was called exactly once for each operation
        verify(mockDecryptionFailureHandler, times(1)).onDecryptionFailed(any())
    }

    @Test
    fun testSaveNullValues() {
        keychain.saveEmail(null)
        keychain.saveUserId(null)
        keychain.saveAuthToken(null)

        // Verify remove calls for both key and plaintext flag
        verify(mockEditor, times(6)).remove(any())  // 2 removes per save (key + plaintext flag)
        verify(mockEditor, times(3)).remove(matches(".*_plaintext"))
        // Verify exactly one apply call for each save operation
        verify(mockEditor, times(3)).apply()
    }

    @Test
    fun testConcurrentAccess() {
        val testEmail = "test@example.com"
        val threads = mutableListOf<Thread>()
        val exceptions = mutableListOf<Exception>()
        
        // Simulate multiple threads accessing keychain
        for (i in 1..5) {
            threads.add(Thread {
                try {
                    keychain.saveEmail(testEmail)
                    assertEquals(testEmail, keychain.getEmail())
                } catch (e: Exception) {
                    exceptions.add(e)
                }
            })
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        assertTrue("Concurrent access caused exceptions: $exceptions", exceptions.isEmpty())
    }

    @Test
    fun testMigrationFailure() {
        // Mock migration to throw exception
        val mockMigrator = mock(IterableKeychainEncryptedDataMigrator::class.java)
        val migrationException = RuntimeException("Test migration failed")
        
        doThrow(migrationException).`when`(mockMigrator).attemptMigration()
        
        // Create new keychain with mock migrator
        keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler,
            mockMigrator
        )
        
        // Verify data was cleared
        verify(mockEditor).remove(eq("iterable-email"))
        verify(mockEditor).remove(eq("iterable-user-id"))
        verify(mockEditor).remove(eq("iterable-auth-token"))
        verify(mockEditor).apply()
        
        // Verify failure handler was called
        verify(mockDecryptionFailureHandler).onDecryptionFailed(migrationException)
    }

    @Test
    fun testMigrationOnlyAttemptedOnce() {
        // Create mock migrator
        val mockMigrator = mock(IterableKeychainEncryptedDataMigrator::class.java)
        // First check returns false, subsequent checks return true
        `when`(mockMigrator.isMigrationCompleted())
            .thenReturn(false)  // first call
        
        // First initialization
        keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler,
            mockMigrator
        )

        `when`(mockMigrator.isMigrationCompleted())
            .thenReturn(true)  // subsequent calls
        
        // Second initialization
        keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler,
            mockMigrator
        )
        
        // Verify attemptMigration was called exactly once
        verify(mockMigrator, times(1)).attemptMigration()
    }

    @Test
    fun testEncryptionAndDecryptionFailure() {
        // Setup encryption and decryption to fail
        `when`(mockEncryptor.encrypt(any())).thenThrow(RuntimeException("Simulated encryption failure"))
        `when`(mockEncryptor.decrypt(any())).thenThrow(RuntimeException("Simulated decryption failure"))
        
        val testData = "test data for encryption failure"
        
        // Save data - should fall back to plaintext
        keychain.saveUserId(testData)

        // Verify plaintext save operations
        verify(mockEditor).putString(eq("iterable-user-id"), eq(testData))
        verify(mockEditor).putBoolean(eq("iterable-user-id_plaintext"), eq(true))

        // Setup SharedPreferences to return the saved data
        `when`(mockSharedPrefs.getString(eq("iterable-user-id"), isNull())).thenReturn(testData)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-user-id_plaintext"), eq(false))).thenReturn(true)

        // Verify data can be retrieved
        assertEquals(testData, keychain.getUserId())

        // Verify encryption was attempted and failed
        verify(mockEncryptor).encrypt(eq(testData))
        verify(mockEncryptor, never()).decrypt(any()) // Should not attempt decryption for plaintext

        // Test null handling
        clearInvocations(mockEditor)
        keychain.saveUserId(null)
        verify(mockEditor).remove(eq("iterable-user-id"))
        verify(mockEditor).remove(eq("iterable-user-id_plaintext"))

        // Verify no more encryption attempts for null
        verify(mockEncryptor, never()).encrypt(isNull())
        verify(mockEncryptor, never()).decrypt(isNull())
    }

    @Test
    fun testEncryptionDisabled() {
        // Create a new keychain with encryption disabled
        val plaintextKeychain = IterableKeychain(
            mockContext,
            mockDecryptionFailureHandler,
            null,
            false  // encryption = false means encryption is disabled
        )
        
        val testEmail = "test@example.com"
        val testUserId = "user123"
        val testToken = "auth-token-123"
        
        // Mock the SharedPreferences to return plaintext values
        `when`(mockSharedPrefs.getString(eq("iterable-email"), isNull())).thenReturn(testEmail)
        `when`(mockSharedPrefs.getString(eq("iterable-user-id"), isNull())).thenReturn(testUserId)
        `when`(mockSharedPrefs.getString(eq("iterable-auth-token"), isNull())).thenReturn(testToken)

        // Mock plaintext flag checks to return true when encryption is disabled
        `when`(mockSharedPrefs.getBoolean(eq("iterable-email_plaintext"), eq(false))).thenReturn(true)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-user-id_plaintext"), eq(false))).thenReturn(true)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-auth-token_plaintext"), eq(false))).thenReturn(true)
        
        // Test save operations
        plaintextKeychain.saveEmail(testEmail)
        plaintextKeychain.saveUserId(testUserId)
        plaintextKeychain.saveAuthToken(testToken)
        
        // Verify values are stored as plaintext
        verify(mockEditor).putString(eq("iterable-email"), eq(testEmail))
        verify(mockEditor).putString(eq("iterable-user-id"), eq(testUserId))
        verify(mockEditor).putString(eq("iterable-auth-token"), eq(testToken))
        
        // Verify plaintext suffix flags are set for better compatibility
        verify(mockEditor).putBoolean(eq("iterable-email_plaintext"), eq(true))
        verify(mockEditor).putBoolean(eq("iterable-user-id_plaintext"), eq(true))
        verify(mockEditor).putBoolean(eq("iterable-auth-token_plaintext"), eq(true))
        
        // Test get operations
        assertEquals(testEmail, plaintextKeychain.getEmail())
        assertEquals(testUserId, plaintextKeychain.getUserId())
        assertEquals(testToken, plaintextKeychain.getAuthToken())
        
        // Verify IterableDataEncryptor was never created
        assertNull(plaintextKeychain.encryptor)
    }

    @Test
    fun testEncryptorInitializationFailureScenario() {
        // This test validates the MOB-11856 fix behavior
        // When IterableDataEncryptor() constructor throws an exception (like KeyStoreException on Nexus 5):
        // 1. Exception is caught in keychain initialization  
        // 2. handleDecryptionError() is called which disables encryption permanently
        // 3. App continues to work with plaintext storage
        
        // Test the actual scenario where encryption is disabled after init failure
        // First, mock SharedPreferences to return false for encryption-enabled (as it would after failure)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-encryption-enabled"), eq(true))).thenReturn(false)
        
        // Create keychain with encryption=true but it will be disabled due to the flag
        val keychainAfterInitFailure = IterableKeychain(
            mockContext,
            mockDecryptionFailureHandler,
            null,
            true  // encryption requested but will be disabled due to stored flag
        )
        
        val testEmail = "nexus5@example.com"
        val testUserId = "nexus5-user-123"
        
        // Verify that encryption is actually disabled (as it would be after init failure)
        assertNull("Encryptor should be null when encryption flag is disabled", keychainAfterInitFailure.encryptor)
        
        // Test that the keychain works in plaintext mode after the simulated failure
        keychainAfterInitFailure.saveEmail(testEmail)
        keychainAfterInitFailure.saveUserId(testUserId)
        
        // Verify plaintext storage calls
        verify(mockEditor).putString(eq("iterable-email"), eq(testEmail))
        verify(mockEditor).putString(eq("iterable-user-id"), eq(testUserId))
        verify(mockEditor).putBoolean(eq("iterable-email_plaintext"), eq(true))
        verify(mockEditor).putBoolean(eq("iterable-user-id_plaintext"), eq(true))
        
        // Mock retrieval to test that data can be read back
        `when`(mockSharedPrefs.getString(eq("iterable-email"), isNull())).thenReturn(testEmail)
        `when`(mockSharedPrefs.getString(eq("iterable-user-id"), isNull())).thenReturn(testUserId)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-email_plaintext"), eq(false))).thenReturn(true)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-user-id_plaintext"), eq(false))).thenReturn(true)
        
        // Verify retrieval works in plaintext mode
        assertEquals("Email should be retrieved from plaintext storage", testEmail, keychainAfterInitFailure.getEmail())
        assertEquals("UserId should be retrieved from plaintext storage", testUserId, keychainAfterInitFailure.getUserId())
        
        // This test validates that the MOB-11856 fix ensures:
        // - When encryption-enabled flag is false (set after KeyStore failure), encryption is disabled
        // - App continues to work with plaintext storage
        // - Normal save/retrieve operations work correctly
    }
} 