package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

public class IterableInAppManagerSyncTest extends BaseTest {

    @Mock
    private IterableApi iterableApiMock;
    @Mock
    private IterableInAppHandler handlerMock;
    @Mock
    private IterableInAppStorage storageMock;
    @Mock
    private IterableActivityMonitor activityMonitorMock;
    @Mock
    private IterableInAppDisplayer inAppDisplayerMock;

    private IterableInAppManager inAppManager;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        inAppManager = spy(new IterableInAppManager(iterableApiMock, handlerMock, 30.0, storageMock, activityMonitorMock, inAppDisplayerMock));
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
        IterableApi.sharedInstance = new IterableApi(inAppManagerMock);
        IterableApi.initialize(getApplicationContext(), "apiKey");
        IterableApi.getInstance().setEmail("test@email.com");
        verify(inAppManagerMock).syncInApp();
    }

    @Test
    public void testRecalledMessagesAreConsumed() throws Exception {
        // Create a test message in local storage
        IterableInAppMessage testMessage = InAppTestUtils.getTestInboxInAppWithId("test-message-1");
        doReturn(testMessage).when(storageMock).getMessage("test-message-1");

        // Create a storage with only this message
        doReturn(Arrays.asList(testMessage)).when(storageMock).getMessages();

        // Setup the API to return empty message list (simulating recall)
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                IterableHelper.IterableActionHandler handler = invocation.getArgument(1);
                // Return an empty message list
                handler.execute("{\"" + IterableConstants.ITERABLE_IN_APP_MESSAGE + "\": []}");
                return null;
            }
        }).when(iterableApiMock).getInAppMessages(any(Integer.class), any(IterableHelper.IterableActionHandler.class));

        // Verify message is not consumed initially
        assertFalse(testMessage.isConsumed());

        // Sync with remote queue
        inAppManager.syncInApp();

        // Verify that the message was marked as consumed
        assertTrue(testMessage.isConsumed());

        // Verify that inAppConsume was called
        verify(iterableApiMock).inAppConsume(testMessage, null, null, null, null);
    }

}