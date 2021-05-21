package com.iterable.iterableapi;

import android.app.Application;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class HealthMonitorTest extends BaseTest {
    private IterableTaskStorage mockTaskStorage;
    private IterableTaskRunner mockTaskRunner;
    private TaskScheduler mockTaskScheduler;

    @Before
    public void setUp() {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockTaskRunner = mock(IterableTaskRunner.class);
        mockTaskScheduler = mock(TaskScheduler.class);
    }

    @Test
    public void testUseOfflineProcessorByDefault() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        IterableTask task = new IterableTask("testTask", IterableTaskType.API, request.toJSONObject().toString());
        when(mockTaskStorage.getNextScheduledTask()).thenReturn(task).thenReturn(null);

//        assertEquals(request.getProcessorType().toString(), "Offline");
    }


    @Test
    public void canScheduleFailWhenMaxCountReached() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        when(mockTaskStorage.numberOfTasks()).thenReturn((long) 1000);
        assertFalse(healthMonitor.canSchedule());
    }

    @Test
    public void canScheduleWhenMaxCountNotReached() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        when(mockTaskStorage.numberOfTasks()).thenReturn((long) 999);
        assertTrue(healthMonitor.canSchedule());
    }

    //TODO: Modify below tests to check for canProcess functionality
//    @Test
//    public void canProcessReturnTrueIfDBok() throws Exception {
//        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(getContext());
//        HealthMonitor healthMonitor = new HealthMonitor(taskStorage);
//        assertTrue(healthMonitor.canProcess());
//        taskStorage = null;
//    }
//
//
//    public void canProcessReturnFalseIfDBError() throws Exception {
//        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(getContext());
//        HealthMonitor healthMonitor = new HealthMonitor(taskStorage);
//        healthMonitor.onNextTaskError();
//        assertFalse(healthMonitor.canProcess());
//        taskStorage = null;
//    }

}
