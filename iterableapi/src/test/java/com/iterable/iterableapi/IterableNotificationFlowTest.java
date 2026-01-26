package com.iterable.iterableapi;

import android.os.Bundle;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class IterableNotificationFlowTest extends BaseTest {

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
    // SCHEDULER INTEGRATION TESTS
    // ========================================================================

    @Test
    public void testNotificationUsesWorkManagerScheduling() throws Exception {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, 
                IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test");
        
        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());
        
        // Verify notification was posted (via WorkManager with SynchronousExecutor)
        verify(helperSpy).postNotificationOnDevice(any(), any(IterableNotificationBuilder.class));
    }

    @Test
    public void testSchedulerHandlesMultipleNotifications() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        // Send three notifications
        for (int i = 0; i < 3; i++) {
            RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
            builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
            builder.addData(IterableConstants.ITERABLE_DATA_BODY, "Test " + i);
            
            IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());
        }
        
        // Verify all three notifications were created
        verify(helperSpy, org.mockito.Mockito.times(3))
                .createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testSchedulerPreservesNotificationDataThroughWorkManager() {
        when(helperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.isEmptyBody(any(Bundle.class))).thenCallRealMethod();
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String testTitle = "Scheduler Test Title";
        String testBody = "Scheduler Test Body";
        
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@gcm.googleapis.com");
        builder.addData(IterableConstants.ITERABLE_DATA_KEY, "{}");
        builder.addData(IterableConstants.ITERABLE_DATA_TITLE, testTitle);
        builder.addData(IterableConstants.ITERABLE_DATA_BODY, testBody);
        
        IterableFirebaseMessagingService.handleMessageReceived(getContext(), builder.build());
        
        // Verify data was preserved through the scheduler -> worker -> notification flow
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());
        
        Bundle capturedBundle = bundleCaptor.getValue();
        assertEquals("Title should be preserved through scheduler", 
                testTitle, capturedBundle.getString(IterableConstants.ITERABLE_DATA_TITLE));
        assertEquals("Body should be preserved through scheduler",
                testBody, capturedBundle.getString(IterableConstants.ITERABLE_DATA_BODY));
    }
}
