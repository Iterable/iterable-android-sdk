package com.iterable.iterableapi;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

public class IterableTestUtils {
    public static void legacyInitIterableApi() {
        IterableApi.sharedInstanceWithApiKey(getApplicationContext(), "fake_key", "test_email");
    }

    public static void initIterableApi(IterableConfig config) {
        IterableApi.initialize(getApplicationContext(), "fake_key", config);
    }
}
