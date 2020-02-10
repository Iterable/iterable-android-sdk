package com.iterable.iterableapi;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockWebServer;

import static com.iterable.iterableapi.IterableTestUtils.bundleToMap;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        controller = Robolectric.buildService(IterableFirebaseMessagingService.class);
        Intent intent = new Intent(RuntimeEnvironment.application, IterableFirebaseMessagingService.class);
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
        verify(notificationHelperSpy).createNotification(eq(RuntimeEnvironment.application), bundleCaptor.capture());
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

}
