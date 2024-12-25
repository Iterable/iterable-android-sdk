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
        `when`(mockSharedPrefs.getBoolean(any<String>(), anyBoolean())).thenReturn(false)
        `when`(mockSharedPrefs.getString(any<String>(), any())).thenReturn(null)
        
        // Mock editor.apply() to do nothing
        Mockito.doNothing().`when`(mockEditor).apply()

        keychain = IterableKeychain(
            mockContext, 
            mockDecryptionFailureHandler
        )
        // Directly set the mock encryptor
        keychain.encryptor = mockEncryptor

        // Setup encrypt/decrypt behavior
        `when`(mockEncryptor.encrypt(any())).thenAnswer { invocation ->
            "encrypted_${invocation.arguments[0]}"
        }
        `when`(mockEncryptor.decrypt(any())).thenAnswer { invocation ->
            val encrypted = invocation.arguments[0] as String
            if (encrypted.startsWith("encrypted_")) {
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
        // Setup mock to simulate decryption failure
        `when`(mockSharedPrefs.getString(eq("iterable-email"), isNull()))
            .thenReturn("corrupted_encrypted_value")

        val result = keychain.getEmail()

        // Verify data was cleared
        verify(mockEditor).remove("iterable-email")
        verify(mockEditor).remove("iterable-user-id")
        verify(mockEditor).remove("iterable-auth-token")
        verify(mockEditor).apply()

        // Verify failure handler was called
        verify(mockDecryptionFailureHandler).onDecryptionFailed(any())

        assertNull(result)
    }

    @Test
    fun testSaveNullValues() {
        keychain.saveEmail(null)
        keychain.saveUserId(null)
        keychain.saveAuthToken(null)

        // Verify exactly one putString call for each save operation
        verify(mockEditor, times(3)).putString(any(), isNull())
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
} 