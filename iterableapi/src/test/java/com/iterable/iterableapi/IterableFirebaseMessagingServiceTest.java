package com.iterable.iterableapi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.iterable.iterableapi.unit.BaseTest;
import com.iterable.iterableapi.unit.IterableTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.MockRepository;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

import okhttp3.mockwebserver.MockWebServer;

import static com.iterable.iterableapi.unit.IterableTestUtils.bundleToMap;
import static com.iterable.iterableapi.unit.IterableTestUtils.getMapFromJsonResource;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@PrepareForTest({IterableNotificationBuilder.class, NotificationCompat.class})
public class IterableFirebaseMessagingServiceTest extends BaseTest {

    private MockWebServer server;
    private IterableApi originalApi;
    private IterableApi apiMock;
    private FirebaseInstanceId mockInstanceId;

    private ServiceController<IterableFirebaseMessagingService> controller;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        controller = Robolectric.buildService(IterableFirebaseMessagingService.class);
        Intent intent = new Intent(RuntimeEnvironment.application, IterableFirebaseMessagingService.class);
        controller.withIntent(intent).startCommand(0, 0);

        originalApi = IterableApi.sharedInstance;
        apiMock = spy(IterableApi.sharedInstance);
        IterableApi.sharedInstance = apiMock;

        mockInstanceId = mock(FirebaseInstanceId.class);
        PowerMockito.stub(PowerMockito.method(FirebaseInstanceId.class, "getInstance")).toReturn(mockInstanceId);
        PowerMockito.stub(PowerMockito.method(IterablePushRegistration.Util.class, "getFirebaseResouceId")).toReturn(1);
    }

    @After
    public void tearDown() throws Exception {
        controller.destroy();
        IterableApi.sharedInstance = originalApi;
        MockRepository.remove(FirebaseInstanceId.class);
        MockRepository.remove(IterablePushRegistration.Util.class);

        server.shutdown();
        server = null;
    }

    @Test
    public void testOnMessageReceived() throws Exception {
        PowerMockito.mockStatic(IterableNotificationBuilder.class);

        RemoteMessage.Builder builder = new RemoteMessage.Builder("1234@gcm.googleapis.com");
        builder.setData(getMapFromJsonResource("push_payload_custom_action.json"));
        controller.get().onMessageReceived(builder.build());

        PowerMockito.verifyStatic(IterableNotificationBuilder.class);
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        IterableNotificationBuilder.createNotification(eq(RuntimeEnvironment.application), bundleCaptor.capture());
        assertTrue(bundleToMap(bundleCaptor.getValue()).equals(getMapFromJsonResource("push_payload_custom_action.json")));
    }

}
