package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class IterableInAppManagerSyncTest extends BaseTest {

    @Mock
    private IterableApi iterableApiMock;
    @Mock
    private IterableInAppHandler handlerMock;
    @Mock
    private IterableInAppStorage storageMock;
    @Mock
    private IterableActivityMonitor activityMonitorMock;

    private IterableInAppManager inAppManager;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        inAppManager = spy(new IterableInAppManager(iterableApiMock, handlerMock, 30.0, storageMock, activityMonitorMock));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                IterableHelper.IterableActionHandler handler = invocation.getArgument(1);
                handler.execute(IterableTestUtils.getResourceString("inapp_payload_single.json"));
                return null;
            }
        }).when(iterableApiMock).getInAppMessages(any(Integer.class), any(IterableHelper.IterableActionHandler.class));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSyncOnLaunch() throws Exception {
        ArgumentCaptor<IterableActivityMonitor.AppStateCallback> callbackCaptor = ArgumentCaptor.forClass(IterableActivityMonitor.AppStateCallback.class);
        verify(activityMonitorMock).addCallback(callbackCaptor.capture());
        inAppManager.onSwitchToForeground();
        verify(inAppManager).syncInApp();

        // Test if we sync if the last sync was just now
        reset(inAppManager);
        inAppManager.onSwitchToForeground();
        verify(inAppManager, never()).syncInApp();

        // Test if we sync if the last sync was 10 minutes ago
        reset(inAppManager);
        doReturn(System.currentTimeMillis() + 600 * 1000).when(getIterableUtilSpy()).currentTimeMillis();
        inAppManager.onSwitchToForeground();
        verify(inAppManager).syncInApp();
    }

    @Test
    public void testSyncOnLogin() throws Exception {
        IterableInAppManager inAppManagerMock = mock(IterableInAppManager.class);
        IterableApi apiSpy = spy(new IterableApi());
        doReturn(inAppManagerMock).when(apiSpy).getInAppManager();
        apiSpy.setEmail("test@email.com");
        verify(inAppManagerMock).syncInApp();
    }

}
