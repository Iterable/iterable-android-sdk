package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Covers the per-task credential/endpoint binding of the persisted offline queue, and the queue
 * purge semantics that keep queued device disables alive across a logout or a project switch.
 */
@RunWith(TestRunner.class)
public class IterableOfflineTaskRegionTest extends BaseTest {

    private static final String US_ENDPOINT = IterableDataRegion.US.getEndpoint();
    private static final String EU_ENDPOINT = IterableDataRegion.EU.getEndpoint();

    private String previousOverrideUrl;

    @Before
    public void setUp() {
        previousOverrideUrl = IterableRequestTask.overrideUrl;
        IterableRequestTask.overrideUrl = null;
        IterableTestUtils.resetIterableApi();
    }

    @After
    public void tearDown() {
        IterableRequestTask.overrideUrl = previousOverrideUrl;
    }

    @Test
    public void testBaseUrlIsPersistedAndRestored() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKeyA", EU_ENDPOINT, IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, "authTokenA", null, null);

        JSONObject serialized = request.toJSONObject();
        assertEquals(EU_ENDPOINT, serialized.getString(IterableApiRequest.KEY_BASE_URL));

        IterableApiRequest restored = IterableApiRequest.fromJSON(serialized, null, null);
        assertNotNull(restored);
        assertEquals("apiKeyA", restored.apiKey);
        assertEquals(EU_ENDPOINT, restored.baseUrl);
    }

    @Test
    public void testRequestWithoutBaseUrlOmitsTheKey() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKeyA", IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);

        assertFalse("baseUrl should not be written when the request carries none",
                request.toJSONObject().has(IterableApiRequest.KEY_BASE_URL));
    }

    @Test
    public void testTaskPersistedUnderPreviousSchemaFallsBackToLiveConfig() throws Exception {
        // A task written by an older SDK version: same JSON shape, no baseUrl key.
        JSONObject legacyTaskData = new JSONObject();
        legacyTaskData.put("apiKey", "apiKeyA");
        legacyTaskData.put("resourcePath", IterableConstants.ENDPOINT_TRACK);
        legacyTaskData.put("authToken", "authTokenA");
        legacyTaskData.put("requestType", IterableApiRequest.POST);
        legacyTaskData.put("data", new JSONObject());

        IterableApiRequest restored = IterableApiRequest.fromJSON(legacyTaskData, null, null);
        assertNotNull(restored);
        assertNull("Legacy tasks restore without a baseUrl", restored.baseUrl);

        IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyB",
                new IterableConfig.Builder().setDataRegion(IterableDataRegion.EU).build());

        assertEquals("A legacy task resolves its endpoint from the live config, as it always did",
                EU_ENDPOINT, IterableRequestTask.getBaseUrl(restored));
    }

    @Test
    public void testPersistedBaseUrlWinsOverLiveConfig() {
        IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyB",
                new IterableConfig.Builder().setDataRegion(IterableDataRegion.EU).build());

        IterableApiRequest taskFromUsProject = new IterableApiRequest("apiKeyA", US_ENDPOINT, IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);

        assertEquals("A task queued against US must not be sent to the region that is live now",
                US_ENDPOINT, IterableRequestTask.getBaseUrl(taskFromUsProject));
    }

    @Test
    public void testOverrideUrlStillWinsOverPersistedBaseUrl() {
        IterableRequestTask.overrideUrl = "http://localhost:8080/";
        IterableApiRequest request = new IterableApiRequest("apiKeyA", EU_ENDPOINT, IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);

        assertEquals("http://localhost:8080/", IterableRequestTask.getBaseUrl(request));
    }

    @Test
    public void testScheduledTaskCarriesTheRegionItWasCreatedFor() {
        IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyA",
                new IterableConfig.Builder().setDataRegion(IterableDataRegion.EU).build());

        TaskScheduler mockScheduler = mock(TaskScheduler.class);
        HealthMonitor mockHealthMonitor = mock(HealthMonitor.class);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
        OfflineRequestProcessor processor = new OfflineRequestProcessor(mockScheduler, mock(IterableTaskRunner.class), mock(IterableTaskStorage.class), mockHealthMonitor);

        processor.processPostRequest("apiKeyA", IterableConstants.ENDPOINT_TRACK, new JSONObject(), null, null, null);

        ArgumentCaptor<IterableApiRequest> requestCaptor = ArgumentCaptor.forClass(IterableApiRequest.class);
        verify(mockScheduler).scheduleTask(requestCaptor.capture(), isNull(), isNull());
        assertEquals(EU_ENDPOINT, requestCaptor.getValue().baseUrl);
        assertEquals("apiKeyA", requestCaptor.getValue().apiKey);
    }

    /**
     * The endpoint captured by a caller has to win over the live one, or the capture is pointless:
     * pairing a captured key with whichever region is live now is exactly the mismatch it exists to
     * prevent. {@code disablePush} is the caller that does this.
     */
    @Test
    public void testCapturedBaseUrlWinsOverTheLiveRegionWhenSchedulingATask() {
        IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyB",
                new IterableConfig.Builder().setDataRegion(IterableDataRegion.US).build());

        TaskScheduler mockScheduler = mock(TaskScheduler.class);
        HealthMonitor mockHealthMonitor = mock(HealthMonitor.class);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
        OfflineRequestProcessor processor = new OfflineRequestProcessor(mockScheduler, mock(IterableTaskRunner.class), mock(IterableTaskStorage.class), mockHealthMonitor);

        processor.processPostRequest("apiKeyA", EU_ENDPOINT, IterableConstants.ENDPOINT_TRACK, new JSONObject(), null, null, null);

        ArgumentCaptor<IterableApiRequest> requestCaptor = ArgumentCaptor.forClass(IterableApiRequest.class);
        verify(mockScheduler).scheduleTask(requestCaptor.capture(), isNull(), isNull());
        assertEquals("apiKeyA", requestCaptor.getValue().apiKey);
        assertEquals(EU_ENDPOINT, requestCaptor.getValue().baseUrl);
    }

    @Test
    public void testOnLogoutPreservesQueuedDeviceDisable() {
        IterableTaskStorage mockTaskStorage = mock(IterableTaskStorage.class);
        OfflineRequestProcessor processor = new OfflineRequestProcessor(mock(TaskScheduler.class), mock(IterableTaskRunner.class), mockTaskStorage, mock(HealthMonitor.class));

        processor.onLogout(ApplicationProvider.getApplicationContext());

        verify(mockTaskStorage).deleteAllTasksExcept(IterableConstants.ENDPOINT_DISABLE_DEVICE);
        verify(mockTaskStorage, never()).deleteAllTasks();
    }

    @Test
    public void testDeleteAllTasksExceptKeepsOnlyTheDeviceDisable() throws Exception {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(ApplicationProvider.getApplicationContext());
        taskStorage.deleteAllTasks();

        IterableApiRequest trackRequest = new IterableApiRequest("apiKeyA", IterableDataRegion.EU.getEndpoint(), IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);
        IterableApiRequest disableRequest = new IterableApiRequest("apiKeyA", IterableDataRegion.EU.getEndpoint(), IterableConstants.ENDPOINT_DISABLE_DEVICE, new JSONObject(), IterableApiRequest.POST, null, null, null);

        taskStorage.createTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, trackRequest.toJSONObject().toString());
        String disableTaskId = taskStorage.createTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, disableRequest.toJSONObject().toString());
        assertEquals(2, taskStorage.getNumberOfTasks());

        taskStorage.deleteAllTasksExcept(IterableConstants.ENDPOINT_DISABLE_DEVICE);

        ArrayList<String> remaining = taskStorage.getAllTaskIds();
        assertEquals(1, remaining.size());
        assertEquals(disableTaskId, remaining.get(0));

        // The preserved task still knows which project and region it belongs to.
        IterableTask preservedTask = taskStorage.getTask(disableTaskId);
        assertNotNull(preservedTask);
        IterableApiRequest rehydrated = IterableApiRequest.fromJSON(new JSONObject(preservedTask.data), null, null);
        assertNotNull(rehydrated);
        assertEquals("apiKeyA", rehydrated.apiKey);
        assertEquals(IterableDataRegion.EU.getEndpoint(), rehydrated.baseUrl);

        taskStorage.deleteAllTasks();
    }

    /**
     * NAME is nullable in the schema, and SQL three-valued logic makes `name != ?` evaluate to NULL
     * rather than true for a null name, hence the `IS NOT` form of the predicate. A null name is not
     * reachable through the SDK today, which is why the behaviour needs asserting rather than assuming.
     */
    @Test
    public void testDeleteAllTasksExceptRemovesRowsWithNoName() {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(ApplicationProvider.getApplicationContext());
        taskStorage.deleteAllTasks();

        String unnamedTaskId = taskStorage.createTask(null, IterableTaskType.API, "{}");
        String disableTaskId = taskStorage.createTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, "{}");
        assertEquals(2, taskStorage.getNumberOfTasks());

        taskStorage.deleteAllTasksExcept(IterableConstants.ENDPOINT_DISABLE_DEVICE);

        ArrayList<String> remaining = taskStorage.getAllTaskIds();
        assertEquals("only the disable is spared; the null-named row must go", 1, remaining.size());
        assertEquals(disableTaskId, remaining.get(0));
        assertNull(taskStorage.getTask(unnamedTaskId));

        taskStorage.deleteAllTasks();
    }

    @Test
    public void testRehydratedTaskNeverMixesTheNewProjectRegionWithTheOldKey() throws Exception {
        // Task queued while project A (EU) was live.
        IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyA",
                new IterableConfig.Builder().setDataRegion(IterableDataRegion.EU).build());
        IterableApiRequest queuedRequest = new IterableApiRequest("apiKeyA", IterableRequestTask.getRegionBaseUrl(), IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);
        String persisted = queuedRequest.toJSONObject().toString();

        // Project B (US) is live by the time the task is flushed.
        IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyB",
                new IterableConfig.Builder().setDataRegion(IterableDataRegion.US).build());
        assertEquals("The live region must differ from the queued one for this test to mean anything",
                US_ENDPOINT, IterableApi.getInstance().config.dataRegion.getEndpoint());

        IterableApiRequest flushed = IterableApiRequest.fromJSON(new JSONObject(persisted), null, null);
        assertNotNull(flushed);
        assertEquals("apiKeyA", flushed.apiKey);
        assertEquals("The queued key must go to the queued region, not the one that is live now",
                EU_ENDPOINT, IterableRequestTask.getBaseUrl(flushed));
    }

    /**
     * Every other region test resolves the endpoint through
     * {@link IterableRequestTask#getBaseUrl(IterableApiRequest)} directly. This one runs the flush the
     * way {@link IterableTaskRunner} does, rehydrating from storage and executing the request, and
     * checks it actually lands on the persisted endpoint.
     */
    @Test
    public void testTaskRunnerFlushesToThePersistedEndpointNotTheLiveRegion() throws Exception {
        MockWebServer queuedRegion = new MockWebServer();
        queuedRegion.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{}");
            }
        });
        try {
            String queuedEndpoint = queuedRegion.url("").toString();

            IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(ApplicationProvider.getApplicationContext());
            taskStorage.deleteAllTasks();

            IterableApiRequest queuedRequest = new IterableApiRequest("apiKeyA", queuedEndpoint, IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, "authTokenA", null, null);
            String taskId = taskStorage.createTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, queuedRequest.toJSONObject().toString());

            // Project B, a different region, is live by flush time.
            IterableApi.initialize(ApplicationProvider.getApplicationContext(), "apiKeyB",
                    new IterableConfig.Builder().setDataRegion(IterableDataRegion.EU).build());

            IterableTaskRunner taskRunner = new IterableTaskRunner(taskStorage,
                    IterableActivityMonitor.getInstance(),
                    IterableNetworkConnectivityManager.sharedInstance(ApplicationProvider.getApplicationContext()),
                    mock(HealthMonitor.class));
            IterableTask task = taskStorage.getTask(taskId);
            assertNotNull(task);

            // Exactly what IterableTaskRunner.processTask does to a persisted API task.
            IterableApiRequest flushed = IterableApiRequest.fromJSON(taskRunner.getTaskDataWithDate(task), null, null, null);
            assertNotNull(flushed);
            IterableRequestTask.executeApiRequest(flushed);

            RecordedRequest recorded = queuedRegion.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull("The flushed task must reach the endpoint it was queued for", recorded);
            assertEquals("/" + IterableConstants.ENDPOINT_TRACK, recorded.getPath());
            assertEquals("apiKeyA", recorded.getHeader(IterableConstants.HEADER_API_KEY));

            taskStorage.deleteAllTasks();
        } finally {
            queuedRegion.shutdown();
        }
    }

    @Test
    public void testScheduleTaskSerializesBaseUrlIntoStorage() throws Exception {
        IterableTaskStorage mockTaskStorage = mock(IterableTaskStorage.class);
        TaskScheduler scheduler = new TaskScheduler(mockTaskStorage, mock(IterableTaskRunner.class));

        IterableApiRequest request = new IterableApiRequest("apiKeyA", EU_ENDPOINT, IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);
        scheduler.scheduleTask(request, null, null);

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockTaskStorage).createTask(eq(IterableConstants.ENDPOINT_TRACK), any(IterableTaskType.class), dataCaptor.capture());
        assertEquals(EU_ENDPOINT, new JSONObject(dataCaptor.getValue()).getString(IterableApiRequest.KEY_BASE_URL));
    }
}
