package com.iterable.iterableapi;

import android.os.Build;

import java.lang.reflect.Field;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.mockito.Mockito.mock;

/**
 * Utility class for setting up Iterable API in instrumentation tests.
 *
 * <p>Handles Android API 36+ compatibility where Mockito's ByteBuddy cannot create
 * mocks due to security restrictions on writable dex files.</p>
 *
 * @see <a href="https://developer.android.com/privacy-and-security/risks/dynamic-code-loading">
 *      Android Security: Dynamic Code Loading</a>
 */
public class IterableTestUtils {

    static {
        // Increase HTTP timeouts for instrumentation tests to accommodate slow emulators
        // The default 3-second POST timeout is too short for MockWebServer on emulators
        try {
            increaseHttpTimeouts();
            android.util.Log.i("IterableTestUtils", "✅ Successfully increased HTTP timeouts for tests");
        } catch (Exception e) {
            // If reflection fails, tests will continue with default timeouts
            android.util.Log.e("IterableTestUtils", "❌ Could not increase HTTP timeouts for tests", e);
        }
    }

    /**
     * Uses reflection to increase HTTP timeouts in IterableRequestTask for testing.
     * This prevents flaky test failures due to emulator performance limitations.
     *
     * Increases timeouts to 30 seconds to handle even the slowest emulator scenarios.
     * The timeout fields are intentionally non-final in production code to enable this.
     */
    private static void increaseHttpTimeouts() throws Exception {
        Class<?> taskClass = IterableRequestTask.class;

        android.util.Log.d("IterableTestUtils", "Increasing HTTP timeouts for tests...");

        // Increase POST timeout from 3s to 30s for tests (very generous for slow emulators)
        Field postTimeoutField = taskClass.getDeclaredField("POST_REQUEST_DEFAULT_TIMEOUT_MS");
        postTimeoutField.setAccessible(true);

        int oldPostTimeout = postTimeoutField.getInt(null);
        postTimeoutField.setInt(null, 30000);
        int newPostTimeout = postTimeoutField.getInt(null);

        android.util.Log.i("IterableTestUtils", "✓ POST timeout: " + oldPostTimeout + "ms → " + newPostTimeout + "ms");

        // Increase GET timeout from 10s to 40s for tests
        Field getTimeoutField = taskClass.getDeclaredField("GET_REQUEST_DEFAULT_TIMEOUT_MS");
        getTimeoutField.setAccessible(true);

        int oldGetTimeout = getTimeoutField.getInt(null);
        getTimeoutField.setInt(null, 40000);
        int newGetTimeout = getTimeoutField.getInt(null);

        android.util.Log.i("IterableTestUtils", "✓ GET timeout: " + oldGetTimeout + "ms → " + newGetTimeout + "ms");

        // Verify the changes took effect
        if (newPostTimeout != 30000 || newGetTimeout != 40000) {
            throw new RuntimeException("Failed to update timeouts via reflection");
        }
    }

    public static void createIterableApi() {
        IterableInAppManager inAppManager;

        if (Build.VERSION.SDK_INT >= 36) {
            // Android API 36+ blocks Mockito's ByteBuddy from creating dex files in cache
            // Pass null instead - the IterableApi constructor supports this
            inAppManager = null;
        } else {
            // On older APIs, use Mockito mock for better test isolation
            inAppManager = mock(IterableInAppManager.class);
        }

        IterableApi.sharedInstance = new IterableApi(inAppManager);
        IterableConfig config = new IterableConfig.Builder().build();
        initIterableApi(config);
        IterableApi.getInstance().setEmail("test_email");
    }

    public static void initIterableApi(IterableConfig config) {
        IterableApi.initialize(getApplicationContext(), "fake_key", config);
    }
}
