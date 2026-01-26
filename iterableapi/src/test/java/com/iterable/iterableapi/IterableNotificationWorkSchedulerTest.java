package com.iterable.iterableapi;

import android.os.Bundle;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TDD-style atomic tests for IterableNotificationWorkScheduler.
 * Each test validates ONE specific behavior of the scheduler.
 * 
 * Tests verify:
 * - Work scheduling with WorkManager
 * - Callback invocations
 * - Error handling
 * - Data preservation
 * - WorkRequest configuration
 */
public class IterableNotificationWorkSchedulerTest extends BaseTest {

    private MockWebServer server;
    private WorkManager mockWorkManager;
    private IterableNotificationWorkScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.resetIterableApi();
        IterableTestUtils.createIterableApiNew();
        
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        // Create mock WorkManager for testing
        mockWorkManager = mock(WorkManager.class);
        
        // Create scheduler with mock WorkManager
        scheduler = new IterableNotificationWorkScheduler(getContext(), mockWorkManager);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    // ========================================================================
    // SCHEDULING SUCCESS TESTS
    // ========================================================================

    @Test
    public void testScheduleNotificationWorkEnqueuesWithWorkManager() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testScheduleNotificationWorkCallsSuccessCallback() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        verify(callback).onScheduleSuccess(any(UUID.class));
    }

    @Test
    public void testScheduleNotificationWorkPassesWorkIdToCallback() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(callback).onScheduleSuccess(uuidCaptor.capture());

        UUID workId = uuidCaptor.getValue();
        assertNotNull("Work ID should not be null", workId);
    }

    @Test
    public void testScheduleNotificationWorkSucceedsWithNullCallback() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        // Should not throw exception with null callback
        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testScheduleNotificationWorkEnqueuesOnlyOnce() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        // Verify enqueue called exactly once
        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    // ========================================================================
    // SCHEDULING FAILURE TESTS
    // ========================================================================

    @Test
    public void testScheduleNotificationWorkCallsFailureCallbackOnException() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        // Configure mock to throw exception
        doThrow(new RuntimeException("WorkManager error"))
            .when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        verify(callback).onScheduleFailure(any(Exception.class), any(Bundle.class));
    }

    @Test
    public void testScheduleNotificationWorkPassesExceptionToFailureCallback() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        RuntimeException testException = new RuntimeException("Test error");
        doThrow(testException).when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onScheduleFailure(exceptionCaptor.capture(), any(Bundle.class));

        assertEquals("Exception should match", testException, exceptionCaptor.getValue());
    }

    @Test
    public void testScheduleNotificationWorkPassesOriginalDataToFailureCallback() {
        Bundle data = new Bundle();
        data.putString("testKey", "testValue");

        doThrow(new RuntimeException("Error"))
            .when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(callback).onScheduleFailure(any(Exception.class), bundleCaptor.capture());

        Bundle capturedData = bundleCaptor.getValue();
        assertEquals("testValue", capturedData.getString("testKey"));
    }

    @Test
    public void testScheduleNotificationWorkHandlesFailureWithNullCallback() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        doThrow(new RuntimeException("Error"))
            .when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        // Should not throw exception with null callback
        scheduler.scheduleNotificationWork(data, false, null);
    }

    // ========================================================================
    // DATA HANDLING TESTS
    // ========================================================================

    @Test
    public void testScheduleNotificationWorkPreservesNotificationData() {
        Bundle data = new Bundle();
        data.putString(IterableConstants.ITERABLE_DATA_TITLE, "Test Title");
        data.putString(IterableConstants.ITERABLE_DATA_BODY, "Test Body");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
        Data workData = capturedRequest.getWorkSpec().input;

        String jsonString = workData.getString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON);
        assertNotNull("Notification data should be preserved", jsonString);
        assertTrue("Should contain title", jsonString.contains("Test Title"));
        assertTrue("Should contain body", jsonString.contains("Test Body"));
    }

    @Test
    public void testScheduleNotificationWorkHandlesGhostPushFlagTrue() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, true, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
        Data workData = capturedRequest.getWorkSpec().input;

        boolean isGhostPush = workData.getBoolean(IterableNotificationWorker.KEY_IS_GHOST_PUSH, false);
        assertEquals("Ghost push flag should be true", true, isGhostPush);
    }

    @Test
    public void testScheduleNotificationWorkHandlesGhostPushFlagFalse() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
        Data workData = capturedRequest.getWorkSpec().input;

        boolean isGhostPush = workData.getBoolean(IterableNotificationWorker.KEY_IS_GHOST_PUSH, true);
        assertEquals("Ghost push flag should be false", false, isGhostPush);
    }

    @Test
    public void testScheduleNotificationWorkHandlesEmptyBundle() {
        Bundle emptyData = new Bundle();

        scheduler.scheduleNotificationWork(emptyData, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testScheduleNotificationWorkPreservesMultipleFields() {
        Bundle data = new Bundle();
        data.putString("field1", "value1");
        data.putString("field2", "value2");
        data.putString("field3", "value3");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
        Data workData = capturedRequest.getWorkSpec().input;

        String jsonString = workData.getString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON);
        assertTrue("Should contain field1", jsonString.contains("field1"));
        assertTrue("Should contain field2", jsonString.contains("field2"));
        assertTrue("Should contain field3", jsonString.contains("field3"));
    }

    // ========================================================================
    // WORKMANAGER INTEGRATION TESTS
    // ========================================================================

    @Test
    public void testScheduleNotificationWorkUsesCorrectWorkerClass() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
        assertEquals("Should use IterableNotificationWorker",
                IterableNotificationWorker.class.getName(),
                capturedRequest.getWorkSpec().workerClassName);
    }

    @Test
    public void testScheduleNotificationWorkCreatesOneTimeRequest() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        // Verify a OneTimeWorkRequest was enqueued
        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testScheduleNotificationWorkSetsInputData() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
        Data workData = capturedRequest.getWorkSpec().input;

        assertNotNull("Input data should be set", workData);
        assertNotNull("Should have notification JSON", 
                workData.getString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON));
    }

    // ========================================================================
    // CALLBACK BEHAVIOR TESTS
    // ========================================================================

    @Test
    public void testSuccessCallbackIsCalledExactlyOnce() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        verify(callback).onScheduleSuccess(any(UUID.class));
        verify(callback, never()).onScheduleFailure(any(Exception.class), any(Bundle.class));
    }

    @Test
    public void testFailureCallbackIsCalledExactlyOnce() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        doThrow(new RuntimeException("Error"))
            .when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        verify(callback).onScheduleFailure(any(Exception.class), any(Bundle.class));
        verify(callback, never()).onScheduleSuccess(any(UUID.class));
    }

    @Test
    public void testCallbacksAreOptional() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        // Should work without callbacks (null)
        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testFailureCallbackReceivesCorrectException() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        IllegalStateException testException = new IllegalStateException("Test exception");
        doThrow(testException).when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onScheduleFailure(exceptionCaptor.capture(), any(Bundle.class));

        assertEquals("Should pass the same exception", testException, exceptionCaptor.getValue());
    }

    @Test
    public void testFailureCallbackReceivesOriginalNotificationData() {
        Bundle data = new Bundle();
        data.putString("originalKey", "originalValue");

        doThrow(new RuntimeException("Error"))
            .when(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));

        IterableNotificationWorkScheduler.SchedulerCallback callback = 
            mock(IterableNotificationWorkScheduler.SchedulerCallback.class);

        scheduler.scheduleNotificationWork(data, false, callback);

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(callback).onScheduleFailure(any(Exception.class), bundleCaptor.capture());

        Bundle capturedData = bundleCaptor.getValue();
        assertEquals("originalValue", capturedData.getString("originalKey"));
    }

    // ========================================================================
    // CONSTRUCTOR AND INITIALIZATION TESTS
    // ========================================================================

    @Test
    public void testConstructorWithContext() {
        // Create scheduler with just context (production constructor)
        IterableNotificationWorkScheduler productionScheduler = 
            new IterableNotificationWorkScheduler(getContext());

        assertNotNull("Scheduler should be created", productionScheduler);
        assertNotNull("Context should be set", productionScheduler.getContext());
        assertNotNull("WorkManager should be initialized", productionScheduler.getWorkManager());
    }

    @Test
    public void testConstructorWithContextAndWorkManager() {
        WorkManager testWorkManager = mock(WorkManager.class);

        IterableNotificationWorkScheduler testScheduler = 
            new IterableNotificationWorkScheduler(getContext(), testWorkManager);

        assertNotNull("Scheduler should be created", testScheduler);
        assertEquals("Should use injected WorkManager", testWorkManager, testScheduler.getWorkManager());
    }

    @Test
    public void testConstructorUsesApplicationContext() {
        IterableNotificationWorkScheduler testScheduler = 
            new IterableNotificationWorkScheduler(getContext());

        assertEquals("Should use application context", 
                getContext().getApplicationContext(), 
                testScheduler.getContext());
    }

    // ========================================================================
    // DATA CREATION TESTS
    // ========================================================================

    @Test
    public void testScheduleNotificationWorkCreatesValidInputData() {
        Bundle data = new Bundle();
        data.putString(IterableConstants.ITERABLE_DATA_TITLE, "Title");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        Data inputData = requestCaptor.getValue().getWorkSpec().input;
        assertNotNull("Input data should not be null", inputData);
    }

    @Test
    public void testScheduleNotificationWorkIncludesAllRequiredKeys() {
        Bundle data = new Bundle();
        data.putString("key", "value");

        scheduler.scheduleNotificationWork(data, false, null);

        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = 
            ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(mockWorkManager).enqueue(requestCaptor.capture());

        Data inputData = requestCaptor.getValue().getWorkSpec().input;
        
        // Verify required keys are present
        assertNotNull("Should have notification JSON", 
                inputData.getString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON));
        
        // Ghost push flag should be present (default false)
        boolean hasFlag = inputData.getKeyValueMap()
                .containsKey(IterableNotificationWorker.KEY_IS_GHOST_PUSH);
        assertTrue("Should have ghost push flag", hasFlag);
    }

    @Test
    public void testScheduleNotificationWorkWithComplexData() {
        Bundle data = new Bundle();
        data.putString(IterableConstants.ITERABLE_DATA_KEY, "{\"campaignId\":123}");
        data.putString(IterableConstants.ITERABLE_DATA_TITLE, "Title");
        data.putString(IterableConstants.ITERABLE_DATA_BODY, "Body");
        data.putString("customField", "customValue");

        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Test
    public void testScheduleNotificationWorkHandlesSpecialCharactersInData() {
        Bundle data = new Bundle();
        data.putString("special", "Value with symbols: !@#$% and \"quotes\"");

        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testScheduleNotificationWorkHandlesUnicodeInData() {
        Bundle data = new Bundle();
        data.putString("unicode", "Unicode: 你好 👋 émojis 🎉");

        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testScheduleNotificationWorkHandlesLargeBundle() {
        Bundle data = new Bundle();
        for (int i = 0; i < 100; i++) {
            data.putString("key" + i, "value" + i);
        }

        scheduler.scheduleNotificationWork(data, false, null);

        verify(mockWorkManager).enqueue(any(OneTimeWorkRequest.class));
    }
}
