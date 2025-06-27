package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
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
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.clearInvocations
import org.mockito.ArgumentMatchers.matches
import org.mockito.Mockito.never

@OptIn(ExperimentalCoroutinesApi::class)
class IterableKeychainTest {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockSharedPrefs: SharedPreferences
    @Mock private lateinit var mockEditor: SharedPreferences.Editor
    @Mock private lateinit var mockDecryptionFailureHandler: IterableDecryptionFailureHandler
    @Mock private lateinit var mockEncryptor: IterableDataEncryptor

    private lateinit var mockedLog: MockedStatic<Log>
    private lateinit var mockedBase64: MockedStatic<Base64>
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Setup test coroutines
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        
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
    fun testBasicAsyncOperations() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            true, 
            testScope, 
            testDispatcher
        )
        
        // Wait for initialization to complete
        advanceUntilIdle()
        
        // Initially should be null (no stored data)
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
        
        // Advance coroutines to complete background saves
        advanceUntilIdle()
    }

    @Test
    fun testAsyncGettersWaitForInitialization() = testScope.runTest {
        // Setup some existing data
        `when`(mockSharedPrefs.getString(eq("iterable-email"), any())).thenReturn("existing@test.com")
        `when`(mockSharedPrefs.getBoolean(eq("iterable-email_plaintext"), eq(false))).thenReturn(true)
        
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            false, // No encryption for simpler test
            testScope, 
            testDispatcher
        )
        
        // Don't advance time yet - initialization should be pending
        
        // This should wait for initialization
        val emailDeferred = async { keychain.getEmail() }
        
        // Should not complete yet
        assertFalse(emailDeferred.isCompleted)
        
        // Now advance time to complete initialization
        advanceUntilIdle()
        
        // Now getter should complete with loaded value
        assertEquals("existing@test.com", emailDeferred.await())
    }

    @Test
    fun testSyncSettersUpdateCacheImmediately() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            false,
            testScope, 
            testDispatcher
        )
        
        advanceUntilIdle() // Complete initialization
        
        // Save should update cache immediately, even before background save
        keychain.saveEmail("instant@test.com")
        assertEquals("instant@test.com", keychain.getEmail())
        
        // Test rapid updates
        keychain.saveEmail("update1@test.com")
        assertEquals("update1@test.com", keychain.getEmail())
        
        keychain.saveEmail("update2@test.com")
        assertEquals("update2@test.com", keychain.getEmail())
        
        // Complete all background saves
        advanceUntilIdle()
    }

    @Test
    fun testNullValueHandling() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            false,
            testScope, 
            testDispatcher
        )
        
        advanceUntilIdle()
        
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
        
        advanceUntilIdle()
    }

    @Test
    fun testEncryptionDisabledMode() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            false, // Encryption disabled
            testScope, 
            testDispatcher
        )
        
        advanceUntilIdle()
        
        // Encryptor should be null when encryption is disabled
        assertNull(keychain.encryptor)
        
        // Test saving and retrieving
        keychain.saveEmail("plaintext@test.com")
        assertEquals("plaintext@test.com", keychain.getEmail())
        
        advanceUntilIdle()
    }

    @Test
    fun testDecryptionFailureHandling() = testScope.runTest {
        // Setup encrypted data that will fail to decrypt
        `when`(mockSharedPrefs.getString(eq("iterable-email"), any())).thenReturn("bad_encrypted_data")
        `when`(mockSharedPrefs.getBoolean(eq("iterable-email_plaintext"), eq(false))).thenReturn(false)
        
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            true,
            testScope, 
            testDispatcher
        )
        keychain.encryptor = mockEncryptor
        
        // Setup encryptor to fail decryption
        `when`(mockEncryptor.decrypt(any())).thenThrow(RuntimeException("Decryption failed"))
        
        // Complete initialization
        advanceUntilIdle()
        
        // Should return null due to decryption failure
        assertNull(keychain.getEmail())
        
        // Should have called failure handler
        verify(mockDecryptionFailureHandler, times(1)).onDecryptionFailed(any())
        
        // Should have removed the bad data
        verify(mockEditor, times(1)).remove(eq("iterable-email"))
    }

    @Test
    fun testEncryptionFailureFallback() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            true,
            testScope, 
            testDispatcher
        )
        keychain.encryptor = mockEncryptor
        
        // Setup encryption to fail
        `when`(mockEncryptor.encrypt(any())).thenThrow(RuntimeException("Encryption failed"))
        
        advanceUntilIdle()
        clearInvocations(mockEditor) // Clear setup interactions
        
        // Save should still work (cache updated immediately)
        keychain.saveEmail("fallback@test.com")
        assertEquals("fallback@test.com", keychain.getEmail())
        
        // Complete background save
        advanceUntilIdle()
        
        // Should have saved as plaintext (fallback)
        verify(mockEditor, times(1)).putString(eq("iterable-email"), eq("fallback@test.com"))
        verify(mockEditor, times(1)).putBoolean(eq("iterable-email_plaintext"), eq(true))
    }

    @Test
    fun testMigrationFailureHandling() = testScope.runTest {
        val mockMigrator = mock(IterableKeychainEncryptedDataMigrator::class.java)
        val migrationException = RuntimeException("Migration failed")
        
        `when`(mockMigrator.isMigrationCompleted()).thenReturn(false)
        doThrow(migrationException).`when`(mockMigrator).attemptMigration()
        
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler,
            mockMigrator,
            true,
            testScope,
            testDispatcher
        )
        
        advanceUntilIdle()
        
        // Verify failure handler was called
        verify(mockDecryptionFailureHandler, times(1)).onDecryptionFailed(any())
    }

    @Test
    fun testConcurrentOperations() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            false,
            testScope, 
            testDispatcher
        )
        
        advanceUntilIdle()
        
        // Launch multiple concurrent operations
        val jobs = mutableListOf<Job>()
        
        for (i in 1..10) {
            jobs.add(launch {
                keychain.saveEmail("email$i@test.com")
                val result = keychain.getEmail()
                assertNotNull("Should get some email value", result)
            })
        }
        
        // Wait for all to complete
        jobs.forEach { it.join() }
        advanceUntilIdle()
        
        // Should have some final email value
        assertNotNull(keychain.getEmail())
        assertTrue(keychain.getEmail()!!.contains("@test.com"))
    }

    @Test
    fun testTimeoutHandling() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            true,
            testScope, 
            testDispatcher
        )
        keychain.encryptor = mockEncryptor
        
        // Setup encryptor to hang (will be cancelled by timeout)
        `when`(mockEncryptor.decrypt(any())).thenAnswer {
            runBlocking { delay(5000) } // Longer than timeout
            "decrypted"
        }
        
        `when`(mockSharedPrefs.getString(eq("iterable-email"), any())).thenReturn("encrypted_data")
        
        advanceUntilIdle()
        
        // Should return null due to timeout
        assertNull(keychain.getEmail())
    }

    @Test
    fun testAllFieldsIndependently() = testScope.runTest {
        val keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler, 
            null, 
            false,
            testScope, 
            testDispatcher
        )
        
        advanceUntilIdle()
        
        // Test each field independently
        keychain.saveEmail("email@test.com")
        assertEquals("email@test.com", keychain.getEmail())
        assertNull(keychain.getUserId())
        assertNull(keychain.getAuthToken())
        
        keychain.saveUserId("user123")
        assertEquals("email@test.com", keychain.getEmail())
        assertEquals("user123", keychain.getUserId())
        assertNull(keychain.getAuthToken())
        
        keychain.saveAuthToken("token456")
        assertEquals("email@test.com", keychain.getEmail())
        assertEquals("user123", keychain.getUserId())
        assertEquals("token456", keychain.getAuthToken())
        
        // Clear one field
        keychain.saveUserId(null)
        assertEquals("email@test.com", keychain.getEmail())
        assertNull(keychain.getUserId())
        assertEquals("token456", keychain.getAuthToken())
        
        advanceUntilIdle()
    }
} 