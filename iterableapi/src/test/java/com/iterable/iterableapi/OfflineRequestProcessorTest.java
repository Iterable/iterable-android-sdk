package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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
        String[] offlineApis = new String[]{
                IterableConstants.ENDPOINT_TRACK,
                IterableConstants.ENDPOINT_TRACK_PUSH_OPEN,
                IterableConstants.ENDPOINT_TRACK_PURCHASE,
                IterableConstants.ENDPOINT_TRACK_INAPP_OPEN,
                IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
                IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE,
                IterableConstants.ENDPOINT_TRACK_INBOX_SESSION,
                IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY,
                IterableConstants.ENDPOINT_INAPP_CONSUME};
        for (String uri : offlineApis) {
            assertTrue(offlineRequestProcessor.isRequestOfflineCompatible(uri));
        }
    }
}
