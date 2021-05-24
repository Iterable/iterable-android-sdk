package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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
        assertEquals("Online", request.getProcessorType().toString());
    }


    @Test
    public void canScheduleFailWhenMaxCountReached() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        when(mockTaskStorage.numberOfTasks()).thenReturn(IterableConstants.MAX_OFFLINE_OPERATION);
        assertFalse(healthMonitor.canSchedule());
    }

    @Test
    public void canScheduleWhenMaxCountNotReached() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        when(mockTaskStorage.numberOfTasks()).thenReturn(IterableConstants.MAX_OFFLINE_OPERATION - 1);
        assertTrue(healthMonitor.canSchedule());
    }

    @Test
    public void canProcessReturnTrueIfDBok() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        assertTrue(healthMonitor.canProcess());
    }

    @Test
    public void canProcessReturnFalseIfDBError() throws Exception {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(getContext());
        HealthMonitor healthMonitor = new HealthMonitor(taskStorage);
        healthMonitor.onNextTaskError();
        assertFalse(healthMonitor.canProcess());
    }

}
