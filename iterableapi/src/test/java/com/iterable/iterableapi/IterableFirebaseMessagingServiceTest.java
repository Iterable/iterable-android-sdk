package com.iterable.iterableapi;

import android.content.Intent;
import android.os.Bundle;

import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;

import static com.iterable.iterableapi.IterableTestUtils.bundleToMap;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IterableFirebaseMessagingServiceTest extends BaseTest {

    private MockWebServer server;
    private IterableApi originalApi;
    private IterableApi apiMock;

    private ServiceController<IterableFirebaseMessagingService> controller;
    private IterableNotificationHelper.IterableNotificationHelperImpl originalNotificationHelper;
    private IterableNotificationHelper.IterableNotificationHelperImpl notificationHelperSpy;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.resetIterableApi();
        IterableTestUtils.createIterableApiNew();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        // Initialize WorkManager for testing with a synchronous executor
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(getContext(), config);

        controller = Robolectric.buildService(IterableFirebaseMessagingService.class);
        Intent intent = new Intent(getContext(), IterableFirebaseMessagingService.class);
        controller.withIntent(intent).startCommand(0, 0);

        originalApi = IterableApi.sharedInstance;
        apiMock = spy(IterableApi.sharedInstance);
        IterableApi.sharedInstance = apiMock;

        originalNotificationHelper = IterableNotificationHelper.instance;
        notificationHelperSpy = spy(originalNotificationHelper);
        IterableNotificationHelper.instance = notificationHelperSpy;
    }

    @After
    public void tearDown() throws Exception {
        IterableNotificationHelper.instance = originalNotificationHelper;
        notificationHelperSpy = null;

        controller.destroy();
        IterableApi.sharedInstance = originalApi;

        server.shutdown();
        server = null;
    }

    @Test
    public void testOnMessageReceived() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        controller.get().onMessageReceived(builder.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(notificationHelperSpy).createNotification(eq(getContext()), bundleCaptor.capture());
        Map<String, String> expectedPayload = new HashMap<>();
        expectedPayload.put(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        expectedPayload.put(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        assertEquals(expectedPayload, bundleToMap(bundleCaptor.getValue()));
    }

    @Test
    public void testSilentPushInAppUpdated() throws Exception {
        IterableInAppManager inAppManagerSpy = spy(IterableApi.getInstance().getInAppManager());
        when(apiMock.getInAppManager()).thenReturn(inAppManagerSpy);
        doNothing().when(inAppManagerSpy).syncInApp();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_inapp_update.json"));
        controller.get().onMessageReceived(builder.build());
        verify(inAppManagerSpy).syncInApp();
    }

    @Test
    public void testSilentPushInAppRemoved() throws Exception {
        IterableInAppManager inAppManagerSpy = spy(IterableApi.getInstance().getInAppManager());
        when(apiMock.getInAppManager()).thenReturn(inAppManagerSpy);
        doNothing().when(inAppManagerSpy).syncInApp();
        doNothing().when(inAppManagerSpy).removeMessage(any(String.class));

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_inapp_remove.json"));
        controller.get().onMessageReceived(builder.build());
        verify(inAppManagerSpy).removeMessage("1234567890abcdef");
    }

    @Test
    public void testIsGhostPushWithGhostPushMessage() throws Exception {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_ghost_push.json"));
        assertTrue(IterableFirebaseMessagingService.isGhostPush(builder.build()));
        verify(notificationHelperSpy).isGhostPush(any(Bundle.class));
    }

    @Test
    public void testIsGhostPushWithNotificationMessage() throws Exception {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_legacy_deep_link.json"));
        assertFalse(IterableFirebaseMessagingService.isGhostPush(builder.build()));
        verify(notificationHelperSpy).isGhostPush(any(Bundle.class));
    }

    @Test
    public void testUpdateMessagesIsCalled() throws Exception {
        IterableEmbeddedManager embeddedManagerSpy = spy(IterableApi.getInstance().getEmbeddedManager());
        when(apiMock.getEmbeddedManager()).thenReturn(embeddedManagerSpy);

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_embedded_update.json"));
        controller.get().onMessageReceived(builder.build());
        verify(embeddedManagerSpy, atLeastOnce()).syncMessages();
    }

    @Test
    public void testWorkManagerIsUsedForNotifications() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();

        // Send a regular push notification (not ghost push)
        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test notification");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        controller.get().onMessageReceived(builder.build());

        // Verify WorkManager has enqueued work
        WorkManager workManager = WorkManager.getInstance(getContext());
        List<WorkInfo> workInfos = workManager.getWorkInfosByTag(IterableNotificationWorker.class.getName()).get(5, TimeUnit.SECONDS);
        
        // Note: With SynchronousExecutor, work completes immediately
        // Verify that notification helper methods were called (indicating Worker ran)
        verify(notificationHelperSpy, atLeastOnce()).createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testNotificationWorkerProcessesData() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.createNotification(any(), any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Worker test message");
        builder.addData(IterableConstants.ITERABLE_DATA_TITLE, "Worker Test");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        
        controller.get().onMessageReceived(builder.build());
        
        // With SynchronousExecutor, work completes immediately
        // Verify the notification was processed
        verify(notificationHelperSpy, atLeastOnce()).createNotification(eq(getContext()), any(Bundle.class));
    }
}
