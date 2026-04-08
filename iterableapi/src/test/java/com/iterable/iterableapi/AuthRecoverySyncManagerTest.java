package com.iterable.iterableapi;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthRecoverySyncManagerTest {

    private IterableApi createMockApi(boolean autoPushRegistration) {
        IterableApi mockApi = mock(IterableApi.class);
        mockApi.config = new IterableConfig.Builder()
                .setAutoPushRegistration(autoPushRegistration)
                .build();
        IterableInAppManager mockInApp = mock(IterableInAppManager.class);
        IterableEmbeddedManager mockEmbedded = mock(IterableEmbeddedManager.class);
        when(mockApi.getInAppManager()).thenReturn(mockInApp);
        when(mockApi.getEmbeddedManager()).thenReturn(mockEmbedded);
        return mockApi;
    }

    @Test
    public void testOnAuthTokenReadySyncsInApp() {
        IterableApi mockApi = createMockApi(false);
        AuthRecoverySyncManager manager = new AuthRecoverySyncManager(mockApi);

        manager.onAuthTokenReady();

        verify(mockApi.getInAppManager()).syncInApp();
    }

    @Test
    public void testOnAuthTokenReadySyncsEmbeddedMessages() {
        IterableApi mockApi = createMockApi(false);
        AuthRecoverySyncManager manager = new AuthRecoverySyncManager(mockApi);

        manager.onAuthTokenReady();

        verify(mockApi.getEmbeddedManager()).syncMessages();
    }

    @Test
    public void testOnAuthTokenReadyRegistersForPushWhenEnabled() {
        IterableApi mockApi = createMockApi(true);
        AuthRecoverySyncManager manager = new AuthRecoverySyncManager(mockApi);

        manager.onAuthTokenReady();

        verify(mockApi).registerForPush();
    }

    @Test
    public void testOnAuthTokenReadySkipsPushWhenDisabled() {
        IterableApi mockApi = createMockApi(false);
        AuthRecoverySyncManager manager = new AuthRecoverySyncManager(mockApi);

        manager.onAuthTokenReady();

        verify(mockApi, never()).registerForPush();
    }
}
