package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class OfflineRequestProcessorTest extends BaseTest {
    private OfflineRequestProcessor offlineRequestProcessor;
    private IterableTaskRunner mockTaskRunner;
    private TaskScheduler mockTaskScheduler;
    private IterableTaskStorage mockTaskStorage;
    private HealthMonitor mockHealthMonitor;

    @Before
    public void setUp() {
        mockTaskRunner = mock(IterableTaskRunner.class);
        mockTaskScheduler = mock(TaskScheduler.class);
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockHealthMonitor = mock(HealthMonitor.class);
        offlineRequestProcessor = new OfflineRequestProcessor(mockTaskScheduler, mockTaskRunner, mockTaskStorage, mockHealthMonitor);
    }

    @Test
    public void testOfflineRequestIsStored() {
        IterableApiRequest request = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, new JSONObject(), "POST", null, null, null);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
        offlineRequestProcessor.processPostRequest(request.apiKey, request.resourcePath, request.json, request.authToken, request.successCallback, request.failureCallback);
        verify(mockTaskScheduler).scheduleTask(any(IterableApiRequest.class), isNull(), isNull());
    }

    @Test
    public void testNonOfflineRequestIsNotStored() {
        IterableApiRequest request = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_UPDATE_EMAIL, new JSONObject(), "POST", null, null, null);
        offlineRequestProcessor.processPostRequest(request.apiKey, request.resourcePath, request.json, request.authToken, request.successCallback, request.failureCallback);
        verifyNoInteractions(mockTaskScheduler);
    }

    @Test
    public void testOnlineRequestWhenDBError() {
        IterableApiRequest request = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, new JSONObject(), "POST", null, null, null);
        when(mockHealthMonitor.canSchedule()).thenReturn(false);
        offlineRequestProcessor.processPostRequest(request.apiKey, request.resourcePath, request.json, request.authToken, request.successCallback, request.failureCallback);
        verifyNoInteractions(mockTaskScheduler);
    }

    @Test
    public void testAllOfflineApisUseTaskScheduler() {
        for (String uri : EXPECTED_OFFLINE_APIS) {
            assertTrue(uri + " should be offline compatible", offlineRequestProcessor.isRequestOfflineCompatible(uri));
        }
    }

    /**
     * The offline API set encodes a product decision about which requests survive a network
     * outage, so it must not drift silently. ENDPOINT_DISABLE_DEVICE was added in 3.5.16 and
     * quietly dropped again in 3.7.0 because nothing asserted its membership. Any edit to the
     * set now has to be made here too, which forces the change to be reviewed deliberately.
     */
    @Test
    public void testOfflineApiSetMembershipIsExact() {
        assertEquals(EXPECTED_OFFLINE_APIS, OfflineRequestProcessor.getOfflineApiSet());
    }

    @Test
    public void testLogoutPreservesQueuedDisableDeviceTasks() {
        offlineRequestProcessor.onLogout(null);
        verify(mockTaskStorage).deleteAllTasksExcept(IterableConstants.ENDPOINT_DISABLE_DEVICE);
        verify(mockTaskStorage, never()).deleteAllTasks();
    }

    private static final Set<String> EXPECTED_OFFLINE_APIS = new HashSet<>(Arrays.asList(
            IterableConstants.ENDPOINT_TRACK,
            IterableConstants.ENDPOINT_TRACK_PUSH_OPEN,
            IterableConstants.ENDPOINT_TRACK_PURCHASE,
            IterableConstants.ENDPOINT_TRACK_INAPP_OPEN,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
            IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE,
            IterableConstants.ENDPOINT_TRACK_INBOX_SESSION,
            IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY,
            IterableConstants.ENDPOINT_INAPP_CONSUME,
            IterableConstants.ENDPOINT_UPDATE_CART,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK,
            IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION,
            IterableConstants.ENDPOINT_DISABLE_DEVICE,
            IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN
    ));
}
