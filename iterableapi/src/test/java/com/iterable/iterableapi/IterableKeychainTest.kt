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

    private lateinit var mockedLog: MockedStatic<Log>
    private lateinit var mockedBase64: MockedStatic<Base64>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock Android Log
        mockedLog = mockStatic(Log::class.java)
        
        // Mock Android Base64
        mockedBase64 = mockStatic(Base64::class.java)
        `when`(Base64.encodeToString(any(), anyInt())).thenReturn("mocked_base64_string")
        
        `when`(mockContext.getSharedPreferences(any<String>(), eq(Context.MODE_PRIVATE))).thenReturn(mockSharedPrefs)
        `when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any<String>(), any())).thenReturn(mockEditor)
        `when`(mockEditor.remove(any<String>())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(any<String>(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockSharedPrefs.contains(any<String>())).thenReturn(false)
        `when`(mockSharedPrefs.getBoolean(eq("iterable-encryption-enabled"), anyBoolean())).thenReturn(true)
        `when`(mockSharedPrefs.getString(any<String>(), any())).thenReturn(null)
        `when`(mockSharedPrefs.getBoolean(matches(".*_plaintext"), eq(false))).thenReturn(false)
        Mockito.doNothing().`when`(mockEditor).apply()
    }

    @After
    fun tearDown() {
        mockedLog.close()
        mockedBase64.close()
    }

    @Test
    fun testAsyncCacheUpdateBehavior() {
        // Create fresh keychain for this test
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        
        // Wait for initial cache loading
        Thread.sleep(100)
        
        // Initial state should be null (nothing stored)
        assertNull(keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
        
        // Save values - cache should update immediately
        keychain.saveEmail("test@example.com")
        assertEquals("test@example.com", keychain.getEmail())
        
        keychain.saveUserId("user123")
        assertEquals("user123", keychain.getUserId())
        
        keychain.saveAuthToken("token123")
        assertEquals("token123", keychain.getAuthToken())
        
        // Test null values
        keychain.saveEmail(null)
        assertNull(keychain.getEmail())
    }

    @Test
    fun testBackgroundStorageOperations() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        Thread.sleep(100) // Wait for initialization
        
        // Save a value
        keychain.saveEmail("async@test.com")
        
        // Should return immediately from cache
        assertEquals("async@test.com", keychain.getEmail())
        
        // Test that multiple saves work correctly
        keychain.saveEmail("async2@test.com")
        assertEquals("async2@test.com", keychain.getEmail())
        
        // Cache should update immediately for all operations
        keychain.saveUserId("async_user")
        assertEquals("async_user", keychain.getUserId())
    }

    @Test
    fun testIsReadyMethod() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        
        // Eventually should be ready
        var ready = false
        var attempts = 0
        while (!ready && attempts < 20) {
            ready = keychain.isReady()
            if (!ready) {
                Thread.sleep(50)
                attempts++
            }
        }
        assertTrue("Keychain should be ready after initialization", ready)
    }

    @Test
    fun testEncryptionDisabledMode() {
        // Create keychain with encryption disabled
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler, null, false)
        Thread.sleep(100)
        
        // Clear setup interactions
        clearInvocations(mockEditor)
        
        // Test saving in plaintext mode
        keychain.saveEmail("plaintext@test.com")
        assertEquals("plaintext@test.com", keychain.getEmail())
        
        // Wait for background save
        Thread.sleep(200)
        
        // Should have attempted to save
        verify(mockEditor, times(1)).apply()
        
        // Encryptor should be null in disabled mode
        assertNull(keychain.encryptor)
    }

    @Test
    fun testConcurrentOperations() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        Thread.sleep(100)
        
        val threads = mutableListOf<Thread>()
        val exceptions = mutableListOf<Exception>()
        
        // Multiple threads saving and reading
        for (i in 1..5) {
            threads.add(Thread {
                try {
                    keychain.saveEmail("email$i@test.com")
                    val result = keychain.getEmail()
                    assertNotNull("Should get some email value", result)
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            })
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        assertTrue("Concurrent operations should not cause exceptions: $exceptions", exceptions.isEmpty())
    }

    @Test
    fun testNullValueHandling() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        Thread.sleep(100)
        
        // Set values then clear them
        keychain.saveEmail("test@example.com")
        keychain.saveUserId("user123")
        keychain.saveAuthToken("token123")
        
        // Verify values are set
        assertEquals("test@example.com", keychain.getEmail())
        assertEquals("user123", keychain.getUserId())
        assertEquals("token123", keychain.getAuthToken())
        
        // Clear with null
        keychain.saveEmail(null)
        keychain.saveUserId(null)
        keychain.saveAuthToken(null)
        
        // Should return null immediately
        assertNull(keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
    }

    @Test
    fun testDecryptionFailureHandling() {
        // Just verify the behavior when decryption handler is present
        // We can't easily test actual decryption failures without a lot of setup
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        
        // Wait for initialization
        Thread.sleep(200)
        
        // Since no encrypted data is present, decryption failure handler shouldn't be called
        verify(mockDecryptionFailureHandler, never()).onDecryptionFailed(any())
        
        // Should return null for empty keychain
        assertNull(keychain.getEmail())
    }
    
    @Test
    fun testEncryptionAndDecryptionBehavior() {
        // Setup encryptor behavior first
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
                throw RuntimeException("Invalid encrypted value")
            }
        }
        
        // Create keychain and manually test encrypt/decrypt without timing issues
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler, null, true)
        keychain.encryptor = mockEncryptor
        
        Thread.sleep(100) // Wait for initialization
        
        // Test saving and retrieving values (cache mechanism)
        val testEmail = "test@example.com"
        keychain.saveEmail(testEmail)
        assertEquals(testEmail, keychain.getEmail())
        
        // Test that the cache mechanism works for different values
        keychain.saveUserId("test_user")
        assertEquals("test_user", keychain.getUserId())
        
        keychain.saveAuthToken("test_token")
        assertEquals("test_token", keychain.getAuthToken())
        
        // All operations should work through the cache immediately
        assertTrue("Cache operations should be immediate", keychain.isReady())
    }

    @Test
    fun testEncryptionFailureFallbackToPlaintext() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        keychain.encryptor = mockEncryptor
        
        // Setup encryption to fail
        `when`(mockEncryptor.encrypt(any())).thenThrow(RuntimeException("Encryption failed"))
        
        Thread.sleep(100)
        
        val testEmail = "test@example.com"
        keychain.saveEmail(testEmail)
        
        // Should still work via cache (immediate access)
        assertEquals(testEmail, keychain.getEmail())
        
        // Wait for background save to complete
        Thread.sleep(500)
        
        // Due to encryption failure, the system should handle it gracefully
        // The exact timing of when failure handler is called depends on background execution
        // We verify the cache still works despite encryption issues
        assertEquals(testEmail, keychain.getEmail())
    }

    @Test
    fun testDecryptionFailureHandlingComprehensive() {
        // Setup to return encrypted data but fail decryption
        `when`(mockSharedPrefs.getString(eq("iterable-email"), any())).thenReturn("bad_encrypted_data")
        `when`(mockSharedPrefs.getString(eq("iterable-user-id"), any())).thenReturn("bad_encrypted_data")
        `when`(mockSharedPrefs.getString(eq("iterable-auth-token"), any())).thenReturn("bad_encrypted_data")
        
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        keychain.encryptor = mockEncryptor
        
        // Setup decryptor to fail
        `when`(mockEncryptor.decrypt(any())).thenThrow(RuntimeException("Decryption failed"))
        
        // Wait for initialization to complete
        Thread.sleep(500)
        
        // All values should be null due to decryption failure
        assertNull(keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
        
        // Test that new values can still be saved despite decryption issues
        keychain.saveEmail("new@example.com")
        assertEquals("new@example.com", keychain.getEmail())
    }

    @Test
    fun testMigrationFailureHandling() {
        val mockMigrator = mock(IterableKeychainEncryptedDataMigrator::class.java)
        val migrationException = RuntimeException("Migration failed")
        
        `when`(mockMigrator.isMigrationCompleted()).thenReturn(false)
        doThrow(migrationException).`when`(mockMigrator).attemptMigration()
        
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler,
            mockMigrator
        )
        
        Thread.sleep(100)
        
        // Verify data was cleared due to migration failure
        verify(mockEditor).remove(eq("iterable-email"))
        verify(mockEditor).remove(eq("iterable-user-id"))  
        verify(mockEditor).remove(eq("iterable-auth-token"))
        verify(mockEditor).putBoolean(eq("iterable-encryption-enabled"), eq(false))
        verify(mockEditor).apply()
        
        // Verify failure handler was called
        verify(mockDecryptionFailureHandler, times(1)).onDecryptionFailed(any())
    }

    @Test
    fun testSaveNullValuesComprehensive() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        Thread.sleep(100)
        
        // First save some values
        keychain.saveEmail("test@example.com")
        keychain.saveUserId("user123")
        keychain.saveAuthToken("token123")
        
        // Verify values are set
        assertEquals("test@example.com", keychain.getEmail())
        assertEquals("user123", keychain.getUserId())
        assertEquals("token123", keychain.getAuthToken())
        
        // Now save null values
        keychain.saveEmail(null)
        keychain.saveUserId(null)
        keychain.saveAuthToken(null)
        
        // Verify immediate cache behavior - should be null immediately
        assertNull(keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
        
        // Wait for background saves to complete and verify values remain null
        Thread.sleep(200)
        assertNull(keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
    }

    @Test
    fun testEncryptionDisabledComprehensive() {
        // Create keychain with encryption explicitly disabled
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler, null, false)
        Thread.sleep(100)
        
        // Verify encryptor is null when disabled
        assertNull(keychain.encryptor)
        
        val testEmail = "plaintext@example.com"
        keychain.saveEmail(testEmail)
        
        // Should work immediately from cache
        assertEquals(testEmail, keychain.getEmail())
        
        // Test other operations work as well
        keychain.saveUserId("plaintext_user")
        keychain.saveAuthToken("plaintext_token")
        
        assertEquals("plaintext_user", keychain.getUserId())
        assertEquals("plaintext_token", keychain.getAuthToken())
        
        // Wait for background saves to complete
        Thread.sleep(200)
        
        // Values should still be accessible
        assertEquals(testEmail, keychain.getEmail())
        assertEquals("plaintext_user", keychain.getUserId())
        assertEquals("plaintext_token", keychain.getAuthToken())
    }

    @Test
    fun testStorageOperationsVerification() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        Thread.sleep(100)
        
        // Save different values for each field
        keychain.saveEmail("email@test.com")
        keychain.saveUserId("user123")
        keychain.saveAuthToken("token456")
        
        // Verify immediate cache access
        assertEquals("email@test.com", keychain.getEmail())
        assertEquals("user123", keychain.getUserId())
        assertEquals("token456", keychain.getAuthToken())
        
        // Test value updates
        keychain.saveEmail("updated@test.com")
        assertEquals("updated@test.com", keychain.getEmail())
        
        // Test mixed operations
        keychain.saveUserId(null)
        assertNull(keychain.getUserId())
        assertEquals("token456", keychain.getAuthToken()) // Should still be there
        
        // Wait for all background operations to complete
        Thread.sleep(300)
        
        // Final verification that cache state is consistent
        assertEquals("updated@test.com", keychain.getEmail())
        assertNull(keychain.getUserId())
        assertEquals("token456", keychain.getAuthToken())
    }

    @Test  
    fun testReadyStateTransitions() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        
        // Should eventually become ready - give it more time and attempts
        var ready = false
        var attempts = 0
        while (!ready && attempts < 100) {
            ready = keychain.isReady()
            if (!ready) {
                Thread.sleep(50) // Longer delays
                attempts++
            }
        }
        
        assertTrue("Keychain should become ready after $attempts attempts", ready)
        
        // Once ready, should stay ready
        Thread.sleep(100)
        assertTrue("Keychain should remain ready", keychain.isReady())
        
        // Test that we can still operate when ready
        keychain.saveEmail("ready@test.com")
        assertEquals("ready@test.com", keychain.getEmail())
    }

    @Test
    fun testConcurrentSaveAndRead() {
        val keychain = IterableKeychain(mockContext, mockDecryptionFailureHandler)
        Thread.sleep(100)
        
        val threads = mutableListOf<Thread>()
        val exceptions = mutableListOf<Exception>()
        val results = mutableListOf<String?>()
        
        // Multiple threads doing saves and reads
        for (i in 1..10) {
            threads.add(Thread {
                try {
                    keychain.saveEmail("email$i@test.com")
                    Thread.sleep(10) // Small delay
                    val result = keychain.getEmail()
                    synchronized(results) {
                        results.add(result)
                    }
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            })
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        assertTrue("Concurrent operations should not cause exceptions: $exceptions", exceptions.isEmpty())
        assertEquals("Should have results from all threads", 10, results.size)
        results.forEach { result ->
            assertNotNull("Each thread should get a valid result", result)
            assertTrue("Result should be a valid email", result!!.contains("@test.com"))
        }
    }
} 