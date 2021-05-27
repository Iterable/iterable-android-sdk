package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void canScheduleFailWhenMaxCountReached() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        when(mockTaskStorage.getNumberOfTasks()).thenReturn(IterableConstants.OFFLINE_TASKS_LIMIT);
        assertFalse(healthMonitor.canSchedule());
    }

    @Test
    public void canScheduleWhenMaxCountNotReached() throws Exception {
        HealthMonitor healthMonitor = new HealthMonitor(mockTaskStorage);
        when(mockTaskStorage.getNumberOfTasks()).thenReturn(IterableConstants.OFFLINE_TASKS_LIMIT - 1);
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
        healthMonitor.onDBError();
        assertFalse(healthMonitor.canProcess());
    }
}
