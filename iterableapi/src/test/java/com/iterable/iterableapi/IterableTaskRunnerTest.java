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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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

    private String createJwt401ResponseBody() throws Exception {
        JSONObject body = new JSONObject();
        body.put("code", "InvalidJwtPayload");
        body.put("msg", "jwt token is expired");
        return body.toString();
    }

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

        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(createJwt401ResponseBody()));

        IterableTaskRunner.TaskCompletedListener taskCompletedListener = mock(IterableTaskRunner.TaskCompletedListener.class);
        taskRunner.addTaskCompletedListener(taskCompletedListener);

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        verify(mockTaskStorage, never()).deleteTask(any(String.class));

        shadowOf(getMainLooper()).idle();
        verify(taskCompletedListener).onTaskCompleted(any(String.class), eq(IterableTaskRunner.TaskResult.RETRY), any(IterableApiResponse.class));

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

        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(createJwt401ResponseBody()));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        shadowOf(getMainLooper()).idle();
        verify(mockTaskStorage).deleteTask(any(String.class));
    }

    @Test
    public void testAutoRetryEnabled_ProcessingPausesWhenAuthInvalid() throws Exception {
        initApiWithAutoRetry(true);

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

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);

        verify(mockTaskStorage, never()).deleteTask(any(String.class));
    }

    @Test
    public void testAutoRetryEnabled_ProcessingResumesOnAuthTokenReady() throws Exception {
        initApiWithAutoRetry(true);

        IterableApi.getInstance().getAuthManager().setAuthTokenInvalid();

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);

        IterableApi.getInstance().getAuthManager().addAuthTokenReadyListener(taskRunner);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);

        IterableApi.getInstance().getAuthManager().setIsLastAuthTokenValid(true);
        runHandlerTasks(taskRunner);

        recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/api/test", recordedRequest.getPath());

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

        JSONObject body400 = new JSONObject();
        body400.put("msg", "Bad request");
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody(body400.toString()));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);

        shadowOf(getMainLooper()).idle();
        verify(mockTaskStorage).deleteTask(any(String.class));
    }

    @Test
    public void testAuthManagerListenerRegistration() {
        initApiWithAutoRetry(true);
        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();

        authManager.addAuthTokenReadyListener(taskRunner);

        assertTrue(authManager.isAuthTokenReady());

        authManager.setAuthTokenInvalid();
        assertFalse(authManager.isAuthTokenReady());
        assertEquals(IterableAuthManager.AuthState.INVALID, authManager.getAuthState());

        authManager.setIsLastAuthTokenValid(true);
        assertTrue(authManager.isAuthTokenReady());
        assertEquals(IterableAuthManager.AuthState.VALID, authManager.getAuthState());
    }

    @Test
    public void testAutoRetryEnabled_UsesCurrentLiveAuthToken() throws Exception {
        initApiWithAutoRetry(true);

        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", "stale_token_from_db", null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        JSONObject taskJson = request.toJSONObject();
        IterableApiRequest deserializedRequest = IterableApiRequest.fromJSON(taskJson, "fresh_live_token", null, null);
        assertEquals("fromJSON should use authTokenOverride instead of stored token",
                "fresh_live_token", deserializedRequest.authToken);

        IterableApiRequest deserializedWithoutOverride = IterableApiRequest.fromJSON(taskJson, null, null, null);
        assertEquals("fromJSON without override should use stored token",
                "stale_token_from_db", deserializedWithoutOverride.authToken);
    }

    @Test
    public void testAutoRetryEnabled_MultipleTasksInQueue_PausesAfterFirstJwtFailure() throws Exception {
        initApiWithAutoRetry(true);

        IterableApiRequest request1 = new IterableApiRequest("apiKey", "api/test1", new JSONObject(), "POST", null, null, null);
        IterableApiRequest request2 = new IterableApiRequest("apiKey", "api/test2", new JSONObject(), "POST", null, null, null);
        IterableApiRequest request3 = new IterableApiRequest("apiKey", "api/test3", new JSONObject(), "POST", null, null, null);

        IterableTask task1 = new IterableTask("task1", IterableTaskType.API, request1.toJSONObject().toString());
        IterableTask task2 = new IterableTask("task2", IterableTaskType.API, request2.toJSONObject().toString());
        IterableTask task3 = new IterableTask("task3", IterableTaskType.API, request3.toJSONObject().toString());

        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task1).thenReturn(task2).thenReturn(task3).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(createJwt401ResponseBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        taskRunner.onTaskCreated(null);
        runHandlerTasks(taskRunner);

        RecordedRequest recordedRequest1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest1);
        assertEquals("/api/test1", recordedRequest1.getPath());

        RecordedRequest recordedRequest2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull("Processing should pause after first JWT failure", recordedRequest2);

        verify(mockTaskStorage, never()).deleteTask(any(String.class));

        assertEquals(IterableAuthManager.AuthState.INVALID, IterableApi.getInstance().getAuthManager().getAuthState());
    }

    @Test
    public void testAuthTokenReadyListener_NotifiedOnStateTransitionFromInvalid() {
        initApiWithAutoRetry(true);
        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();

        IterableAuthManager.AuthTokenReadyListener mockListener = mock(IterableAuthManager.AuthTokenReadyListener.class);
        authManager.addAuthTokenReadyListener(mockListener);

        authManager.setAuthTokenInvalid();
        verify(mockListener, never()).onAuthTokenReady();

        authManager.setIsLastAuthTokenValid(true);
        verify(mockListener, times(1)).onAuthTokenReady();

        authManager.setAuthTokenInvalid();
        verify(mockListener, times(1)).onAuthTokenReady();

        authManager.setAuthTokenInvalid();
        verify(mockListener, times(1)).onAuthTokenReady();

        authManager.setIsLastAuthTokenValid(true);
        verify(mockListener, times(2)).onAuthTokenReady();

        authManager.removeAuthTokenReadyListener(mockListener);
    }

    // endregion

    // region Unauthenticated API Bypass Tests

    @Test
    public void testUnauthenticatedTaskExecutesDuringAuthPause() throws Exception {
        ApiEndpointClassification classification = new ApiEndpointClassification();
        IterableTaskRunner runner = new IterableTaskRunner(mockTaskStorage, mockActivityMonitor, mockNetworkConnectivityManager, mockHealthMonitor, classification);

        IterableApiRequest request = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_DISABLE_DEVICE, new JSONObject(), "POST", null, null, null);
        IterableTask unauthTask = new IterableTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, request.toJSONObject().toString());

        when(mockTaskStorage.getNextScheduledTaskNotRequiringJwt(classification)).thenReturn(unauthTask).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        runner.setIsPausedForAuth(true);
        runner.onTaskCreated(null);
        runHandlerTasks(runner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_DISABLE_DEVICE, recordedRequest.getPath());
        verify(mockTaskStorage).deleteTask(unauthTask.id);
    }

    @Test
    public void testAuthRequiredTaskStaysBlockedDuringAuthPause() throws Exception {
        ApiEndpointClassification classification = new ApiEndpointClassification();
        IterableTaskRunner runner = new IterableTaskRunner(mockTaskStorage, mockActivityMonitor, mockNetworkConnectivityManager, mockHealthMonitor, classification);

        when(mockTaskStorage.getNextScheduledTaskNotRequiringJwt(classification)).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        runner.setIsPausedForAuth(true);
        runner.onTaskCreated(null);
        runHandlerTasks(runner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(recordedRequest);
        verify(mockTaskStorage, never()).deleteTask(any(String.class));
    }

    @Test
    public void testQueueIntegrityAfterAuthPausedProcessing() throws Exception {
        ApiEndpointClassification classification = new ApiEndpointClassification();
        IterableTaskRunner runner = new IterableTaskRunner(mockTaskStorage, mockActivityMonitor, mockNetworkConnectivityManager, mockHealthMonitor, classification);

        IterableApiRequest trackRequestA = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK, new JSONObject("{\"eventName\":\"A\"}"), "POST", null, null, null);
        IterableTask trackTaskA = new IterableTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, trackRequestA.toJSONObject().toString());

        IterableApiRequest disableRequest = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_DISABLE_DEVICE, new JSONObject(), "POST", null, null, null);
        IterableTask disableTask = new IterableTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, disableRequest.toJSONObject().toString());

        IterableApiRequest trackRequestB = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK, new JSONObject("{\"eventName\":\"B\"}"), "POST", null, null, null);
        IterableTask trackTaskB = new IterableTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, trackRequestB.toJSONObject().toString());

        IterableApiRequest trackRequestC = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK, new JSONObject("{\"eventName\":\"C\"}"), "POST", null, null, null);
        IterableTask trackTaskC = new IterableTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, trackRequestC.toJSONObject().toString());

        when(mockTaskStorage.getNextScheduledTaskNotRequiringJwt(classification)).thenReturn(disableTask).thenReturn(null);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        runner.setIsPausedForAuth(true);
        runner.onTaskCreated(null);
        runHandlerTasks(runner);

        verify(mockTaskStorage).deleteTask(disableTask.id);
        verify(mockTaskStorage, never()).deleteTask(trackTaskA.id);
        verify(mockTaskStorage, never()).deleteTask(trackTaskB.id);
        verify(mockTaskStorage, never()).deleteTask(trackTaskC.id);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_DISABLE_DEVICE, recordedRequest.getPath());

        RecordedRequest secondRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(secondRequest);
    }

    @Test
    public void testAuthRequiredTasksResumeAfterAuthReady() throws Exception {
        ApiEndpointClassification classification = new ApiEndpointClassification();
        IterableTaskRunner runner = new IterableTaskRunner(mockTaskStorage, mockActivityMonitor, mockNetworkConnectivityManager, mockHealthMonitor, classification);

        IterableApiRequest trackRequest = new IterableApiRequest("apiKey", IterableConstants.ENDPOINT_TRACK, new JSONObject(), "POST", null, null, null);
        IterableTask trackTask = new IterableTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, trackRequest.toJSONObject().toString());

        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        when(mockHealthMonitor.canProcess()).thenReturn(true);

        // Phase 1: Auth paused, no unauthenticated tasks available
        when(mockTaskStorage.getNextScheduledTaskNotRequiringJwt(classification)).thenReturn(null);
        runner.setIsPausedForAuth(true);
        runner.onTaskCreated(null);
        runHandlerTasks(runner);

        verify(mockTaskStorage, never()).deleteTask(any(String.class));

        // Phase 2: Auth ready, track task should now process
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(trackTask).thenReturn(null);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        runner.setIsPausedForAuth(false);
        runner.onTaskCreated(null);
        runHandlerTasks(runner);

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK, recordedRequest.getPath());
        verify(mockTaskStorage).deleteTask(trackTask.id);
    }

    // endregion

    private void runHandlerTasks(IterableTaskRunner taskRunner) throws InterruptedException {
        shadowOf(taskRunner.handler.getLooper()).idle();
    }
}
