package com.iterable.iterableapi;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IterableDataEncryptorTest extends BaseTest {

    private IterableDataEncryptor encryptor;

    @Mock
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        encryptor = new IterableDataEncryptor();
    }

    @Test
    public void testConstructor() {
        // Simply creating a new instance should not throw any exceptions
        IterableDataEncryptor encryptor = new IterableDataEncryptor();
        assertNotNull("Encryptor should be created successfully", encryptor);
    }

    @Test
    public void testEncryptDecryptSuccess() {
        String originalText = "test data to encrypt";
        String encrypted = encryptor.encrypt(originalText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull("Encrypted text should not be null", encrypted);
        assertNotEquals("Encrypted text should not match original", originalText, encrypted);
        assertEquals("Decrypted text should match original", originalText, decrypted);
    }

    @Test
    public void testEncryptNullInput() {
        String encrypted = encryptor.encrypt(null);
        assertNull("Encrypting null should return null", encrypted);
    }

    @Test
    public void testDecryptNullInput() {
        String decrypted = encryptor.decrypt(null);
        assertNull("Decrypting null should return null", decrypted);
    }

    @Test
    public void testEncryptEmptyString() {
        String encrypted = encryptor.encrypt("");
        String decrypted = encryptor.decrypt(encrypted);
        
        assertNotNull("Encrypted text should not be null", encrypted);
        assertEquals("Decrypted text should be empty string", "", decrypted);
    }

    @Test
    public void testEncryptLongString() {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("test");
        }
        String originalText = longString.toString();
        
        String encrypted = encryptor.encrypt(originalText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull("Encrypted text should not be null", encrypted);
        assertEquals("Decrypted long text should match original", originalText, decrypted);
    }

    @Test
    public void testMultipleEncryptions() {
        String text1 = "first text";
        String text2 = "second text";
        
        String encrypted1 = encryptor.encrypt(text1);
        String encrypted2 = encryptor.encrypt(text2);
        
        assertNotEquals("Different texts should have different encryptions", encrypted1, encrypted2);
        assertEquals("First text should decrypt correctly", text1, encryptor.decrypt(encrypted1));
        assertEquals("Second text should decrypt correctly", text2, encryptor.decrypt(encrypted2));
    }

    @Test(expected = IterableDataEncryptor.DecryptionException.class)
    public void testDecryptInvalidData() {
        encryptor.decrypt("invalid encrypted data");
    }

    @Test
    public void testClearKeyAndData() {
        String originalText = "test data";
        String encrypted = encryptor.encrypt(originalText);
        
        // Clear the key
        encryptor.clearKeyAndData(sharedPreferences);
        
        // Try to decrypt the data encrypted with the old key
        try {
            encryptor.decrypt(encrypted);
            fail("Should not be able to decrypt data with cleared key");
        } catch (Exception e) {
            // Expected behavior - old encrypted data should not be decryptable
            assertNotNull(e);
        }
        
        // Verify new encryption/decryption works after clearing
        String newEncrypted = encryptor.encrypt(originalText);
        String newDecrypted = encryptor.decrypt(newEncrypted);
        assertEquals("New encryption/decryption should work after clearing", originalText, newDecrypted);
    }

    @Test
    public void testKeyGeneration() throws Exception {
        // Create new encryptor which should trigger key generation
        IterableDataEncryptor encryptor = new IterableDataEncryptor();
        
        // Get the keystore from the encryptor (we'll need to add a method to expose this)
        KeyStore keyStore = encryptor.getKeyStore();
        
        // Verify the key exists in keystore
        assertTrue("Key should exist in keystore", keyStore.containsAlias("iterable_encryption_key"));
        
        // Rest of the test remains the same
        String testData = "test data";
        String encrypted = encryptor.encrypt(testData);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals("Data should be correctly encrypted and decrypted", testData, decrypted);
    }

    @Test
    public void testKeyRegeneration() throws Exception {
        // Create first encryptor and encrypt data
        IterableDataEncryptor encryptor1 = new IterableDataEncryptor();
        KeyStore keyStore = encryptor1.getKeyStore();
        
        String testData = "test data";
        String encrypted1 = encryptor1.encrypt(testData);
        
        // Delete the key
        encryptor1.clearKeyAndData(sharedPreferences);
        
        // Create second encryptor which should generate a new key
        IterableDataEncryptor encryptor2 = new IterableDataEncryptor();
        
        // Rest of the test remains the same
        assertTrue("Key should be regenerated", keyStore.containsAlias("iterable_encryption_key"));
        
        // Verify old encrypted data can't be decrypted with new key
        try {
            encryptor2.decrypt(encrypted1);
            fail("Should not be able to decrypt data encrypted with old key");
        } catch (Exception e) {
            // Expected
        }
        
        // Verify new encryption/decryption works
        String encrypted2 = encryptor2.encrypt(testData);
        String decrypted2 = encryptor2.decrypt(encrypted2);
        assertEquals("New key should work for encryption/decryption", testData, decrypted2);
    }

    @Test
    public void testMultipleEncryptorInstances() throws Exception {
        // Create two encryptor instances
        IterableDataEncryptor encryptor1 = new IterableDataEncryptor();
        IterableDataEncryptor encryptor2 = new IterableDataEncryptor();
        
        // Test that they can decrypt each other's encrypted data
        String testData = "test data";
        String encrypted1 = encryptor1.encrypt(testData);
        String encrypted2 = encryptor2.encrypt(testData);
        
        assertEquals("Encryptor 2 should decrypt Encryptor 1's data", testData, encryptor2.decrypt(encrypted1));
        assertEquals("Encryptor 1 should decrypt Encryptor 2's data", testData, encryptor1.decrypt(encrypted2));
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 5;
        int operationsPerThread = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean hasErrors = new AtomicBoolean(false);
        List<Thread> threads = new ArrayList<>();

        // Create and start multiple threads
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String originalText = "Thread-" + threadId + "-Data-" + j;
                        String encrypted = encryptor.encrypt(originalText);
                        String decrypted = encryptor.decrypt(encrypted);
                        
                        if (!originalText.equals(decrypted)) {
                            hasErrors.set(true);
                            IterableLogger.e("TestConcurrent", "Encryption/Decryption mismatch: " + originalText + " != " + decrypted);
                        }
                    }
                } catch (Exception e) {
                    hasErrors.set(true);
                    IterableLogger.e("TestConcurrent", "Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete with a shorter timeout
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        
        assertTrue("All threads should complete within timeout", completed);
        assertFalse("No errors should occur", hasErrors.get());
    }

    @Test
    public void testSpecialCharacters() {
        String specialChars = "!@#$%^&*()_+{}[]|\"':;?/>.<,~`";
        String encrypted = encryptor.encrypt(specialChars);
        assertEquals(specialChars, encryptor.decrypt(encrypted));
    }

    @Test
    public void testUnicodeStrings() {
        String unicodeText = "Hello 世界 🌍";
        String encrypted = encryptor.encrypt(unicodeText);
        assertEquals(unicodeText, encryptor.decrypt(encrypted));
    }
} 