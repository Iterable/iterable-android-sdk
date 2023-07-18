package com.iterable.iterableapi;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.mockito.Mockito.mock;

public class IterableTestUtils {
    public static void createIterableApi() {
        IterableApi.sharedInstance = new IterableApi(mock(IterableInAppManager.class));
        IterableConfig config = new IterableConfig.Builder().build();
        initIterableApi(config);
        IterableApi.getInstance().setEmail("test_email");
    }

    public static void initIterableApi(IterableConfig config) {
        IterableApi.initialize(getApplicationContext(), "fake_key", config);
    }
}
