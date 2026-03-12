package com.iterable.iterableapi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

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

        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(IterableConstants.SHARED_PREFS_AUTO_RETRY_KEY, autoRetryEnabled)
                .apply();

        // Initialize directly without calling setEmail to avoid triggering an async
        // auth flow. The null token from the mock handler would race with the test,
        // and the resulting syncInApp() call would send unexpected requests to the
        // mock server, breaking assertions that check for no requests.
        IterableConfig config = new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setAuthHandler(mockAuthHandler)
                .build();
        IterableApi.initialize(context, IterableTestUtils.apiKey, config);
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

        // Register our test taskRunner as a listener so it gets notified
        IterableApi.getInstance().getAuthManager().addAuthTokenReadyListener(taskRunner);

        // Simulate auth token becoming ready: INVALID → VALID via setIsLastAuthTokenValid(true).
        // This transitions auth state and notifies listeners (including taskRunner).
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);

        IterableApi.getInstance().getAuthManager().setIsLastAuthTokenValid(true);
        runHandlerTasks(taskRunner);

        // Now request should be made since auth is valid again
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

    @Test
    public void testAutoRetryEnabled_UsesCurrentLiveAuthToken() throws Exception {
        initApiWithAutoRetry(true);

        // Create a task with a stale auth token stored in the DB
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", "stale_token_from_db", null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // Verify that fromJSON with authTokenOverride replaces the stale token.
        // We verify by checking the deserialized request object directly.
        JSONObject taskJson = request.toJSONObject();
        IterableApiRequest deserializedRequest = IterableApiRequest.fromJSON(taskJson, "fresh_live_token", null, null);
        assertEquals("fromJSON should use authTokenOverride instead of stored token",
                "fresh_live_token", deserializedRequest.authToken);

        // Also verify that without override, the original stale token is used
        IterableApiRequest deserializedWithoutOverride = IterableApiRequest.fromJSON(taskJson, null, null, null);
        assertEquals("fromJSON without override should use stored token",
                "stale_token_from_db", deserializedWithoutOverride.authToken);
    }

    @Test
    public void testAutoRetryEnabled_MultipleTasksInQueue_PausesAfterFirstJwtFailure() throws Exception {
        initApiWithAutoRetry(true);

        // Create 3 tasks in the queue
        IterableApiRequest request1 = new IterableApiRequest("apiKey", "api/test1", new JSONObject(), "POST", null, null, null);
        IterableApiRequest request2 = new IterableApiRequest("apiKey", "api/test2", new JSONObject(), "POST", null, null, null);
        IterableApiRequest request3 = new IterableApiRequest("apiKey", "api/test3", new JSONObject(), "POST", null, null, null);

        IterableTask task1 = new IterableTask("task1", IterableTaskType.API, request1.toJSONObject().toString());
        IterableTask task2 = new IterableTask("task2", IterableTaskType.API, request2.toJSONObject().toString());
        IterableTask task3 = new IterableTask("task3", IterableTaskType.API, request3.toJSONObject().toString());

        // Return task1 first, then task2, then task3
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task1).thenReturn(task2).thenReturn(task3).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // First task gets a 401 JWT error
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(createJwt401ResponseBody()));
        // Enqueue success responses for task2 and task3 (should NOT be reached)
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        // Only one request should have been made (task1)
        RecordedRequest recordedRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest1);
        assertEquals("/api/test1", recordedRequest1.getPath());

        // Task2 and task3 should NOT have been attempted
        RecordedRequest recordedRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("Processing should pause after first JWT failure — task2 should not be attempted", recordedRequest2);

        // No tasks should be deleted (task1 retained for retry, task2/task3 never processed)
        verify(mockTaskStorage, never()).deleteTask(any(String.class));

        // Auth state should be INVALID
        assertEquals(IterableAuthManager.AuthState.INVALID, IterableApi.getInstance().getAuthManager().getAuthState());
    }

    @Test
    public void testAuthTokenReadyListener_NotifiedOnStateTransitionFromInvalid() {
        initApiWithAutoRetry(true);
        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();

        // Use a mock listener to verify notification
        IterableAuthManager.AuthTokenReadyListener mockListener = mock(IterableAuthManager.AuthTokenReadyListener.class);
        authManager.addAuthTokenReadyListener(mockListener);

        // UNKNOWN → INVALID: no notification expected
        authManager.setAuthTokenInvalid();
        verify(mockListener, never()).onAuthTokenReady();

        // INVALID → VALID (via setIsLastAuthTokenValid): notification expected
        authManager.setIsLastAuthTokenValid(true);
        verify(mockListener, times(1)).onAuthTokenReady();

        // VALID → INVALID: no notification expected
        authManager.setAuthTokenInvalid();
        verify(mockListener, times(1)).onAuthTokenReady(); // still just 1

        // INVALID → INVALID: no notification expected
        authManager.setAuthTokenInvalid();
        verify(mockListener, times(1)).onAuthTokenReady(); // still just 1

        // INVALID → UNKNOWN (simulating handleAuthTokenSuccess path via setIsLastAuthTokenValid(false)
        // then a new token obtained): this doesn't trigger because setIsLastAuthTokenValid(false) doesn't change auth state.
        // The actual INVALID→UNKNOWN transition happens inside handleAuthTokenSuccess when authHandler returns a token.
        // We can verify INVALID → VALID again as the primary production path.
        authManager.setIsLastAuthTokenValid(true);
        verify(mockListener, times(2)).onAuthTokenReady();

        // Cleanup
        authManager.removeAuthTokenReadyListener(mockListener);
    }

    // endregion

    private void runHandlerTasks(IterableTaskRunner taskRunner) throws InterruptedException {
        shadowOf(taskRunner.handler.getLooper()).idle();
    }
}
