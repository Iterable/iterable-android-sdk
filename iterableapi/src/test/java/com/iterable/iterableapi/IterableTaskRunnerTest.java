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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class IterableTaskRunnerTest extends BaseTest {
    private IterableTaskRunner taskRunner;
    private IterableTaskStorage mockTaskStorage;
    private IterableActivityMonitor mockActivityMonitor;
    private HealthMonitor mockHealthMonitor;
    private IterableNetworkConnectivityManager mockNetworkConnectivityManager;
    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockActivityMonitor = mock(IterableActivityMonitor.class);
        mockNetworkConnectivityManager = mock(IterableNetworkConnectivityManager.class);
        mockHealthMonitor = mock(HealthMonitor.class);
        taskRunner = new IterableTaskRunner(mockTaskStorage, mockActivityMonitor, mockNetworkConnectivityManager, mockHealthMonitor);
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        IterableTestUtils.resetIterableApi();
    }

    @Test
    public void testRunOnTaskCreatedMakesApiRequest() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
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
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
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
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        taskRunner.onTaskCreated(mock(IterableTask.class));
        runHandlerTasks(taskRunner);

        verify(mockNetworkConnectivityManager, times(1)).isConnected();
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);

        shadowOf(getMainLooper()).idle();
        verifyNoInteractions(taskCompletedListener);
        verifyNoInteractions(mockTaskStorage);
    }

    @Test
    public void testNoRequestsWhenInBackground() throws Exception {
        clearInvocations(mockTaskStorage);
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(false);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        verify(mockActivityMonitor, times(1)).isInForeground();
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
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        shadowOf(getMainLooper()).idle();
        verify(mockNetworkConnectivityManager, times(2)).isConnected();
    }

    // region Auto-Retry on JWT Failure Tests

    /**
     * Helper to create a JWT 401 error response body matching IterableRequestTask's JWT error codes.
     */
    private String createJwt401ResponseBody() throws Exception {
        JSONObject body = new JSONObject();
        body.put("code", "InvalidJwtPayload");
        body.put("msg", "jwt token is expired");
        return body.toString();
    }

    /**
     * Helper to initialize IterableApi with autoRetry enabled and a mock auth handler.
     */
    private IterableAuthHandler initApiWithAutoRetry(boolean autoRetryEnabled) {
        IterableApi.sharedInstance = new IterableApi();
        final IterableAuthHandler mockAuthHandler = mock(IterableAuthHandler.class);
        doReturn(null).when(mockAuthHandler).onAuthTokenRequested();

        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder
                        .setAutoRetryOnJwtFailure(autoRetryEnabled)
                        .setAuthHandler(mockAuthHandler);
            }
        });
        return mockAuthHandler;
    }

    @Test
    public void testAutoRetryEnabled_JwtFailure_TaskRetainedInDB() throws Exception {
        initApiWithAutoRetry(true);

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", "expired_token", null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // Server returns 401 with JWT error code
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(createJwt401ResponseBody()));

        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        // Task should NOT be deleted from DB when autoRetry is enabled
        verify(mockTaskStorage, never()).deleteTask(any(String.class));

        // Completion listener should be called with RETRY result
        shadowOf(getMainLooper()).idle();
        verify(taskCompletedListener).onTaskCompleted(any(String.class), eq(IterableTaskRunner.TaskResult.RETRY), any(IterableApiResponse.class));

        // Auth state should be INVALID
        assertEquals(IterableAuthManager.AuthState.INVALID, IterableApi.getInstance().getAuthManager().getAuthState());
    }

    @Test
    public void testAutoRetryDisabled_JwtFailure_TaskDeletedFromDB() throws Exception {
        initApiWithAutoRetry(false);

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", "expired_token", null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // Server returns 401 with JWT error code
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(createJwt401ResponseBody()));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        // Task should be deleted from DB when autoRetry is disabled (existing behavior)
        shadowOf(getMainLooper()).idle();
        verify(mockTaskStorage).deleteTask(any(String.class));
    }

    @Test
    public void testAutoRetryEnabled_ProcessingPausesWhenAuthInvalid() throws Exception {
        initApiWithAutoRetry(true);

        // Mark auth as invalid
        IterableApi.getInstance().getAuthManager().setAuthTokenInvalid();

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        // No request should be made because auth is invalid
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);

        // Task should NOT be deleted since processing was paused
        verify(mockTaskStorage, never()).deleteTask(any(String.class));
    }

    @Test
    public void testAutoRetryEnabled_ProcessingResumesOnAuthTokenReady() throws Exception {
        initApiWithAutoRetry(true);

        // Mark auth as invalid first
        IterableApi.getInstance().getAuthManager().setAuthTokenInvalid();

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // First attempt: auth is invalid, should not process
        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);

        // Now simulate auth token becoming ready (UNKNOWN state, ready to make requests)
        IterableApi.getInstance().getAuthManager().setIsLastAuthTokenValid(false); // Reset state
        // Manually set auth state to UNKNOWN (simulating new token obtained)
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        // Calling onAuthTokenReady should trigger processing
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        taskRunner.onAuthTokenReady();
        runHandlerTasks(taskRunner);

        // Now request should be made
        recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/api/test", recordedRequest.getPath());

        // Task should be deleted after successful execution
        verify(mockTaskStorage).deleteTask(any(String.class));
    }

    @Test
    public void testAutoRetryEnabled_SuccessfulRequest_TaskDeleted() throws Exception {
        initApiWithAutoRetry(true);

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        // Task should be deleted on success even with autoRetry enabled
        verify(mockTaskStorage).deleteTask(any(String.class));

        shadowOf(getMainLooper()).idle();
        verify(taskCompletedListener).onTaskCompleted(any(String.class), eq(IterableTaskRunner.TaskResult.SUCCESS), any(IterableApiResponse.class));
    }

    @Test
    public void testAutoRetryEnabled_Any401_TaskRetainedInDB() throws Exception {
        initApiWithAutoRetry(true);

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // Server returns 401 without JWT-specific error code.
        // In offline context, the API key is valid (task was queued with it),
        // so any 401 is treated as a JWT auth issue and the task is retained.
        JSONObject body401 = new JSONObject();
        body401.put("code", "InvalidApiKey");
        body401.put("msg", "Invalid API key");
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(body401.toString()));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        // Any 401 should retain the task when autoRetry is enabled (offline tasks have valid API keys)
        shadowOf(getMainLooper()).idle();
        verify(mockTaskStorage, never()).deleteTask(any(String.class));
    }

    @Test
    public void testAutoRetryEnabled_Non401Error_TaskDeletedNormally() throws Exception {
        initApiWithAutoRetry(true);

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // Server returns 400 (not 401) - should be treated as a normal failure
        JSONObject body400 = new JSONObject();
        body400.put("msg", "Bad request");
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody(body400.toString()));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        // Non-401 errors should delete the task as a FAILURE
        shadowOf(getMainLooper()).idle();
        verify(mockTaskStorage).deleteTask(any(String.class));
    }

    @Test
    public void testAuthManagerListenerRegistration() {
        initApiWithAutoRetry(true);
        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();

        // Register the task runner as a listener
        authManager.addAuthTokenReadyListener(taskRunner);

        // Auth should be ready initially (UNKNOWN state)
        assertTrue(authManager.isAuthTokenReady());

        // Mark invalid
        authManager.setAuthTokenInvalid();
        assertFalse(authManager.isAuthTokenReady());
        assertEquals(IterableAuthManager.AuthState.INVALID, authManager.getAuthState());

        // Mark valid
        authManager.setIsLastAuthTokenValid(true);
        assertTrue(authManager.isAuthTokenReady());
        assertEquals(IterableAuthManager.AuthState.VALID, authManager.getAuthState());
    }

    // endregion

    private void runHandlerTasks(IterableTaskRunner taskRunner) throws InterruptedException {
        shadowOf(taskRunner.handler.getLooper()).idle();
    }
}
