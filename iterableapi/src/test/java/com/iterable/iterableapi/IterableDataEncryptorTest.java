package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IterableDataEncryptorTest extends BaseTest {

    private IterableDataEncryptor encryptor;
    private IterableKeychain keychain;

    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
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
    public void testDecryptionExceptionDetails() {
        try {
            encryptor.decrypt("invalid_base64_data!!!");
            fail("Should throw DecryptionException");
        } catch (Exception e) {
            assertTrue("Should be instance of DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertNotNull("Exception should have a cause", e.getCause());
            assertEquals("Exception should have correct message", "Failed to decrypt data", e.getMessage());
        }
    }

    @Test
    public void testDecryptTamperedData() {
        String originalText = "test data";
        String encrypted = encryptor.encrypt(originalText);
        // Tamper with the encrypted data while maintaining valid base64
        String tamperedData = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        try {
            encryptor.decrypt(tamperedData);
            fail("Should throw DecryptionException for tampered data");
        } catch (Exception e) {
            assertTrue("Should be instance of DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertNotNull("Exception should have a cause", e.getCause());
        }
    }

    @Test
    public void testResetKeys() {
        String originalText = "test data";
        String encrypted = encryptor.encrypt(originalText);

        // Clear the key
        encryptor.resetKeys();

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
        encryptor1.resetKeys();

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

    @Test
    public void testDecryptionAfterKeyLoss() {
        // Create data with original key
        String testData = "test data";
        String encrypted = encryptor.encrypt(testData);

        // Clear the key and generate a new one
        encryptor.resetKeys();

        try {
            encryptor.decrypt(encrypted);
            fail("Should throw DecryptionException when decrypting with new key");
        } catch (Exception e) {
            assertTrue("Should be instance of DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertNotNull("Exception should have a cause", e.getCause());
            assertEquals("Exception should have correct message", "Failed to decrypt data", e.getMessage());
        }
    }

    @Test
    public void testEncryptionAcrossApiLevels() {
        String testData = "test data for cross-version compatibility";

        // Test API 16 (Legacy)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        String encryptedOnApi16 = encryptor.encrypt(testData);

        // Test API 18 (Legacy)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN_MR2);
        String encryptedOnApi18 = encryptor.encrypt(testData);
        assertEquals("Legacy decryption should work on API 18", testData, encryptor.decrypt(encryptedOnApi16));

        // Test API 19 (Modern - First version with GCM support)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.KITKAT);
        String encryptedOnApi19 = encryptor.encrypt(testData);
        assertEquals("Should decrypt legacy data on API 19", testData, encryptor.decrypt(encryptedOnApi16));
        assertEquals("Should decrypt legacy data on API 19", testData, encryptor.decrypt(encryptedOnApi18));

        // Test API 23 (Modern with KeyStore)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        String encryptedOnApi23 = encryptor.encrypt(testData);
        assertEquals("Should decrypt legacy data on API 23", testData, encryptor.decrypt(encryptedOnApi16));
        assertEquals("Should decrypt API 19 data on API 23", testData, encryptor.decrypt(encryptedOnApi19));

        // Test that modern encryption fails on legacy devices
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        try {
            encryptor.decrypt(encryptedOnApi19);
            fail("Should not be able to decrypt modern encryption on legacy device");
        } catch (Exception e) {
            assertTrue("Should be DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertEquals("Should have correct error message", "Modern encryption cannot be decrypted on legacy devices", e.getMessage());
        }
        try {
            encryptor.decrypt(encryptedOnApi23);
            fail("Should not be able to decrypt modern encryption on legacy device");
        } catch (Exception e) {
            assertTrue("Should be DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertEquals("Should have correct error message", "Modern encryption cannot be decrypted on legacy devices", e.getMessage());
        }
    }

    @Test
    public void testEncryptionMethodFlag() {
        String testData = "test data for encryption method verification";

        // Test legacy encryption flag (API 16)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        String legacyEncrypted = encryptor.encrypt(testData);
        byte[] legacyBytes = Base64.decode(legacyEncrypted, Base64.NO_WRAP);
        assertEquals("Legacy encryption should have flag 0", 0, legacyBytes[0]);

        // Test modern encryption flag (API 19)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.KITKAT);
        String modernEncrypted = encryptor.encrypt(testData);
        byte[] modernBytes = Base64.decode(modernEncrypted, Base64.NO_WRAP);
        assertEquals("Modern encryption should have flag 1", 1, modernBytes[0]);
    }

    @Test
    public void testDecryptCorruptData() {
        String testData = "test data";
        String encrypted = encryptor.encrypt(testData);
        byte[] bytes = Base64.decode(encrypted, Base64.NO_WRAP);

        // Corrupt the data portion
        bytes[bytes.length - 1] ^= 0xFF;
        String corrupted = Base64.encodeToString(bytes, Base64.NO_WRAP);

        try {
            encryptor.decrypt(corrupted);
            fail("Should throw exception for corrupted data");
        } catch (Exception e) {
            assertTrue("Should be DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertNotNull("Should have a cause", e.getCause());
        }
    }

    @Test
    public void testDecryptManipulatedIV() {
        String testData = "test data";
        String encrypted = encryptor.encrypt(testData);
        byte[] bytes = Base64.decode(encrypted, Base64.NO_WRAP);

        // Manipulate the IV
        bytes[1] ^= 0xFF;  // First byte after version flag
        String manipulated = Base64.encodeToString(bytes, Base64.NO_WRAP);

        try {
            encryptor.decrypt(manipulated);
            fail("Should throw exception for manipulated IV");
        } catch (Exception e) {
            assertTrue("Should be DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertNotNull("Should have a cause", e.getCause());
        }
    }

    @Test
    public void testDecryptManipulatedVersionFlag() {
        // Test on API 16 device
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);

        String testData = "test data";
        String encrypted = encryptor.encrypt(testData);
        byte[] bytes = Base64.decode(encrypted, Base64.NO_WRAP);

        // Change version flag from legacy (0) to modern (1)
        bytes[0] = 1;
        String manipulated = Base64.encodeToString(bytes, Base64.NO_WRAP);

        try {
            encryptor.decrypt(manipulated);
            fail("Should throw exception for manipulated version flag");
        } catch (Exception e) {
            assertTrue("Should be DecryptionException", e instanceof IterableDataEncryptor.DecryptionException);
            assertEquals("Modern encryption cannot be decrypted on legacy devices", e.getMessage());
        }
    }

    @Test
    public void testLegacyEncryptionAndDecryption() {
        // Set to API 16 (Legacy)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);

        String testData = "test data for legacy encryption";
        String encrypted = encryptor.encrypt(testData);
        String decrypted = encryptor.decrypt(encrypted);

        assertEquals("Legacy encryption/decryption should work on API 16", testData, decrypted);

        // Verify it's using legacy encryption
        byte[] encryptedBytes = Base64.decode(encrypted, Base64.NO_WRAP);
        assertEquals("Should use legacy encryption flag", 0, encryptedBytes[0]);

        // Test on API 18
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN_MR2);
        String decryptedOnApi18 = encryptor.decrypt(encrypted);
        assertEquals("Legacy data should be decryptable on API 18", testData, decryptedOnApi18);

        String encryptedOnApi18 = encryptor.encrypt(testData);
        String decryptedFromApi18 = encryptor.decrypt(encryptedOnApi18);
        assertEquals("API 18 encryption/decryption should work", testData, decryptedFromApi18);

        // Verify API 18 also uses legacy encryption
        byte[] api18EncryptedBytes = Base64.decode(encryptedOnApi18, Base64.NO_WRAP);
        assertEquals("Should use legacy encryption flag on API 18", 0, api18EncryptedBytes[0]);
    }

    @Test
    public void testModernEncryptionAndDecryption() {
        String testData = "test data for modern encryption";

        // Test on API 19 (First modern version)
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.KITKAT);
        String encryptedOnApi19 = encryptor.encrypt(testData);
        String decryptedOnApi19 = encryptor.decrypt(encryptedOnApi19);
        assertEquals("Modern encryption should work on API 19", testData, decryptedOnApi19);

        byte[] api19EncryptedBytes = Base64.decode(encryptedOnApi19, Base64.NO_WRAP);
        assertEquals("Should use modern encryption flag on API 19", 1, api19EncryptedBytes[0]);

        // Test on API 23
        setFinalStatic(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);
        String decryptedOnApi23 = encryptor.decrypt(encryptedOnApi19);
        assertEquals("API 19 data should be decryptable on API 23", testData, decryptedOnApi23);

        String encryptedOnApi23 = encryptor.encrypt(testData);
        String decryptedFromApi23 = encryptor.decrypt(encryptedOnApi23);
        assertEquals("API 23 encryption/decryption should work", testData, decryptedFromApi23);

        byte[] api23EncryptedBytes = Base64.decode(encryptedOnApi23, Base64.NO_WRAP);
        assertEquals("Should use modern encryption flag on API 23", 1, api23EncryptedBytes[0]);
    }

    private static void setFinalStatic(Class<?> clazz, String fieldName, Object newValue) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            // On Java 8 and lower, use modifiers field
            try {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (NoSuchFieldException e) {
                // On Java 9+, use VarHandle to modify final fields
                try {
                    // Get the internal Field.modifiers field via JDK internal API
                    Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                    getDeclaredFields0.setAccessible(true);
                    Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                    Field modifiersField = null;
                    for (Field f : fields) {
                        if ("modifiers".equals(f.getName())) {
                            modifiersField = f;
                            break;
                        }
                    }
                    if (modifiersField != null) {
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    }
                } catch (Exception ignored) {
                    // If all attempts fail, try setting the value anyway
                }
            }

            field.set(null, newValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}