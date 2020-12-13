package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(TestRunner.class)
public class TaskSchedulerTest {
    private IterableTaskStorage mockTaskStorage;
    private IterableTaskRunner mockTaskRunner;
    private TaskScheduler taskScheduler;

    @Before
    public void setUp() throws Exception {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockTaskRunner = mock(IterableTaskRunner.class);
        taskScheduler = new TaskScheduler(mockTaskStorage, mockTaskRunner);
    }

    @Test
    public void testScheduleTaskCreatesTaskInStorage() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        taskScheduler.scheduleTask(request, null, null);
        verify(mockTaskStorage).createTask(eq("api/test"), eq(IterableTaskType.API), eq(request.toJSONObject().toString()));
    }
}
