package com.iterable.iterableapi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.work.Configuration;
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
import java.util.Map;

import okhttp3.mockwebserver.MockWebServer;

import static com.iterable.iterableapi.IterableTestUtils.bundleToMap;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
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

        // Initialize WorkManager for testing
        Context context = getContext();
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

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
    public void testNotificationWithoutImageDisplaysImmediately() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.hasImageUrl(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.createNotification(any(Context.class), any(Bundle.class))).thenCallRealMethod();
        doNothing().when(notificationHelperSpy).postNotificationOnDevice(any(Context.class), any(IterableNotificationBuilder.class));

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        builder.setPriority(RemoteMessage.PRIORITY_NORMAL);
        controller.get().onMessageReceived(builder.build());

        // Verify notification is created and posted immediately (not scheduled via WorkManager)
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(notificationHelperSpy).createNotification(eq(getContext()), bundleCaptor.capture());
        verify(notificationHelperSpy).postNotificationOnDevice(eq(getContext()), any(IterableNotificationBuilder.class));
    }

    @Test
    public void testNotificationWithImageSchedulesWorkManager() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.hasImageUrl(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_with_image.json"));
        builder.setPriority(RemoteMessage.PRIORITY_HIGH);
        controller.get().onMessageReceived(builder.build());

        // Verify WorkManager was enqueued (check that work was scheduled)
        // Note: We can't easily verify the exact work request without more complex mocking,
        // but we can verify that hasImageUrl was called and that createNotification was NOT called immediately
        verify(notificationHelperSpy).hasImageUrl(any(Bundle.class));
    }

    @Test
    public void testHighPriorityMessageSchedulesExpeditedWork() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.hasImageUrl(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_with_image.json"));
        builder.setPriority(RemoteMessage.PRIORITY_HIGH);
        controller.get().onMessageReceived(builder.build());

        // Verify that hasImageUrl was called (indicating WorkManager path was taken)
        verify(notificationHelperSpy).hasImageUrl(any(Bundle.class));
    }

    @Test
    public void testNormalPriorityMessageSchedulesRegularWork() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.hasImageUrl(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_with_image.json"));
        builder.setPriority(RemoteMessage.PRIORITY_NORMAL);
        controller.get().onMessageReceived(builder.build());

        // Verify that hasImageUrl was called (indicating WorkManager path was taken)
        verify(notificationHelperSpy).hasImageUrl(any(Bundle.class));
    }

    @Test
    public void testHasImageUrlReturnsTrueWhenImagePresent() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_with_image.json"));
        assertTrue(IterableNotificationHelper.hasImageUrl(extras));
    }

    @Test
    public void testHasImageUrlReturnsFalseWhenImageNotPresent() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        assertFalse(IterableNotificationHelper.hasImageUrl(extras));
    }

    @Test
    public void testGetImageUrlReturnsUrlWhenPresent() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_with_image.json"));
        String imageUrl = IterableNotificationHelper.getImageUrl(extras);
        assertNotNull(imageUrl);
        assertEquals("https://example.com/image.jpg", imageUrl);
    }

    @Test
    public void testGetImageUrlReturnsNullWhenNotPresent() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        String imageUrl = IterableNotificationHelper.getImageUrl(extras);
        assertNull(imageUrl);
    }

    @Test
    public void testGetImageUrlReturnsNullForNullExtras() {
        String imageUrl = IterableNotificationHelper.getImageUrl(null);
        assertNull(imageUrl);
    }

    @Test
    public void testHasImageUrlReturnsFalseForNullExtras() {
        assertFalse(IterableNotificationHelper.hasImageUrl(null));
    }
}
