package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class IterableKeychainEncryptedDataMigratorTest extends BaseTest {

    @Mock private Context mockContext;
    @Mock private SharedPreferences mockSharedPrefs;
    @Mock private SharedPreferences mockEncryptedPrefs;
    @Mock private SharedPreferences.Editor mockEditor;
    @Mock private SharedPreferences.Editor mockEncryptedEditor;
    @Mock private IterableKeychain mockKeychain;
    
    private IterableKeychainEncryptedDataMigrator migrator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockSharedPrefs.edit()).thenReturn(mockEditor);
        when(mockEncryptedPrefs.edit()).thenReturn(mockEncryptedEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEncryptedEditor.clear()).thenReturn(mockEncryptedEditor);
        
        // Set up keychain keys - using the correct property names with $iterableapi_debug suffix
        when(mockKeychain.getEmailKey$iterableapi_debug()).thenReturn("iterable-email");
        when(mockKeychain.getUserIdKey$iterableapi_debug()).thenReturn("iterable-user-id");
        when(mockKeychain.getAuthTokenKey$iterableapi_debug()).thenReturn("iterable-auth-token");
        
        migrator = new IterableKeychainEncryptedDataMigrator(
            mockContext,
            mockSharedPrefs,
            mockKeychain
        );
        migrator.setMockEncryptedPrefs(mockEncryptedPrefs);
    }

    @Test
    public void testSkipIfAlreadyCompleted() {
        when(mockSharedPrefs.getBoolean("iterable-encrypted-migration-completed", false))
            .thenReturn(true);

        migrator.attemptMigration();

        verify(mockSharedPrefs, never()).edit();
    }

    @Test
    public void testSkipIfBelowAndroidM() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP);
        
        migrator.attemptMigration();

        verify(mockEditor).putBoolean("iterable-encrypted-migration-completed", true);
        verify(mockEditor).apply();
    }

    @Test
    public void testThrowsExceptionIfPreviouslyInterrupted() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        when(mockSharedPrefs.getBoolean("iterable-encrypted-migration-started", false))
            .thenReturn(true);

        migrator.setMigrationCompletionCallback(throwable -> {
            error.set(throwable);
            latch.countDown();
            return null;
        });

        migrator.attemptMigration();

        assertTrue("Migration timed out", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have received an error", error.get());
        assertTrue("Exception should be MigrationException",
            error.get() instanceof IterableKeychainEncryptedDataMigrator.MigrationException);
        assertEquals("Previous migration attempt was interrupted", error.get().getMessage());

        verify(mockEditor).putBoolean("iterable-encrypted-migration-completed", true);
        verify(mockEditor).apply();
    }

    @Test
    public void testSuccessfulMigration() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        String testEmail = "test@example.com";
        String testUserId = "user123";
        String testAuthToken = "auth-token-123";

        when(mockEncryptedPrefs.getString(eq("iterable_email"), eq(null))).thenReturn(testEmail);
        when(mockEncryptedPrefs.getString(eq("iterable_user_id"), eq(null))).thenReturn(testUserId);
        when(mockEncryptedPrefs.getString(eq("iterable_auth_token"), eq(null))).thenReturn(testAuthToken);

        migrator.setMigrationCompletionCallback(throwable -> {
            error.set(throwable);
            latch.countDown();
            return null;
        });

        migrator.attemptMigration();
        
        assertTrue("Migration timed out", latch.await(5, TimeUnit.SECONDS));
        assertNull("Migration failed with error: " + error.get(), error.get());

        verify(mockEditor).putBoolean("iterable-encrypted-migration-started", true);
        verify(mockEditor).putBoolean("iterable-encrypted-migration-completed", true);
        
        verify(mockKeychain).saveEmail(testEmail);
        verify(mockKeychain).saveUserId(testUserId);
        verify(mockKeychain).saveAuthToken(testAuthToken);
        
        verify(mockEncryptedEditor).clear();
        verify(mockEncryptedEditor).apply();
    }

    @Test
    public void testPartialDataMigration() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        String testEmail = "test@example.com";
        when(mockEncryptedPrefs.getString(eq("iterable_email"), eq(null))).thenReturn(testEmail);
        when(mockEncryptedPrefs.getString(eq("iterable_user_id"), eq(null))).thenReturn(null);
        when(mockEncryptedPrefs.getString(eq("iterable_auth_token"), eq(null))).thenReturn(null);

        migrator.setMigrationCompletionCallback(throwable -> {
            error.set(throwable);
            latch.countDown();
            return null;
        });

        migrator.attemptMigration();
        
        assertTrue("Migration timed out", latch.await(5, TimeUnit.SECONDS));
        assertNull("Migration failed with error: " + error.get(), error.get());

        verify(mockKeychain).saveEmail(testEmail);
        verify(mockKeychain, never()).saveUserId(anyString());
        verify(mockKeychain, never()).saveAuthToken(anyString());
    }

    @Test
    public void testMigrationWithEmptyData() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        when(mockEncryptedPrefs.getString(anyString(), eq(null))).thenReturn(null);

        migrator.setMigrationCompletionCallback(throwable -> {
            error.set(throwable);
            latch.countDown();
            return null;
        });

        migrator.attemptMigration();
        
        assertTrue("Migration timed out", latch.await(5, TimeUnit.SECONDS));
        assertNull("Migration failed with error: " + error.get(), error.get());

        verify(mockKeychain, never()).saveEmail(anyString());
        verify(mockKeychain, never()).saveUserId(anyString());
        verify(mockKeychain, never()).saveAuthToken(anyString());
        
        verify(mockEncryptedEditor).clear();
        verify(mockEditor).putBoolean("iterable-encrypted-migration-completed", true);
    }

    @Test
    public void testMigrationError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        when(mockEncryptedPrefs.getString(anyString(), eq(null)))
            .thenThrow(new SecurityException("Encryption error"));

        migrator.setMigrationCompletionCallback(throwable -> {
            error.set(throwable);
            latch.countDown();
            return null;
        });

        migrator.attemptMigration();
        
        assertTrue("Migration timed out", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have received an error", error.get());
        assertTrue("Exception should be MigrationException", 
            error.get() instanceof IterableKeychainEncryptedDataMigrator.MigrationException);
        assertEquals("Failed to migrate data", error.get().getMessage());
    }

    @Test
    public void testMigrationTimeout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // Set a very short timeout (50ms)
        migrator.setMigrationTimeout(50L);

        // Make the migration hang by making getString block
        when(mockEncryptedPrefs.getString(anyString(), eq(null))).thenAnswer(invocation -> {
            Thread.sleep(1000); // Sleep longer than timeout
            return "test@example.com";
        });

        migrator.setMigrationCompletionCallback(throwable -> {
            error.set(throwable);
            latch.countDown();
            return null;
        });

        migrator.attemptMigration();
        
        assertTrue("Migration timed out", latch.await(1, TimeUnit.SECONDS));
        assertNotNull("Should have received an error", error.get());
        assertTrue("Exception should be MigrationException", 
            error.get() instanceof IterableKeychainEncryptedDataMigrator.MigrationException);
        assertTrue("Error message should mention timeout", 
            error.get().getMessage().contains("Migration timed out after 50ms"));
        
        // Verify both calls to apply():
        // 1. Setting migration started flag
        verify(mockEditor).putBoolean("iterable-encrypted-migration-started", true);
        // 2. Setting migration completed flag during timeout
        verify(mockEditor).putBoolean("iterable-encrypted-migration-completed", true);
        verify(mockEditor, times(2)).apply();
    }

} 