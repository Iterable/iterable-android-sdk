package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(TestRunner.class)
public class OfflineRequestProcessorTest extends BaseTest {
    private OfflineRequestProcessor offlineRequestProcessor;
    private IterableTaskRunner mockTaskRunner;
    private IterableTaskStorage mockTaskStorage;


    @Before
    public void setUp() {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockTaskRunner = mock(IterableTaskRunner.class);
        offlineRequestProcessor = new OfflineRequestProcessor(getContext(), mockTaskStorage, mockTaskRunner);
    }

    @Test
    public void testOfflineRequestIsStored() throws JSONException {
        IterableApiRequest request = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, new JSONObject(), "POST", null, null, null);
        offlineRequestProcessor.processPostRequest(request.apiKey, request.resourcePath, request.json, request.authToken, request.successCallback, request.failureCallback);
        verify(mockTaskStorage, times(1)).createTask(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, IterableTaskType.API, request.toJSONObject().toString());
    }

    @Test
    public void testNonOfflineRequestIsNotStored() throws JSONException {
        IterableApiRequest request = new IterableApiRequest("apiKey", IterableConstants.PUSH_DISABLE_AFTER_REGISTRATION, new JSONObject(), "POST", null, null, null);
        offlineRequestProcessor.processPostRequest(request.apiKey, request.resourcePath, request.json, request.authToken, request.successCallback, request.failureCallback);
        verify(mockTaskStorage, times(0)).createTask(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, IterableTaskType.API, request.toJSONObject().toString());
    }

    @Test
    public void testAllOfflineApisUseTaskSchedular() {
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