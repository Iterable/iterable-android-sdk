package com.iterable.iterableapi;

import android.os.Build;

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

    // Timeout configuration moved to individual test setUp() methods
    // Each test can configure its own timeouts as needed

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

        // Disable automatic in-app message syncing to prevent background requests during tests
        IterableConfig config = new IterableConfig.Builder()
            .setAutoPushRegistration(false)  // Disable auto push registration
            .build();

        initIterableApi(config);
        IterableApi.getInstance().setEmail("test_email");

        // Pause in-app auto display to prevent automatic syncs
        if (IterableApi.getInstance().getInAppManager() != null) {
            IterableApi.getInstance().getInAppManager().setAutoDisplayPaused(true);
        }
    }

    public static void initIterableApi(IterableConfig config) {
        IterableApi.initialize(getApplicationContext(), "fake_key", config);
    }
}
