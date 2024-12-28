package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class IterableKeychainEncryptedDataMigratorTest extends BaseTest {

    private static final String MIGRATION_STARTED_KEY = "iterable-encrypted-migration-started";
    private static final String MIGRATION_COMPLETED_KEY = "iterable-encrypted-migration-completed";
    private static final String OLD_EMAIL_KEY = IterableKeychain.KEY_EMAIL;
    private static final String OLD_USER_ID_KEY = IterableKeychain.KEY_USER_ID;
    private static final String OLD_AUTH_TOKEN_KEY = IterableKeychain.KEY_AUTH_TOKEN;

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

        // Setup SharedPreferences mocks
        when(mockSharedPrefs.edit()).thenReturn(mockEditor);
        when(mockEncryptedPrefs.edit()).thenReturn(mockEncryptedEditor);

        // Setup editor method chaining
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEncryptedEditor.clear()).thenReturn(mockEncryptedEditor);

        // No need to mock void methods (apply)

        migrator = new IterableKeychainEncryptedDataMigrator(
            mockContext,
            mockSharedPrefs,
            mockKeychain
        );
        migrator.setMockEncryptedPrefs(mockEncryptedPrefs);
    }

    @Test
    public void testSkipIfAlreadyCompleted() {
        when(mockSharedPrefs.getBoolean(MIGRATION_COMPLETED_KEY, false))
            .thenReturn(true);

        migrator.attemptMigration();

        verify(mockSharedPrefs, never()).edit();
    }

    @Test
    public void testThrowsExceptionIfPreviouslyInterrupted() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        when(mockSharedPrefs.getBoolean(MIGRATION_STARTED_KEY, false))
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

        verify(mockEditor).putBoolean(MIGRATION_COMPLETED_KEY, true);
        verify(mockEditor).apply();
    }

    @Test
    public void testSuccessfulMigration() {
        String testEmail = "test@example.com";
        String testUserId = "user123";
        String testAuthToken = "auth-token-123";

        when(mockEncryptedPrefs.getString(eq(OLD_EMAIL_KEY), eq(null))).thenReturn(testEmail);
        when(mockEncryptedPrefs.getString(eq(OLD_USER_ID_KEY), eq(null))).thenReturn(testUserId);
        when(mockEncryptedPrefs.getString(eq(OLD_AUTH_TOKEN_KEY), eq(null))).thenReturn(testAuthToken);

        migrator.attemptMigration();

        verify(mockEditor).putBoolean(MIGRATION_STARTED_KEY, true);
        verify(mockEditor).putBoolean(MIGRATION_COMPLETED_KEY, true);

        verify(mockKeychain).saveEmail(testEmail);
        verify(mockKeychain).saveUserId(testUserId);
        verify(mockKeychain).saveAuthToken(testAuthToken);

        verify(mockEncryptedEditor).remove(OLD_EMAIL_KEY);
        verify(mockEncryptedEditor).remove(OLD_USER_ID_KEY);
        verify(mockEncryptedEditor).remove(OLD_AUTH_TOKEN_KEY);
        verify(mockEncryptedEditor).apply();
    }

    @Test
    public void testPartialDataMigration() {
        String testEmail = "test@example.com";
        when(mockEncryptedPrefs.getString(eq(OLD_EMAIL_KEY), eq(null))).thenReturn(testEmail);
        when(mockEncryptedPrefs.getString(eq(OLD_USER_ID_KEY), eq(null))).thenReturn(null);
        when(mockEncryptedPrefs.getString(eq(OLD_AUTH_TOKEN_KEY), eq(null))).thenReturn(null);

        migrator.attemptMigration();

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

        verify(mockEncryptedEditor).apply();
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
        assertEquals("Migration failed", error.get().getMessage());
    }

    @Test
    public void testMigrationTimeout() {
        // Set a very short timeout (50ms)
        migrator.setMigrationTimeout(50L);

        // Make the migration hang by making getString block
        when(mockEncryptedPrefs.getString(anyString(), eq(null))).thenAnswer(invocation -> {
            Thread.sleep(1000); // Sleep longer than timeout
            return "test@example.com";
        });

        migrator.attemptMigration();

        // Verify both calls to apply():
        // 1. Setting migration started flag
        verify(mockEditor).putBoolean(MIGRATION_STARTED_KEY, true);
        // 2. Setting migration completed flag during timeout
        verify(mockEditor).putBoolean(MIGRATION_COMPLETED_KEY, true);
        verify(mockEditor, times(2)).apply();
    }

    @Test
    public void testMigrationBehaviorBelowAndroidM() {
        // Set Android version to Lollipop (below M)
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP);

        String testEmail = "test@example.com";
        // Use mockSharedPrefs instead of mockEncryptedPrefs since below Android M
        when(mockSharedPrefs.getString(eq(OLD_EMAIL_KEY), eq(null))).thenReturn(testEmail);

        migrator.attemptMigration();

        // Should migrate using regular SharedPreferences
        verify(mockKeychain).saveEmail(testEmail);
        verify(mockEditor).putBoolean(MIGRATION_STARTED_KEY, true);
        verify(mockEditor).putBoolean(MIGRATION_COMPLETED_KEY, true);
        verify(mockEditor, times(3)).apply(); // Called three times during the migration process
        
        // Verify that mockEncryptedPrefs was never used
        verify(mockEncryptedPrefs, never()).getString(anyString(), eq(null));
    }
}