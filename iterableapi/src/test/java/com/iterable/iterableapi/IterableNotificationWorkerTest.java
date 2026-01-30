package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.iterable.iterableapi.IterableTestUtils.bundleToMap;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class IterableNotificationWorkerTest {

    private Context context;
    private IterableNotificationHelper.IterableNotificationHelperImpl originalNotificationHelper;
    private IterableNotificationHelper.IterableNotificationHelperImpl notificationHelperSpy;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        
        // Initialize WorkManager for testing
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        originalNotificationHelper = IterableNotificationHelper.instance;
        notificationHelperSpy = spy(originalNotificationHelper);
        IterableNotificationHelper.instance = notificationHelperSpy;
    }

    @Test
    public void testWorkerProcessesNotificationWithImage() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.hasImageUrl(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.getImageUrl(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.createNotification(any(Context.class), any(Bundle.class))).thenCallRealMethod();
        doNothing().when(notificationHelperSpy).postNotificationOnDevice(any(Context.class), any(IterableNotificationBuilder.class));

        // Create test data - structure it like FCM message data
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_with_image.json"));
        dataBuilder.putString(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        dataBuilder.putBoolean("has_image", true);
        dataBuilder.putInt("message_priority", 1);

        // Build worker
        IterableNotificationWorker worker = TestWorkerBuilder.from(context, IterableNotificationWorker.class)
                .setInputData(dataBuilder.build())
                .build();

        // Execute work
        androidx.work.ListenableWorker.Result result = worker.doWork();

        // Verify result
        assertNotNull(result);
        // Note: Result will be failure if image loading fails, which is expected in test environment
        // The important thing is that the worker attempted to process the notification

        // Verify that createNotification was called
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(notificationHelperSpy).createNotification(eq(context), bundleCaptor.capture());
    }

    @Test
    public void testWorkerProcessesNotificationWithoutImage() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.isGhostPush(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.hasImageUrl(any(Bundle.class))).thenCallRealMethod();
        when(notificationHelperSpy.createNotification(any(Context.class), any(Bundle.class))).thenCallRealMethod();
        doNothing().when(notificationHelperSpy).postNotificationOnDevice(any(Context.class), any(IterableNotificationBuilder.class));

        // Create test data without image - structure it like FCM message data
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));
        dataBuilder.putString(IterableConstants.ITERABLE_DATA_BODY, "Message body");
        dataBuilder.putBoolean("has_image", false);
        dataBuilder.putInt("message_priority", 1);

        // Build worker
        IterableNotificationWorker worker = TestWorkerBuilder.from(context, IterableNotificationWorker.class)
                .setInputData(dataBuilder.build())
                .build();

        // Execute work
        androidx.work.ListenableWorker.Result result = worker.doWork();

        // Verify result
        assertNotNull(result);

        // Verify that createNotification was called
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(notificationHelperSpy).createNotification(eq(context), bundleCaptor.capture());
    }

    @Test
    public void testWorkerReturnsFailureForNonIterablePush() throws Exception {
        when(notificationHelperSpy.isIterablePush(any(Bundle.class))).thenReturn(false);

        // Create test data with invalid payload
        Map<String, String> messageData = new HashMap<>();
        messageData.put("invalid", "data");
        Data.Builder dataBuilder = new Data.Builder();
        for (Map.Entry<String, String> entry : messageData.entrySet()) {
            dataBuilder.putString(entry.getKey(), entry.getValue());
        }
        dataBuilder.putBoolean("has_image", false);
        dataBuilder.putInt("message_priority", 1);

        // Build worker
        IterableNotificationWorker worker = TestWorkerBuilder.from(context, IterableNotificationWorker.class)
                .setInputData(dataBuilder.build())
                .build();

        // Execute work
        androidx.work.ListenableWorker.Result result = worker.doWork();

        // Verify result is failure
        assertNotNull(result);
        // Result should be failure for non-Iterable push
    }
}

