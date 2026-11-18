package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.work.Configuration;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.impl.model.WorkSpec;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class IterableNotificationFlowTest extends BaseTest {

    private static final int IMAGE_PUSH_NOTIFICATION_ID = Math.abs("abc123imagepush".hashCode());

    private MockWebServer server;
    private IterableNotificationHelper.IterableNotificationHelperImpl helperSpy;
    private IterableNotificationHelper.IterableNotificationHelperImpl originalHelper;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.resetIterableApi();
        IterableTestUtils.createIterableApiNew();

        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(getContext(), config);

        originalHelper = IterableNotificationHelper.instance;
        helperSpy = spy(originalHelper);
        IterableNotificationHelper.instance = helperSpy;
    }

    @After
    public void tearDown() throws Exception {
        IterableNotificationHelper.instance = originalHelper;
        if (server != null) {
            server.shutdown();
        }
    }

    // ========================================================================
    // MESSAGE VALIDATION TESTS
    // ========================================================================

    @Test
    public void testIterablePushIsRecognized() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");

        boolean isIterable = IterableFirebaseMessagingService.handleMessageReceived(
                getContext(), builder.build());

        assertTrue("Message with ITERABLE_DATA_KEY should be recognized", isIterable);
    }

    @Test
    public void testNonIterablePushIsIgnored() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData("some_other_key", "value");

        boolean isIterable = IterableFirebaseMessagingService.handleMessageReceived(
                getContext(), builder.build());

        assertFalse("Message without ITERABLE_DATA_KEY should be ignored", isIterable);
    }

    @Test
    public void testEmptyMessageIsIgnored() {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");

        boolean isIterable = IterableFirebaseMessagingService.handleMessageReceived(
                getContext(), builder.build());

        assertFalse("Empty message should be ignored", isIterable);
    }

    @Test
    public void testGhostPushIsDetected() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_ghost_push.json"));

        boolean isGhost = IterableFirebaseMessagingService.isGhostPush(builder.build());

        assertTrue("Ghost push should be detected", isGhost);
    }

    @Test
    public void testRegularPushIsNotGhost() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test");

        boolean isGhost = IterableFirebaseMessagingService.isGhostPush(builder.build());

        assertFalse("Regular push should not be ghost", isGhost);
    }

    // ========================================================================
    // NOTIFICATION CREATION TESTS
    // ========================================================================

    @Test
    public void testNotificationBuilderIsCreatedForValidPush() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test body");

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(helperSpy).createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testNotificationBuilderNotCreatedForGhostPush() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_ghost_push.json"));

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(helperSpy, never()).createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testNotificationBuilderNotCreatedForEmptyBody() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        // No body

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(helperSpy, never()).createNotification(any(), any(Bundle.class));
    }

    // ========================================================================
    // NOTIFICATION POSTING TESTS
    // ========================================================================

    @Test
    public void testNotificationIsPostedForValidPush() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY,
                IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test");

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(helperSpy).postNotificationOnDevice(any(), any(IterableNotificationBuilder.class));
    }

    @Test
    public void testNotificationNotPostedForGhostPush() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_ghost_push.json"));

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(helperSpy, never()).postNotificationOnDevice(any(), any());
    }

    // ========================================================================
    // GHOST PUSH ACTION TESTS
    // ========================================================================

    @Test
    public void testInAppUpdateActionIsTriggered() throws Exception {
        IterableInAppManager inAppManager = org.mockito.Mockito.mock(IterableInAppManager.class);
        IterableApi apiMock = spy(IterableApi.sharedInstance);
        when(apiMock.getInAppManager()).thenReturn(inAppManager);
        IterableApi.sharedInstance = apiMock;

        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_inapp_update.json"));

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(inAppManager).syncInApp();
    }

    @Test
    public void testInAppRemoveActionIsTriggered() throws Exception {
        IterableInAppManager inAppManager = org.mockito.Mockito.mock(IterableInAppManager.class);
        IterableApi apiMock = spy(IterableApi.sharedInstance);
        when(apiMock.getInAppManager()).thenReturn(inAppManager);
        IterableApi.sharedInstance = apiMock;

        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_inapp_remove.json"));

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(inAppManager).removeMessage("1234567890abcdef");
    }

    @Test
    public void testEmbeddedUpdateActionIsTriggered() throws Exception {
        IterableEmbeddedManager embeddedManager = org.mockito.Mockito.mock(IterableEmbeddedManager.class);
        IterableApi apiMock = spy(IterableApi.sharedInstance);
        when(apiMock.getEmbeddedManager()).thenReturn(embeddedManager);
        IterableApi.sharedInstance = apiMock;

        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.setData(IterableTestUtils.getMapFromJsonResource("push_payload_embedded_update.json"));

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        verify(embeddedManager).syncMessages();
    }

    // ========================================================================
    // DATA PRESERVATION TESTS
    // ========================================================================

    @Test
    public void testNotificationTitleIsPreserved() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String expectedTitle = "Test Title";
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_TITLE, expectedTitle);
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Body");

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        assertEquals(expectedTitle, bundleCaptor.getValue().getString(IterableConstants.ITERABLE_DATA_TITLE));
    }

    @Test
    public void testNotificationBodyIsPreserved() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String expectedBody = "Test Body Content";
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, expectedBody);

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        assertEquals(expectedBody, bundleCaptor.getValue().getString(IterableConstants.ITERABLE_DATA_BODY));
    }

    @Test
    public void testNotificationDataKeyIsPreserved() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String expectedData = "{\"campaignId\":123}";
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, expectedData);
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Body");

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        assertEquals(expectedData, bundleCaptor.getValue().getString(IterableConstants.ITERABLE_DATA_KEY));
    }

    @Test
    public void testCustomFieldsArePreserved() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String customValue = "customValue123";
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Body");
        builder.addData("customField", customValue);

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        assertEquals(customValue, bundleCaptor.getValue().getString("customField"));
    }

    // ========================================================================
    // DIRECT vs WORKMANAGER ROUTING TESTS
    // ========================================================================

    @Test
    public void testPlainTextNotificationIsHandledDirectly() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.hasAttachmentUrl(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY,
                IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test");

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        // Direct path: createNotification and postNotificationOnDevice called without WorkManager
        verify(helperSpy).createNotification(any(), any(Bundle.class));
        verify(helperSpy).postNotificationOnDevice(any(), any(IterableNotificationBuilder.class));
    }

    @Test
    public void testImageNotificationUsesWorkManager() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.hasAttachmentUrl(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY,
                IterableTestUtils.getResourceString("push_payload_image_push.json"));
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Image push");

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        // WorkManager path: Worker eventually calls createNotification (via SynchronousExecutor)
        verify(helperSpy).createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testMultiplePlainTextNotificationsAreHandledDirectly() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.hasAttachmentUrl(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        for (int i = 0; i < 3; i++) {
            RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
            builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
            builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test " + i);
            IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());
        }

        verify(helperSpy, org.mockito.Mockito.times(3)).createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testNotificationDataIsPreservedOnDirectPath() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.hasAttachmentUrl(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String testTitle = "Direct Path Title";
        String testBody = "Direct Path Body";

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_TITLE, testTitle);
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, testBody);

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        Bundle capturedBundle = bundleCaptor.getValue();
        assertEquals(testTitle, capturedBundle.getString(IterableConstants.ITERABLE_DATA_TITLE));
        assertEquals(testBody, capturedBundle.getString(IterableConstants.ITERABLE_DATA_BODY));
    }

    @Test
    @Config(sdk = {31, 34})
    public void testImageDownloadFailurePostsNotificationThenRetryAddsImage() throws Exception {
        MockWebServer imageServer = new MockWebServer();
        imageServer.enqueue(new MockResponse().setResponseCode(503));
        imageServer.enqueue(IterableTestUtils.getImageResponse());
        imageServer.start();
        WorkManager workManagerSpy = spy(WorkManager.getInstance(getContext()));
        try (MockedStatic<WorkManager> workManagerStatic = mockStatic(WorkManager.class)) {
            workManagerStatic.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(workManagerSpy);
            IterableFirebaseMessagingService.handleMessageReceived(getContext(),
                    imagePush(imageServer.url("/image.png").toString()));
        }

        StatusBarNotification[] shown = getNotificationManager().getActiveNotifications();
        assertEquals(1, shown.length);
        assertEquals(IMAGE_PUSH_NOTIFICATION_ID, shown[0].getId());
        assertFalse("Notification should be posted without the image",
                shown[0].getNotification().extras.containsKey(Notification.EXTRA_PICTURE));

        List<WorkInfo> retries = WorkManager.getInstance(getContext())
                .getWorkInfosForUniqueWork(
                        IterableNotificationWorkScheduler.getImageRetryWorkName(IMAGE_PUSH_NOTIFICATION_ID))
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, retries.size());
        assertEquals(WorkInfo.State.ENQUEUED, retries.get(0).getState());

        ArgumentCaptor<OneTimeWorkRequest> retryCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(workManagerSpy).enqueueUniqueWork(
                eq(IterableNotificationWorkScheduler.getImageRetryWorkName(IMAGE_PUSH_NOTIFICATION_ID)),
                eq(ExistingWorkPolicy.KEEP),
                retryCaptor.capture());
        WorkSpec retrySpec = retryCaptor.getValue().getWorkSpec();
        assertTrue("Image retry should be expedited", retrySpec.expedited);
        assertEquals(NetworkType.CONNECTED, retrySpec.constraints.getRequiredNetworkType());
        assertEquals(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST, retrySpec.outOfQuotaPolicy);

        WorkManagerTestInitHelper.getTestDriver(getContext()).setAllConstraintsMet(retries.get(0).getId());
        long postTime = retrySpec.input.getLong(IterableNotificationImageRetryWorker.KEY_ORIGINAL_POST_TIME, 0L);
        assertTrue("Retry should wait out the DNS failure cache", System.currentTimeMillis() - postTime
                >= IterableNotificationImageRetryWorker.MIN_RETRY_DELAY_AFTER_POST_MILLIS);

        shown = getNotificationManager().getActiveNotifications();
        assertEquals(1, shown.length);
        assertTrue("Retry should add the image",
                shown[0].getNotification().extras.containsKey(Notification.EXTRA_PICTURE));
        assertTrue((shown[0].getNotification().flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
        imageServer.shutdown();
    }

    @Test
    public void testImageDownloadFailureWithoutMessageIdDoesNotScheduleRetry() throws Exception {
        JSONObject payload = new JSONObject(
                IterableTestUtils.getImagePushPayload(IterableTestUtils.getUnreachableUrl()));
        payload.remove(IterableConstants.KEY_MESSAGE_ID);

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), push(payload.toString()));

        assertEquals(1, getNotificationManager().getActiveNotifications().length);
        assertTrue("No image retry should be scheduled without a messageId", getImageRetryWork().isEmpty());
    }

    @Test
    @Config(sdk = 30)
    public void testImageDownloadFailureBelowApi31DoesNotScheduleRetry() throws Exception {
        IterableFirebaseMessagingService.handleMessageReceived(getContext(),
                imagePush(IterableTestUtils.getUnreachableUrl()));

        StatusBarNotification[] shown = getNotificationManager().getActiveNotifications();
        assertEquals(1, shown.length);
        assertFalse(shown[0].getNotification().extras.containsKey(Notification.EXTRA_PICTURE));
        assertTrue("No image retry should be scheduled below API 31", getImageRetryWork().isEmpty());
    }

    @Test
    public void testSuccessfulImageDownloadDoesNotScheduleRetry() throws Exception {
        String imageUrl = getClass().getClassLoader().getResource("350x150.png").toString();

        IterableFirebaseMessagingService.handleMessageReceived(getContext(), imagePush(imageUrl));

        StatusBarNotification[] shown = getNotificationManager().getActiveNotifications();
        assertEquals(1, shown.length);
        assertTrue(shown[0].getNotification().extras.containsKey(Notification.EXTRA_PICTURE));
        assertTrue("No image retry should be scheduled when the image loads", getImageRetryWork().isEmpty());
    }

    private RemoteMessage imagePush(String imageUrl) throws Exception {
        return push(IterableTestUtils.getImagePushPayload(imageUrl));
    }

    private RemoteMessage push(String iterablePayload) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, iterablePayload);
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Image push");
        return builder.build();
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private List<WorkInfo> getImageRetryWork() throws Exception {
        return WorkManager.getInstance(getContext())
                .getWorkInfosByTag(IterableNotificationImageRetryWorker.class.getName())
                .get(5, TimeUnit.SECONDS);
    }
}
