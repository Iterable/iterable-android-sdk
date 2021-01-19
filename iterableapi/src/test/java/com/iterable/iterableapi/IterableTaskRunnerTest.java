package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class IterableTaskRunnerTest {
    private IterableTaskRunner taskRunner;
    private IterableTaskStorage mockTaskStorage;
    private IterableActivityMonitor mockActivityMonitor;
    private IterableNetworkConnectivityManager mockNetworkConnectivityManager;
    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockActivityMonitor = mock(IterableActivityMonitor.class);
        mockNetworkConnectivityManager = mock(IterableNetworkConnectivityManager.class);
        taskRunner = new IterableTaskRunner(mockTaskStorage, mockActivityMonitor, mockNetworkConnectivityManager);
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testRunOnTaskCreatedMakesApiRequest() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/api/test", recordedRequest.getPath());
        verify(mockTaskStorage).deleteTask(any(String.class));
    }

    @Test
    public void testRunOnTaskCreatedCallsCompletionListener() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        shadowOf(getMainLooper()).idle();
        verify(taskCompletedListener).onTaskCompleted(any(String.class), eq(IterableTaskRunner.TaskResult.SUCCESS), any(IterableApiResponse.class));
    }

    @Test
    public void testNoRequestsWhenOffline() throws Exception {
        clearInvocations(mockTaskStorage);
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(false);
        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        taskRunner.onTaskCreated(null);

        runHandlerTasks(taskRunner);
        verify(mockNetworkConnectivityManager, times(1)).isConnected();
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);
        shadowOf(getMainLooper()).idle();
        verifyNoInteractions(taskCompletedListener);
        verifyNoInteractions(mockTaskStorage);
    }

    @Test
    public void testIfNetworkCheckedBeforeProcessingTask() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);
        shadowOf(getMainLooper()).idle();
        verify(mockNetworkConnectivityManager, times(2)).isConnected();
    }

    private void runHandlerTasks(IterableTaskRunner taskRunner) throws InterruptedException {
        shadowOf(taskRunner.handler.getLooper()).idle();
    }
}
