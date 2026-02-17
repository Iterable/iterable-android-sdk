package com.iterable.iterableapi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import okhttp3.mockwebserver.MockWebServer;

/**
 * TDD-style atomic tests for IterableNotificationWorker.
 * Each test validates ONE specific behavior of the Worker.
 */
public class IterableNotificationWorkerUnitTest extends BaseTest {

    private MockWebServer server;
    private IterableNotificationHelper.IterableNotificationHelperImpl helperSpy;
    private IterableNotificationHelper.IterableNotificationHelperImpl originalHelper;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.resetIterableApi();
        IterableTestUtils.createIterableApiNew();

        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        originalHelper = IterableNotificationHelper.instance;
        helperSpy = spy(originalHelper);
        IterableNotificationHelper.instance = helperSpy;
    }

    @After
    public void tearDown() throws Exception {
        IterableNotificationHelper.instance = originalHelper;
        if (server != null) {
            server.shutdown();
        }
    }

    // ========================================================================
    // WORKER RESULT TESTS
    // ========================================================================

    @Test
    public void testWorkerReturnsSuccessWithValidData() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();
        when(helperSpy.isIterablePush(any())).thenCallRealMethod();
        when(helperSpy.isGhostPush(any())).thenCallRealMethod();

        Bundle bundle = new Bundle();
        bundle.putString(IterableConstants.ITERABLE_DATA_KEY,
                IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        bundle.putString(IterableConstants.ITERABLE_DATA_BODY, "Test");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.success(), result);
    }

    @Test
    public void testWorkerReturnsFailureWithNullData() throws Exception {
        Data inputData = new Data.Builder()
                // No JSON data
                .build();

        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.failure(), result);
    }

    @Test
    public void testWorkerReturnsFailureWithEmptyData() throws Exception {
        Data inputData = new Data.Builder()
                .putString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON, "")
                .build();

        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.failure(), result);
    }

    // ========================================================================
    // WORKER BEHAVIOR TESTS
    // ========================================================================

    @Test
    public void testWorkerCallsCreateNotificationWithValidData() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        Bundle bundle = new Bundle();
        bundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");
        bundle.putString(IterableConstants.ITERABLE_DATA_BODY, "Test");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        verify(helperSpy).createNotification(any(), any(Bundle.class));
    }

    @Test
    public void testWorkerCallsPostNotificationWithValidBuilder() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();
        when(helperSpy.isIterablePush(any())).thenCallRealMethod();
        when(helperSpy.isGhostPush(any())).thenCallRealMethod();

        Bundle bundle = new Bundle();
        bundle.putString(IterableConstants.ITERABLE_DATA_KEY,
                IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        bundle.putString(IterableConstants.ITERABLE_DATA_BODY, "Test");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        verify(helperSpy).postNotificationOnDevice(any(), any(IterableNotificationBuilder.class));
    }

    @Test
    public void testWorkerDoesNotCallPostNotificationWhenBuilderIsNull() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenReturn(null);

        Bundle bundle = new Bundle();
        bundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        verify(helperSpy, never()).postNotificationOnDevice(any(), any());
    }

    @Test
    public void testWorkerSucceedsWhenBuilderIsNull() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenReturn(null);

        Bundle bundle = new Bundle();
        bundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();

        assertEquals("Worker should succeed even when builder is null",
                ListenableWorker.Result.success(), result);
    }

    // ========================================================================
    // DATA SERIALIZATION TESTS - Input Creation
    // ========================================================================

    @Test
    public void testCreateInputDataReturnsNonNullData() {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");

        Data inputData = IterableNotificationWorker.createInputData(bundle);

        assertNotNull("Input data should not be null", inputData);
    }

    @Test
    public void testCreateInputDataIncludesJsonString() {
        Bundle bundle = new Bundle();
        bundle.putString(IterableConstants.ITERABLE_DATA_TITLE, "Test");

        Data inputData = IterableNotificationWorker.createInputData(bundle);

        String json = inputData.getString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON);
        assertNotNull("JSON string should be present", json);
    }

    @Test
    public void testCreateInputDataHandlesEmptyBundle() {
        Bundle bundle = new Bundle();

        Data inputData = IterableNotificationWorker.createInputData(bundle);

        assertNotNull("Input data should not be null for empty bundle", inputData);
    }

    // ========================================================================
    // DATA SERIALIZATION TESTS - Deserialization
    // ========================================================================

    @Test
    public void testDeserializationPreservesSingleField() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        Bundle originalBundle = new Bundle();
        originalBundle.putString(IterableConstants.ITERABLE_DATA_TITLE, "Test Title");
        originalBundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");

        Data inputData = IterableNotificationWorker.createInputData(originalBundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        Bundle deserializedBundle = bundleCaptor.getValue();
        assertEquals("Test Title", deserializedBundle.getString(IterableConstants.ITERABLE_DATA_TITLE));
    }

    @Test
    public void testDeserializationPreservesMultipleFields() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        Bundle originalBundle = new Bundle();
        originalBundle.putString(IterableConstants.ITERABLE_DATA_TITLE, "Title");
        originalBundle.putString(IterableConstants.ITERABLE_DATA_BODY, "Body");
        originalBundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");
        originalBundle.putString("custom", "value");

        Data inputData = IterableNotificationWorker.createInputData(originalBundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        Bundle deserializedBundle = bundleCaptor.getValue();
        assertEquals("Title", deserializedBundle.getString(IterableConstants.ITERABLE_DATA_TITLE));
        assertEquals("Body", deserializedBundle.getString(IterableConstants.ITERABLE_DATA_BODY));
        assertEquals("{}", deserializedBundle.getString(IterableConstants.ITERABLE_DATA_KEY));
        assertEquals("value", deserializedBundle.getString("custom"));
    }

    @Test
    public void testDeserializationPreservesSpecialCharacters() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String specialValue = "Test with spaces, symbols: !@#$%, and \"quotes\"";
        Bundle originalBundle = new Bundle();
        originalBundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");
        originalBundle.putString("special", specialValue);

        Data inputData = IterableNotificationWorker.createInputData(originalBundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        Bundle deserializedBundle = bundleCaptor.getValue();
        assertEquals(specialValue, deserializedBundle.getString("special"));
    }

    @Test
    public void testDeserializationPreservesKeyCount() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        Bundle originalBundle = new Bundle();
        originalBundle.putString("key1", "value1");
        originalBundle.putString("key2", "value2");
        originalBundle.putString("key3", "value3");
        originalBundle.putString(IterableConstants.ITERABLE_DATA_KEY, "{}");

        int originalCount = originalBundle.keySet().size();

        Data inputData = IterableNotificationWorker.createInputData(originalBundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        Bundle deserializedBundle = bundleCaptor.getValue();
        assertEquals("Key count should match", originalCount, deserializedBundle.keySet().size());
    }

    @Test
    public void testDeserializationHandlesJsonWithNestedObjects() throws Exception {
        when(helperSpy.createNotification(any(), any())).thenCallRealMethod();

        String complexJson = "{\"campaignId\":123,\"metadata\":{\"key\":\"value\"}}";
        Bundle originalBundle = new Bundle();
        originalBundle.putString(IterableConstants.ITERABLE_DATA_KEY, complexJson);

        Data inputData = IterableNotificationWorker.createInputData(originalBundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        worker.doWork();

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(helperSpy).createNotification(any(), bundleCaptor.capture());

        Bundle deserializedBundle = bundleCaptor.getValue();
        assertEquals(complexJson, deserializedBundle.getString(IterableConstants.ITERABLE_DATA_KEY));
    }

    // ========================================================================
    // FOREGROUND INFO TESTS (pre-Android 12 expedited work support)
    // ========================================================================

    @Test
    public void testGetForegroundInfoReturnsNonNull() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ForegroundInfo foregroundInfo = worker.getForegroundInfo();

        assertNotNull("getForegroundInfo should return non-null ForegroundInfo", foregroundInfo);
    }

    @Test
    public void testGetForegroundInfoReturnsValidNotificationId() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ForegroundInfo foregroundInfo = worker.getForegroundInfo();

        assertNotNull("ForegroundInfo should contain a notification", foregroundInfo.getNotification());
    }

    @Test
    public void testGetForegroundInfoDoesNotThrow() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        // Should not throw any exception - this is critical for pre-Android 12 expedited work
        try {
            ForegroundInfo foregroundInfo = worker.getForegroundInfo();
            assertNotNull(foregroundInfo);
        } catch (Exception e) {
            throw new AssertionError(
                    "getForegroundInfo() must not throw on pre-Android 12 devices. " +
                    "Without this, setExpedited() causes IllegalStateException. Error: " + e.getMessage(), e);
        }
    }

    @Test
    public void testGetForegroundInfoCanBeCalledMultipleTimes() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");

        Data inputData = IterableNotificationWorker.createInputData(bundle);
        IterableNotificationWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationWorker.class)
                .setInputData(inputData)
                .build();

        // Should be safe to call multiple times without issues
        ForegroundInfo first = worker.getForegroundInfo();
        ForegroundInfo second = worker.getForegroundInfo();

        assertNotNull(first);
        assertNotNull(second);
    }
}
